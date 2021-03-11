package com.ibm.drl.hbcp.parser.enriching;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Random;
import java.util.stream.Collectors;

import org.junit.Assert;
import org.junit.Test;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeNameNumberTriple;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaner;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVWriter;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import edu.emory.mathcs.backport.java.util.Collections;

public class AnnotationOutcomesMinerTest {
    private final File folder = new File("src/main/resources/data/pdfs_extracted");
    private final PdfToDocumentFunction parser = new ReparsePdfToDocument(folder);
    private AnnotationOutcomesMiner miner = new AnnotationOutcomesMiner(parser, folder);

    private static final Logger log = (Logger)LoggerFactory.getLogger(AnnotationOutcomesMinerTest.class);

    public AnnotationOutcomesMinerTest() throws IOException {
    }

    @Test
    public void testOutcomeMinerNonEmpty() throws IOException {
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        for (String docName : annotations.getDocNames()) {
            try {
                List<AnnotatedAttributeNameNumberTriple> otherOutcomes = miner.getOtherOutcomesWithTimepoints(annotations, docName);
                // just enough to find one document which we can enrich
                if (!otherOutcomes.isEmpty()) return;
            } catch (FileNotFoundException fnfe) {
                // this PDF wasn't parsed to a JSON parse file in the provided folder, not a big deal
            }
        }
        // if we didn't find any document we can enrich with other outcomes, just fail this test
        Assert.fail("No other outcome value could be found in " + annotations.getDocNames().size() + " documents.");
    }

    @Test
    public void testAddOutcomesAndFollowups() throws IOException {
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        long countOutcomes = countOutcomeValues(annotations);
        long countFollowups = countFollowUps(annotations);
        // enrich
        AttributeValueCollection<AnnotatedAttributeValuePair> enriched = miner.withOtherOutcomeAndFollowupSeparate(annotations);
        long newCountOutcomes = countOutcomeValues(enriched);
        long newCountFollowups = countFollowUps(enriched);
        log.setLevel(Level.INFO);
        log.info("Before enriching: {} outcomes and {} follow-ups.", countOutcomes, countFollowups);
        log.info("After enriching: {} outcomes and {} follow-ups.", newCountOutcomes, newCountFollowups);
        log.setLevel(Level.DEBUG);
        // compare the counts
        Assert.assertTrue(newCountOutcomes > countOutcomes);
        Assert.assertTrue(newCountFollowups > countFollowups);
    }

    //@Test
    public void testDeltaEnrichingCsvSampling() throws IOException {
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        // enrich
        AttributeValueCollection<AnnotatedAttributeValuePair> enriched = miner.withOtherOutcomeAndFollowupSeparate(annotations);
        // delta
        List<AnnotatedAttributeValuePair> extraOutcomes = new ArrayList<>(Cleaner.delta(enriched, annotations)).stream()
                .filter(aavp -> aavp.getAttribute().isOutcomeValue())
                .collect(Collectors.toList());
        // sample 50
        Collections.shuffle(extraOutcomes, new Random(0));
        List<AnnotatedAttributeValuePair> sample = extraOutcomes.stream().limit(50).collect(Collectors.toList());
        try (CSVWriter w = new CSVWriter(new FileWriter(new File("output/otherOutcomes.csv")))) {
            String[] headers = {
                    "id",
                    "value",
                    "context",
                    "docname",
                    "arm"
            };
            w.writeNext(headers);
            int id = 0;
            for (AnnotatedAttributeValuePair outcome : sample) {
                String[] line = {
                        String.valueOf(id++),
                        outcome.getSingleLineValue(),
                        outcome.getContext(),
                        outcome.getDocName(),
                        outcome.getArm().getStandardName()
                };
                w.writeNext(line);
            }
        }
    }

    private long countOutcomeValues(AttributeValueCollection<AnnotatedAttributeValuePair> annotations) {
        return annotations.stream().filter(aavp -> aavp.getAttribute().isOutcomeValue()).count();
    }

    private long countFollowUps(AttributeValueCollection<AnnotatedAttributeValuePair> annotations) {
        return annotations.stream().filter(aavp -> aavp.getAttribute().getName().equals("Longest follow up")).count();
    }
}
