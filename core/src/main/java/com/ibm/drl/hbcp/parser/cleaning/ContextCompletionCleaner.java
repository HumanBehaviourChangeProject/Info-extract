package com.ibm.drl.hbcp.parser.cleaning;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.Properties;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extends the context of numeric-type attributes when the context doesn't contain the value.
 *
 * Assumes that the NumericValueCleaner has been run before, equivalently, that the presence of the value
 * in the context can be checked with a simple String.contains().
 *
 * @author marting
 */
public class ContextCompletionCleaner extends NumericTypeCleaner {

    private final ContextExtractor contextExtractor;

    private static final int MAXIMUM_SENTENCES = 5;

    // internal stats
    private int totalAVPs = 0;
    private int contextIncompleteAVPs = 0;
    private int cleanedAVPs = 0;
    private int notCleanedAVPs = 0;

    public ContextCompletionCleaner(Properties props, List<String> numericAttributeIds) {
        super(numericAttributeIds);
        File pdfFolder = FileUtils.potentiallyGetAsResource(new File(props.getProperty("coll")));
        PdfToDocumentFunction pdfToDocument = new ReparsePdfToDocument(props);
        contextExtractor = new ContextExtractor(pdfFolder, pdfToDocument,
                MAXIMUM_SENTENCES, 0, 0);
    }

    /** Extends the context of a numeric-type AVP. Assumes the AVP is numeric AND has been passed through the NumericValueCleaner */
    @Override
    protected AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair numericTypeAvp) {
        totalAVPs++;
        if (contextExtractor.isContextIncomplete(numericTypeAvp)) {
            contextIncompleteAVPs++;
            // display the incomplete context here if you want, during debugging (convenient)
            Optional<String> updatedContext = contextExtractor.getExtendedContext(numericTypeAvp);
            if (updatedContext.isPresent()) {
                // the cleaning has changed the context
                cleanedAVPs++;
            } else {
                notCleanedAVPs++;
            }
            return numericTypeAvp.withContext(updatedContext.orElse(numericTypeAvp.getContext()));
        } else {
            return numericTypeAvp;
        }
    }

    public static void main(String[] args) throws IOException {
        ContextCompletionCleaner cleaner = new ContextCompletionCleaner(Props.loadProperties(), Lists.newArrayList());
        JSONRefParser parser = new JSONRefParser(Props.getDefaultPropFilename());
        AttributeValueCollection<AnnotatedAttributeValuePair> aavps = parser.getAttributeValuePairs();
        // first apply the numeric value cleaner
        Cleaner numericValueCleaner = new NumericValueCleaner(Arrays.asList(Props.loadProperties().getProperty("prediction.attribtype.numerical").split(",")));
        aavps = numericValueCleaner.getCleaned(aavps);
        // then clean only the contexts
        AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaner.getCleaned(aavps);
        Multiset<AnnotatedAttributeValuePair> delta = HashMultiset.create(cleaned);
        delta.removeAll(aavps.getAllPairs());
        // displays all the new AAVP contexts
        for (AnnotatedAttributeValuePair avp : delta) {
            System.out.println("=========================================");
            System.out.println(avp.getContext());
            System.out.println("\tcontains " + avp.getValue());
        }
        System.out.println(delta.size());
        System.out.println("Total: " + cleaner.totalAVPs);
        System.out.println("Context incomplete: " + cleaner.contextIncompleteAVPs);
        System.out.println("Cleaned: " + cleaner.cleanedAVPs);
        System.out.println("Not cleaned: " + cleaner.notCleanedAVPs);
    }
}
