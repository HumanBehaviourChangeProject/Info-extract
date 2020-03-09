package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
import lombok.Data;
import lombok.Getter;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Replaces contexts found in tables with template sentences (using automatically detected headers and the value)
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
        this(props, true);
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
                for (AnnotatedAttributeValuePair avp : avps) {
                    res.add(clean(avp, cellFinder));
                }
            } catch (IOException e) {
                // the parse file wasn't found, most likely, add the original AVPs
                res.addAll(avps);
            }
        }
        return res;
    }

    private AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair avp, TableValueFinder cellFinder) {
        Optional<TableValue> cell = cellFinder.findTableValue(avp);
        if (cell.isPresent()) {
            TableValue value = cell.get();
            // modify the context with the artificial template sentence
            String newContext = value.toText();
            return avp.withContext(newContext);
        } else {
            return avp;
        }
    }

    private static AttributeValueCollection<AnnotatedAttributeValuePair> clean(AttributeValueCollection<AnnotatedAttributeValuePair> aavps, boolean useStrictCellEquality) throws IOException {
        Cleaner cleaner = new ContextInTableCleaner(Props.loadProperties(), useStrictCellEquality);
        // first apply the numeric value cleaner
        Cleaner numericValueCleaner = new NumericValueCleaner(Arrays.asList(Props.loadProperties().getProperty("prediction.attribtype.numerical").split(",")));
        aavps = new AttributeValueCollection<>(numericValueCleaner.clean(aavps));
        // then clean only the contexts
        return new AttributeValueCollection<>(cleaner.clean(aavps));
    }

    public static void main(String[] args) throws IOException {
        JSONRefParser parser = new JSONRefParser(Props.getDefaultPropFilename());
        AttributeValueCollection<AnnotatedAttributeValuePair> aavps = parser.getAttributeValuePairs();
        System.out.println("Values: " + aavps.size());
        AttributeValueCollection<AnnotatedAttributeValuePair> cleanStrict = clean(aavps, true);
        AttributeValueCollection<AnnotatedAttributeValuePair> cleanInclusion = clean(aavps,false);
        Set<AnnotatedAttributeValuePair> deltaStrict = Cleaner.delta(cleanStrict, cleanInclusion);
        // displays all the new AAVP contexts
        System.out.println("Strict exclusives: ");
        for (AnnotatedAttributeValuePair avp : deltaStrict) {
            //System.out.println("==================");
            //System.out.println(avp.getContext());
            //System.out.println("Value: " + avp.getValue() + " (" + avp.getAttribute().getName() + ")");
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
}
