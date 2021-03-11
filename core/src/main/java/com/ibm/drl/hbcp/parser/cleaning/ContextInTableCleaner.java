package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;
import lombok.Value;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Replaces contexts found in tables with template sentences (using automatically detected headers and the value).
 * There is a guarantee that this cleaner will update at most the context, it will never change the value.
 *
 * @author marting
 */
public class ContextInTableCleaner implements Cleaner {

    private final File pdfFolder;
    private final PdfToDocumentFunction pdfToDocument;
    private final boolean useStrictCellEquality;

    private double total = 0.0;

    public ContextInTableCleaner(Properties props, boolean useStrictCellEquality) {
        pdfToDocument = new ReparsePdfToDocument(props);
        pdfFolder = FileUtils.potentiallyGetAsResource(new File(props.getProperty("coll")));
        this.useStrictCellEquality = useStrictCellEquality;
    }

    public ContextInTableCleaner(Properties props) {
        this(props, false);
    }

    @Override
    public List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original) {
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        // cluster the values by papers
        AttributeValueCollection<AnnotatedAttributeValuePair> collection = new AttributeValueCollection<>(original);
        for (String docname : collection.byDoc().keySet()) {
            Multiset<AnnotatedAttributeValuePair> avps = collection.byDoc().get(docname);
            try {
                // get for each paper the Abbyy output (pass the Abbyy path in the constructor)
                Document document = pdfToDocument.getDocument(new File(pdfFolder, docname));
                TableValueFinder cellFinder = new TableValueFinder(document, useStrictCellEquality);
                List<AnnotatedAttributeValuePair> cleanedAvps = clean(new ArrayList<>(avps), cellFinder);
                res.addAll(cleanedAvps);
            } catch (IOException e) {
                // the parse file wasn't found, most likely, take the original AVPs
                // TODO: or take nothing?
                res.addAll(avps);
            }
        }
        return res;
    }

    private List<AnnotatedAttributeValuePair> clean(List<AnnotatedAttributeValuePair> avpsForDoc, TableValueFinder cellFinder) {
        // match AVPs into a potential value found in a table (others, that couldn't be found, will be returned as-is)
        List<Pair<Optional<TableValue>, AnnotatedAttributeValuePair>> tableFinderResults = avpsForDoc.stream()
                .map(avp -> Pair.of(cellFinder.findTableValue(avp), avp))
                .collect(Collectors.toList());
        // get only the annotations in a table cell (the clean context is the cell's row/column headers)
        List<AnnotatedTableValue> tableValues = tableFinderResults.stream()
                .filter(maybeRawCellAndAvp -> maybeRawCellAndAvp.getLeft().isPresent())
                .map(rawCellAndAvp -> new AnnotatedTableValue(rawCellAndAvp.getLeft().get(), rawCellAndAvp.getRight()))
                .collect(Collectors.toList());
        // post process the cleaned AVPs with heuristics designed to eliminate noisy cleanings
        List<AnnotatedTableValue> wellDefinedTableValues = postProcessTableCells(tableValues);
        // create the new contexts
        List<AnnotatedAttributeValuePair> cleanedTableValues = wellDefinedTableValues.stream()
                .map(annotatedTableValue -> annotatedTableValue.getAvp().withContext(annotatedTableValue.getRawCell().toText()))
                .collect(Collectors.toList());
        // get the original values that weren't found in tables at all
        List<AnnotatedAttributeValuePair> originalNonTableValues = tableFinderResults.stream()
                .filter(maybeRawCellAndAvp -> !maybeRawCellAndAvp.getLeft().isPresent())
                .map(Pair::getRight)
                .collect(Collectors.toList());
        // results: the cleaned table values + the original non-table values
        List<AnnotatedAttributeValuePair> allCleanValues = new ArrayList<>(cleanedTableValues);
        allCleanValues.addAll(originalNonTableValues);
        return allCleanValues;
    }

    private List<AnnotatedTableValue> postProcessTableCells(List<AnnotatedTableValue> cells) {
        // remove cells with either column/row headers empty
        cells = cells.stream()
                .filter(cell -> !cell.getRawCell().getColumnHeaders().isEmpty() && !cell.getRawCell().getRowHeaders().isEmpty())
                .collect(Collectors.toList());
        // remove the cells which have exactly the same headers as another cell (meaning ambiguous headers)
        Multiset<Pair<List<String>, List<String>>> headerPairs = HashMultiset.create();
        for (AnnotatedTableValue cell : cells) {
            headerPairs.add(Pair.of(cell.getRawCell().getRowHeaders(), cell.getRawCell().getColumnHeaders()));
        }
        Set<Pair<List<String>, List<String>>> unambiguousHeaderPairs = new HashSet<>();
        for (Pair<List<String>, List<String>> headerPair : headerPairs) {
            if (headerPairs.count(headerPair) == 1) {
                unambiguousHeaderPairs.add(headerPair);
            }
        }
        return cells.stream()
                .filter(cell -> unambiguousHeaderPairs.contains(Pair.of(cell.getRawCell().getRowHeaders(), cell.getRawCell().getColumnHeaders())))
                .collect(Collectors.toList());
    }

    private static AttributeValueCollection<AnnotatedAttributeValuePair> clean(AttributeValueCollection<AnnotatedAttributeValuePair> aavps, boolean useStrictCellEquality) throws IOException {
        Cleaner cleaner = new ContextInTableCleaner(Props.loadProperties(), useStrictCellEquality);
        // first apply the numeric value cleaner
        Cleaner numericValueCleaner = new NumericValueCleaner(Arrays.asList(Props.loadProperties().getProperty("prediction.attribtype.numerical").split(",")));
        aavps = new AttributeValueCollection<>(numericValueCleaner.clean(aavps));
        // then clean only the contexts
        return new AttributeValueCollection<>(cleaner.clean(aavps));
    }

    @Value
    private static class AnnotatedTableValue {
        TableValue rawCell;
        AnnotatedAttributeValuePair avp;
    }

    public static void mainCompareCellEqualities(String[] args) throws IOException {
        JSONRefParser parser = new JSONRefParser(Props.getDefaultPropFilename());
        AttributeValueCollection<AnnotatedAttributeValuePair> aavps = parser.getAttributeValuePairs();
        System.out.println("Values: " + aavps.size());
        AttributeValueCollection<AnnotatedAttributeValuePair> cleanStrict = clean(aavps, true);
        AttributeValueCollection<AnnotatedAttributeValuePair> cleanInclusion = clean(aavps,false);
        Set<AnnotatedAttributeValuePair> deltaStrict = Cleaner.delta(cleanStrict, cleanInclusion);
        // displays all the new AAVP contexts
        System.out.println("Strict exclusives: ");
        for (AnnotatedAttributeValuePair avp : deltaStrict) {
            System.out.println("==================");
            System.out.println(avp.getContext());
            System.out.println("Value: " + avp.getValue() + " (" + avp.getAttribute().getName() + ")");
        }
        Set<AnnotatedAttributeValuePair> deltaInclusion = Cleaner.delta(cleanInclusion, cleanStrict);
        System.out.println("Inclusion exclusives: ");
        for (AnnotatedAttributeValuePair avp : deltaInclusion) {
            System.out.println("==================");
            System.out.println(avp.getDocName());
            System.out.println(avp.getContext());
            System.out.println("Value: " + avp.getValue() + " (" + avp.getAttribute().getName() + ")");
        }
        System.out.println(Cleaner.delta(cleanStrict, aavps).size());
    }

    public static void main(String[] args) throws IOException {
        JSONRefParser parser = new JSONRefParser(Props.getDefaultPropFilename());
        AttributeValueCollection<AnnotatedAttributeValuePair> aavps = parser.getAttributeValuePairs();
        System.out.println("Values: " + aavps.size());
        // first apply the numeric value cleaner
        Cleaner numericValueCleaner = new NumericValueCleaner(Arrays.asList(Props.loadProperties().getProperty("prediction.attribtype.numerical").split(",")));
        aavps = numericValueCleaner.getCleaned(aavps);
        AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = clean(aavps, false);
        List<AnnotatedAttributeValuePair> delta = new ArrayList<>(Cleaner.delta(cleaned, aavps));
        Collections.shuffle(delta);
        int i = 0;
        for (AnnotatedAttributeValuePair avp : delta) {
            System.out.println("==================");
            System.out.println(avp.getContext());
            System.out.println("Value: " + avp.getValue() + " (" + avp.getAttribute().getName() + ")");
            i++;
            if (i == 50) break;
        }
        double charLength = 0.0;
        for (AnnotatedAttributeValuePair avp : delta) {
            charLength += avp.getContext().length();
        }
        System.out.println(delta.size());
        System.out.println(charLength / delta.size());
    }
}
