package com.ibm.drl.hbcp.parser.enriching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Streams;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeNameNumberTriple;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.cleaning.TableValueFinder;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.ParsingUtils;

import lombok.EqualsAndHashCode;
import lombok.Value;

/**
 * Mines PDFs for outcomes of other timepoints/followups (than the longest, which is usually the only one annotated).
 *
 * TODO: correct selection of the arm (in case the otherOutcome was found both in the same column as one annotation and the same row as another).
 *
 * @author mgleize
 */
public class AnnotationOutcomesMiner {

    private final PdfToDocumentFunction pdfParser;
    private final File pdfFolder;
    private final Attribute outcomeAttribute = Attributes.get().getFromName("Outcome value");
    private final Attribute followupAttribute = Attributes.get().getFromName("Longest follow up");

    private static final List<String> TIMEPOINT_KEYWORDS = Lists.newArrayList("day", "week", "month");

    // internal statistics for debugging
    private int armsAddedCount = 0;

    public AnnotationOutcomesMiner(PdfToDocumentFunction pdfParser, File pdfFolder) {
        this.pdfParser = pdfParser;
        this.pdfFolder = pdfFolder;
    }

    public AnnotationOutcomesMiner(Properties props) {
        this(new ReparsePdfToDocument(new File(props.getProperty("coll.extracted.reparse"))), new File(props.getProperty("coll")));
    }

    /** Enrich the annotations with outcomes from other timepoints (than the longest) */
    public AttributeValueCollection<AnnotatedAttributeValuePair> withOtherOutcomes(AttributeValueCollection<AnnotatedAttributeValuePair> annotations) throws IOException {
        // mine other outcomes in each document
        List<AnnotatedAttributeNameNumberTriple> otherOutcomes = new ArrayList<>();
        for (String docName : annotations.getDocNames()) {
            otherOutcomes.addAll(getOtherOutcomesWithTimepoints(annotations, docName));
        }
        // build the resulting collection
        List<AnnotatedAttributeValuePair> res = new ArrayList<>(annotations.getAllPairs());
        res.addAll(otherOutcomes);
        return new AttributeValueCollection<>(res);
    }

    public AttributeValueCollection<AnnotatedAttributeValuePair> withOtherOutcomeAndFollowupSeparate(AttributeValueCollection<AnnotatedAttributeValuePair> annotations) throws IOException {
        // mine other outcomes in each document
        AttributeValueCollection<AnnotatedAttributeValuePair> withOthers = withOtherOutcomes(annotations);
        // separate the AnnotatedAttributeNameNumberTriple and copy the rest
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        for (AnnotatedAttributeValuePair aavp : withOthers) {
            if (aavp instanceof AnnotatedAttributeNameNumberTriple && aavp.getAttribute().isOutcomeValue()) {
                // separate the timepoint and the outcome value
                AnnotatedAttributeNameNumberTriple attributeFollowupOutcome = (AnnotatedAttributeNameNumberTriple)aavp;
                AnnotatedAttributeValuePair followUpAlone = new AnnotatedAttributeValuePair(followupAttribute,
                        attributeFollowupOutcome.getValueName(), aavp.getDocName(), aavp.getArm(), aavp.getContext(),
                        aavp.getHighlightedText(), aavp.getSprintNo(), aavp.getAnnotationPage());
                res.add(followUpAlone);
                // the pair can also serve as the outcome value alone
                res.add(aavp);
            } else {
                res.add(aavp);
            }
        }
        return new AttributeValueCollection<>(res);
    }

    public AttributeValueCollection<AnnotatedAttributeValuePair> withOtherOutcomesInSeparateArms(AttributeValueCollection<AnnotatedAttributeValuePair> annotations) throws IOException {
        // mine other outcomes in each document
        List<AnnotatedAttributeValuePair> otherOutcomeArms = new ArrayList<>();
        int extraArmId = 1;
        for (String docName : annotations.getDocNames()) {
            List<AnnotatedAttributeNameNumberTriple> otherOutcomesForDoc = getOtherOutcomesWithTimepoints(annotations, docName);
            for (AnnotatedAttributeNameNumberTriple otherOutcome : otherOutcomesForDoc) {
                // get the values of the original arm
                Multiset<AnnotatedAttributeValuePair> valuesInArm = annotations.getArmifiedPairsInDoc(docName).get(otherOutcome.getArm());
                // overwrite Outcome value and Longest followup
                List<AnnotatedAttributeValuePair> withNewFollowup = getArmValuesWithOverriddenAttribute(
                        valuesInArm,
                        followupAttribute,
                        otherOutcome.getValueName(),
                        otherOutcome);
                List<AnnotatedAttributeValuePair> withNewOutcome = getArmValuesWithOverriddenAttribute(
                        withNewFollowup,
                        outcomeAttribute,
                        otherOutcome.getValueNumber(),
                        otherOutcome);
                // rename their arm
                String newArmId = otherOutcome.getArm().getStandardName() + "-" + extraArmId;
                Arm newArm = new Arm(newArmId, newArmId + "(otherOV)");
                List<AnnotatedAttributeValuePair> newArmValues = withNewOutcome.stream()
                        .map(aavp -> aavp.withArm(newArm))
                        .collect(Collectors.toList());
                extraArmId++;
                // add this new arm to the result
                otherOutcomeArms.addAll(newArmValues);
                armsAddedCount++;
            }
        }
        // build the resulting collection
        List<AnnotatedAttributeValuePair> res = new ArrayList<>(annotations.getAllPairs());
        res.addAll(otherOutcomeArms);
        return new AttributeValueCollection<>(res);
    }

    /** Get the other outcomes using the annotations, for the given document */
    public List<AnnotatedAttributeNameNumberTriple> getOtherOutcomesWithTimepoints(AttributeValueCollection<AnnotatedAttributeValuePair> annotations,
                                                                                   String docName) throws IOException {
        List<OtherOutcomeCandidate> otherOutcomeCells = getContextCellsWithOutcomeStatus(annotations, docName);
        return otherOutcomeCells.stream()
                // keep only the non-annotated outcome values
                .filter(outcomeCandidate -> outcomeCandidate.isOutcomeValue && !outcomeCandidate.isAnnotation)
                .map(cell -> getOutcomeWithTimepoint(cell, docName))
                .collect(Collectors.toList());
    }

    private AnnotatedAttributeNameNumberTriple getOutcomeWithTimepoint(OtherOutcomeCandidate otherOutcome, String docName) {
        // get a header with some timepoint
        List<String> headers = new ArrayList<>(otherOutcome.getCell().getRowHeaders());
        headers.addAll(otherOutcome.getCell().getColumnHeaders());
        Optional<String> firstTimepoint = headers.stream()
                .filter(header -> TIMEPOINT_KEYWORDS.stream().anyMatch(timepoint -> header.contains(timepoint)))
                .findFirst();
        if (firstTimepoint.isPresent()) {
            String timepoint = firstTimepoint.get();
            AnnotatedAttributeNameNumberTriple res = new AnnotatedAttributeNameNumberTriple(
                    outcomeAttribute,
                    timepoint,
                    getNumericValue(otherOutcome.getCell().getValue(), otherOutcome.positionOfNumber),
                    docName,
                    otherOutcome.getOriginalOutcome().getArm(), // we assign the same arm as the original value (which might be completely incorrect :'(
                    // TODO: it should depend on where we found the timepoint
                    otherOutcome.getCell().toText(),
                    otherOutcome.getOriginalOutcome().getSprintNo(),
                    otherOutcome.getOriginalOutcome().getAnnotationPage());
            return res;
        } else {
            // unexpected in the current implementation
            throw new AssertionError("Outcomes that are mined should have a header with a timepoint.");
        }
    }

    /**
     * Returns a list of table values, with a flag indicating whether they are outcome values (annotated or mined)
     * @param annotations the initial collection of annotations (with no table context normalization performed)
     * @param docName the name of a PDF document (including the trailing ".pdf" if relevant)
     * @return a list of OtherOutcomeCandidate, a structure storing the table values and their class
     * according to the outcome value detection
     * @throws IOException this method has to access the documents to parse tables
     * and might throw IOException when failing to do that
     */
    public List<OtherOutcomeCandidate> getContextCellsWithOutcomeStatus(AttributeValueCollection<AnnotatedAttributeValuePair> annotations, String docName) throws IOException {
        File pdfFile = new File(pdfFolder, docName);
        // parse the PDF
        try {
            Document doc = pdfParser.getDocument(pdfFile);
            TableValueFinder finder = new TableValueFinder(doc, true);
            // get all the OV annotations
            List<AnnotatedAttributeValuePair> outcomeValues = annotations.byDoc().get(docName).stream()
                    .filter(aavp -> aavp.getAttribute().isOutcomeValue())
                    .collect(Collectors.toList());
            // look at each annotation, spot its table, and classify each cell in the table
            Map<OtherOutcomeCandidate, CandidateStatus> res = new HashMap<>();
            for (AnnotatedAttributeValuePair outcomeValue : outcomeValues) {
                // find the table value containing the OV annotation
                Optional<TableValue> cell = finder.findTableValue(outcomeValue);
                if (cell.isPresent()) {
                    // compute the position of the actual OV number in the format (sometimes the raw count is first and the percentage is in parenthesis)
                    int position = getPositionOfOutcomeInTableValue(outcomeValue, cell.get());
                    // first add the annotation to the statuses
                    CandidateStatus.update(res, new OtherOutcomeCandidate(cell.get(),
                            true, true, outcomeValue, position), new CandidateStatus(true, true));
                    if (position >= 0) { // this means the OV has a number, we don't try anything otherwise
                        // analyze the format of the cell found in the table
                        List<NumberFormat> format = NumberFormat.getFormat(cell.get().getValue());
                        // get all the table values on the same row and the same column as the OV annotation cell
                        Pair<List<TableValue>, List<TableValue>> sameRowAndColumnValues = finder.getSameRowAndSameColumnValues(cell.get());
                        // get all other outcome values (the cells having similar format as the cell containing the known OV)
                        List<TableValue> allOtherOutcomes = getAllOtherOutcomesInTable(outcomeValue.getValue(), cell.get(), sameRowAndColumnValues);
                        for (TableValue otherCell : allOtherOutcomes) {
                            CandidateStatus.update(res, new OtherOutcomeCandidate(otherCell,
                                            true, false, outcomeValue, position), new CandidateStatus(true, false));
                        }
                    }
                    // consider all the remaining cells in the table as potential negatives (non-outcomes)
                    List<TableValue> allCellsInTable = cell.get().getTableBlock().getTable();
                    for (TableValue nonOutcomeCell : allCellsInTable) {
                        // cells already marked as outcomes or annotations will not be overridden
                        CandidateStatus.update(res, new OtherOutcomeCandidate(nonOutcomeCell,
                                false, false, outcomeValue, position), new CandidateStatus(false, false));
                    }
                }
            }
            // return all the cells with their candidate status
            List<OtherOutcomeCandidate> finalizedResult = new ArrayList<>();
            for (Map.Entry<OtherOutcomeCandidate, CandidateStatus> cellCandidate : res.entrySet()) {
                OtherOutcomeCandidate finalizedCandidate = new OtherOutcomeCandidate(
                        cellCandidate.getKey().getCell(),
                        cellCandidate.getValue().isOutcomeValue(),
                        cellCandidate.getValue().isAnnotation(),
                        cellCandidate.getKey().getOriginalOutcome(),
                        cellCandidate.getKey().getPositionOfNumber()
                );
                finalizedResult.add(finalizedCandidate);
            }
            return finalizedResult;
        } catch (FileNotFoundException fnfe) {
            // we couldn't find the parsed PDF
            return new ArrayList<>();
        }
    }

    public Pair<List<Block>, List<Block>> getGoldAndNonOutcomeTables(AttributeValueCollection<AnnotatedAttributeValuePair> annotations, String docName) throws IOException {
        File pdfFile = new File(pdfFolder, docName);
        // parse the PDF
        try {
            Document doc = pdfParser.getDocument(pdfFile);
            // initialize the non-outcome tables to all the tables by default
            List<Block> nonOutcomeTables = doc.getTables();
            Set<Block> outcomeTables = new HashSet<>();
            TableValueFinder finder = new TableValueFinder(doc, true);
            // get all the OV annotations
            List<AnnotatedAttributeValuePair> outcomeValues = annotations.byDoc().get(docName).stream()
                    .filter(aavp -> aavp.getAttribute().isOutcomeValue())
                    .collect(Collectors.toList());
            // look at each annotation, spot its table
            Map<OtherOutcomeCandidate, CandidateStatus> res = new HashMap<>();
            for (AnnotatedAttributeValuePair outcomeValue : outcomeValues) {
                // find the table value containing the OV annotation
                Optional<TableValue> cell = finder.findTableValue(outcomeValue);
                if (cell.isPresent()) {
                    // remove its table from the non-outcome list
                    nonOutcomeTables.remove(cell.get().getTableBlock());
                    // add it to the outcome tables
                    outcomeTables.add(cell.get().getTableBlock());
                }
            }
            return Pair.of(new ArrayList<>(outcomeTables), nonOutcomeTables);
        } catch (FileNotFoundException fnfe) {
            // we couldn't find the parsed PDF
            return null;
        }
    }

    @Value
    @EqualsAndHashCode(onlyExplicitlyIncluded = true)
    public static class OtherOutcomeCandidate {
        @EqualsAndHashCode.Include
        TableValue cell;
        boolean isOutcomeValue;
        boolean isAnnotation;
        AnnotatedAttributeValuePair originalOutcome;
        int positionOfNumber;
    }

    @Value
    private static class CandidateStatus {
        boolean isOutcomeValue;
        boolean isAnnotation;

        private static <E> void update(Map<E, CandidateStatus> statuses, E newElement, CandidateStatus newStatus) {
            CandidateStatus oldStatus = statuses.get(newElement);
            if (oldStatus != null) {
                newStatus = new CandidateStatus(
                        oldStatus.isOutcomeValue || newStatus.isOutcomeValue,
                        oldStatus.isAnnotation || newStatus.isAnnotation);
            }
            statuses.put(newElement, newStatus);
        }
    }

    private List<TableValue> getAllOtherOutcomesInTable(String value, TableValue cell, Pair<List<TableValue>, List<TableValue>> sameRowAndColumnValues) {
        List<NumberFormat> format = NumberFormat.getFormat(cell.getValue());
        // browse the entire row and the entire columns for similar-looking numbers
        List<TableValue> otherOutcomesInRow = getAllOtherOutcomesInRow(cell, format, sameRowAndColumnValues.getLeft());
        List<TableValue> otherOutcomesInColumn = getAllOtherOutcomesInRow(cell, format, sameRowAndColumnValues.getRight());
        // post process these 2 lists if necessary
        List<TableValue> res = new ArrayList<>();
        res.addAll(otherOutcomesInRow);
        res.addAll(otherOutcomesInColumn);
        // TODO: comment here to get the high recall version, but this one allows to also extract the timepoint easily
        // filter by cells that have headers with a mention of a timepoint
        res = res.stream()
                .filter(tv -> refersToTimepoint(tv))
                .collect(Collectors.toList());
        return res;
    }

    private List<TableValue> getAllOtherOutcomesInRow(TableValue cell, List<NumberFormat> format, List<TableValue> sameRow) {
        return sameRow.stream()
                .filter(tv -> {
                    List<NumberFormat> candidateFormat = NumberFormat.getFormat(tv.getValue());
                    return format.equals(candidateFormat);
                })
                .collect(Collectors.toList());
    }

    private int getPositionOfOutcomeInTableValue(AnnotatedAttributeValuePair outcomeValue, TableValue cell) {
        List<Double> annotationNumbers = ParsingUtils.parseAllDoubles(outcomeValue.getValue());
        if (annotationNumbers.isEmpty()) {
            // numeric OV not found (not frequent)
            return -1;
        } else {
            // the first number of the annotation value is the OV
            double ov = ParsingUtils.parseFirstDouble(outcomeValue.getValue());
            // the table value can contain multiple numbers
            List<Double> numbersInTableValue = ParsingUtils.parseAllDoubles(cell.getValue());
            return numbersInTableValue.indexOf(ov);
        }
    }

    private boolean refersToTimepoint(TableValue cell) {
        return TIMEPOINT_KEYWORDS.stream()
                .anyMatch(keyword -> Streams.concat(cell.getRowHeaders().stream(), cell.getColumnHeaders().stream())
                        .anyMatch(header -> header.contains(keyword)));
    }

    private String getNumericValue(String s, int position) {
        return ParsingUtils.parseAllDoubleStrings(s).get(position);
    }

    private List<AnnotatedAttributeValuePair> getArmValuesWithOverriddenAttribute(Collection<AnnotatedAttributeValuePair> values,
                                                                                  Attribute attribute,
                                                                                  String newValue,
                                                                                  AnnotatedAttributeValuePair templateAvp) {
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        // keep the other values
        List<AnnotatedAttributeValuePair> toKeep = values.stream()
                .filter(aavp -> !aavp.getAttribute().equals(attribute))
                .collect(Collectors.toList());
        res.addAll(toKeep);
        // replace the attribute to override with a new value
        AnnotatedAttributeValuePair newAvp = templateAvp.withAttribute(attribute).withValue(newValue);
        res.add(newAvp);
        return res;
    }
}
