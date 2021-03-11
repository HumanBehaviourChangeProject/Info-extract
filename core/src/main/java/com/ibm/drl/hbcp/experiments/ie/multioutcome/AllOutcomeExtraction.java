package com.ibm.drl.hbcp.experiments.ie.multioutcome;

import com.google.common.collect.Iterables;
import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.TableValueFinder;
import com.ibm.drl.hbcp.parser.enriching.NumberFormat;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class AllOutcomeExtraction {

    private static final File json = new File("data/jsons/MultipleOutcomes_1Dec19.json");
    private static final List<String> TIMEPOINT_KEYWORDS = Lists.newArrayList("day", "week", "month");

    private static RefComparison evaluate(List<String> predictions, List<AnnotatedAttributeValuePair> refOutcomes) {
        Set<String> predictionSet = new HashSet<>(predictions);
        Set<String> refSet = refOutcomes.stream()
                .map(AnnotatedAttributeValuePair::getValue)
                .map(AllOutcomeExtraction::getNumericValue)
                .collect(Collectors.toSet());
        long tp = predictionSet.stream()
                .filter(v -> refSet.contains(v))
                .count();
        long fp = predictionSet.stream()
                .filter(v -> !refSet.contains(v))
                .count();
        long fn = refSet.stream()
                .filter(v -> !predictionSet.contains(v))
                .count();
        return new RefComparison((int)tp, (int)fp, (int)fn, 0);
    }

    private static String getNumericValue(String s, int position) {
        return ParsingUtils.parseAllDoubleStrings(s).get(position);
    }

    private static String getNumericValue(String s) {
        return getNumericValue(s, 0);
    }

    private static List<TableValue> getAllOtherOutcomesInTable(String value, TableValue cell, Pair<List<TableValue>, List<TableValue>> sameRowAndColumnValues) {
        List<NumberFormat> format = NumberFormat.getFormat(cell.getValue());
        // browse the entire row and the entire columns for similar-looking numbers
        List<TableValue> otherOutcomesInRow = getAllOtherOutcomesInRow(cell, format, sameRowAndColumnValues.getLeft());
        List<TableValue> otherOutcomesInColumn = getAllOtherOutcomesInRow(cell, format, sameRowAndColumnValues.getRight());
        // post process these 2 lists if necessary
        List<TableValue> res = new ArrayList<>();
        res.addAll(otherOutcomesInRow);
        res.addAll(otherOutcomesInColumn);
        // TODO: comment here to get the high recall version
        // filter by cells that have headers with a mention of a timepoint
        res = res.stream()
                .filter(tv -> refersToTimepoint(tv))
                .collect(Collectors.toList());
        return res;
    }

    private static List<TableValue> getAllOtherOutcomesInRow(TableValue cell, List<NumberFormat> format, List<TableValue> sameRow) {
        return sameRow.stream()
                .filter(tv -> {
                    List<NumberFormat> candidateFormat = NumberFormat.getFormat(tv.getValue());
                    return format.equals(candidateFormat);
                })
                .collect(Collectors.toList());
    }

    private static int getPositionOfOutcomeInTableValue(AnnotatedAttributeValuePair outcomeValue, TableValue cell) {
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

    private static boolean refersToTimepoint(TableValue cell) {
        return TIMEPOINT_KEYWORDS.stream()
                .anyMatch(keyword -> Streams.concat(cell.getRowHeaders().stream(), cell.getColumnHeaders().stream())
                        .anyMatch(header -> header.contains(keyword)));
    }

    private static List<AnnotatedAttributeValuePair> getFullReferences(List<AnnotatedAttributeValuePair> outcomeValues,
                                                                       List<AnnotatedAttributeValuePair> otherOutcomeValues,
                                                                       TableValueFinder tableValueFinder) {
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        // first check the regular OVs (longest follow up)
        for (AnnotatedAttributeValuePair outcomeValue : outcomeValues) {
            // find the table value containing the OV annotation (take the first one, TODO: check that this is ok)
            Optional<TableValue> cell = tableValueFinder.findTableValue(outcomeValue);
            if (cell.isPresent()) {
                res.add(outcomeValue);
            }
        }
        // ONLY if there were regular OVs in table, do we accept the challenge of extracting the others too
        if (!res.isEmpty()) {
            for (AnnotatedAttributeValuePair outcomeValue : Iterables.concat(outcomeValues, otherOutcomeValues)) {
                // find the table value containing the OV annotation (take the first one, TODO: check that this is ok)
                Optional<TableValue> cell = tableValueFinder.findTableValue(outcomeValue);
                if (cell.isPresent()) {
                    res.add(outcomeValue);
                }
            }
        }
        return res;
    }

    public static void main(String[] args) throws IOException {
        JSONRefParser parser = new JSONRefParser(FileUtils.potentiallyGetAsResource(json));
        Properties props = Props.loadProperties();
        PdfToDocumentFunction pdfToDocument = new ReparsePdfToDocument(props);
        File folder = new File(props.getProperty("coll"));
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = parser.getAttributeValuePairs();
        // evaluate on all annotated documents
        int totalReferenceOutcomeValues = 0;
        RefComparison finalEval = new RefComparison(0, 0, 0, 0);
        for (String docname : annotations.getDocNames()) {
            File pdfFile = new File(folder, docname);
            try {
                // parse the PDF
                Document doc = pdfToDocument.getDocument(pdfFile);
                TableValueFinder finder = new TableValueFinder(doc, true);
                // get all the OV annotations
                List<AnnotatedAttributeValuePair> outcomeValues = annotations.byDoc().get(docname).stream()
                        .filter(aavp -> aavp.getAttribute().getName().equals("Outcome value"))
                        .collect(Collectors.toList());
                totalReferenceOutcomeValues += outcomeValues.size();
                // get the other-OV annotations
                List<AnnotatedAttributeValuePair> otherOutcomeValues = annotations.byDoc().get(docname).stream()
                        .filter(aavp -> aavp.getAttribute().getName().equals("PrimaryOutcome"))
                        //    || aavp.getAttribute().getName().equals("OtherOutcomes"))
                        .collect(Collectors.toList());
                //System.out.println("Starting outcomes: " + refs.stream().map(AttributeValuePair::getValue).distinct().collect(Collectors.joining(", ")));
                List<AnnotatedAttributeValuePair> refs = getFullReferences(outcomeValues, otherOutcomeValues, finder);
                List<String> preds = new ArrayList<>();
                for (AnnotatedAttributeValuePair outcomeValue : outcomeValues) {
                    // find the table value containing the OV annotation (take the first one, TODO: check that this is ok)
                    Optional<TableValue> cell = finder.findTableValue(outcomeValue);
                    if (cell.isPresent()) {
                        // compute the position of the actual OV number in the format (sometimes the raw count is first and the percentage is in parenthesis)
                        int position = getPositionOfOutcomeInTableValue(outcomeValue, cell.get());
                        preds.add(getNumericValue(outcomeValue.getValue()));
                        // analyze the format of the cell found in the table
                        List<NumberFormat> format = NumberFormat.getFormat(cell.get().getValue());
                        System.out.println("=============================");
                        System.out.println(docname);
                        System.out.println("OV: " + outcomeValue.getValue());
                        System.out.println("in: " + cell.get().toText());
                        System.out.println(format);
                        System.out.println("-----------------------------");
                        // get all the table values on the same row and the same column as the OV annotation cell
                        Pair<List<TableValue>, List<TableValue>> sameRowAndColumnValues = finder.getSameRowAndSameColumnValues(cell.get());
                        // get all other outcome values (the cells having similar format as the cell containing the known OV)
                        List<TableValue> allOtherOutcomes = getAllOtherOutcomesInTable(outcomeValue.getValue(), cell.get(), sameRowAndColumnValues);
                        // get the actual number and only it
                        List<String> actualExtraOutcomes = allOtherOutcomes.stream()
                                .map(tv -> getNumericValue(tv.getValue(), position))
                                .collect(Collectors.toList());
                        preds.addAll(actualExtraOutcomes);
                        System.out.println("Other outcome cells in same row/column:");
                        for (TableValue sameRowValue : allOtherOutcomes) {
                            System.out.println(sameRowValue.toText());
                        }
                        System.out.println("Annotations: " + refs.stream().map(AttributeValuePair::getValue).distinct().collect(Collectors.joining(", ")));
                    }
                }
                if (!refs.isEmpty()) {
                    System.out.println("~~~Summary~~~" + docname + "~~~~~~~~~~~~~~~~");
                    System.out.println("Refs: " + refs.stream()
                            .map(aavp -> getNumericValue(aavp.getValue()))
                            .distinct()
                            .sorted()
                            .collect(Collectors.joining(" - ")));
                    System.out.println("Preds: " + preds.stream()
                            .distinct()
                            .sorted()
                            .collect(Collectors.joining(" - ")));
                }
                // we can now evaluate
                RefComparison evalOnDoc = evaluate(preds, refs);
                finalEval.addConfusionMatrixCounts(evalOnDoc);
            } catch (IOException e) {
                System.err.println(e);
            }
        }
        System.out.println(finalEval);
        System.out.println("Doc count: " + annotations.getDocNames().size());
        System.out.println(totalReferenceOutcomeValues + " reference outcome values.");
    }
}
