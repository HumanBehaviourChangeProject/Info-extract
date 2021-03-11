package com.ibm.drl.hbcp.experiments.ie.armification.twoarms;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.ContextExtractor;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.util.ControlGroupArmNames;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVWriter;

import opennlp.tools.sentdetect.SentenceDetector;

public class CsvGenerator {

    private static final File CONTROL_ARM_NAMES = new File("data/armification/ControlGroupArmNames.txt");
    private static final SentenceDetector sbd = ContextExtractor.newSentenceDetector();

    private final boolean onlyOnePerDoc;
    private final boolean noEmptyArm;
    private final int sentencesBefore;
    private final int sentencesAfter;

    private ContextExtractor contextExtractor;

    public CsvGenerator(boolean onlyOnePerDoc, boolean noEmptyArm, int sentencesBefore, int sentencesAfter) throws IOException {
        this.onlyOnePerDoc = onlyOnePerDoc;
        this.noEmptyArm = noEmptyArm;
        this.sentencesBefore = sentencesBefore;
        this.sentencesAfter = sentencesAfter;
        contextExtractor = new ContextExtractor(new File("data/pdfs_413"), new ReparsePdfToDocument(Props.loadProperties()),
                5, sentencesBefore, sentencesAfter);
    }

    public void writeCsvDataset(File trainCsv, File valCsv) throws IOException {
        // read the control arm annotations
        ControlGroupArmNames controlGroups = new ControlGroupArmNames(CONTROL_ARM_NAMES);
        // read the regular annotations
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        AttributeValueCollection<AnnotatedAttributeValuePair> twoArmData = controlGroups.restrictToTwoArmPapers(annotations);
        AttributeValueCollection<AnnotatedAttributeValuePair> twoArmInterventions = new AttributeValueCollection<>(twoArmData.stream()
                .filter(aavp -> aavp.getAttribute().getType() == AttributeType.INTERVENTION)
                .collect(Collectors.toList()));
        // split between train, val (and test eventually)
        List<String> allDocs = new ArrayList<>(twoArmInterventions.getDocNames());
        TrainTestSplitter splitter = new TrainTestSplitter(0.8);
        List<Pair<List<String>, List<String>>> trainTestDocs = splitter.getTrainTestSplits(allDocs);
        writeCsvDataset(trainCsv, twoArmInterventions.filterByDocs(trainTestDocs.get(0).getLeft()));
        writeCsvDataset(valCsv, twoArmInterventions.filterByDocs(trainTestDocs.get(0).getRight()));
    }

    private void writeCsvDataset(File outputCsv, Collection<AnnotatedAttributeValuePair> instances) throws IOException {
        int id = 0;
        outputCsv.getParentFile().mkdirs();
        try (CSVWriter w = new CSVWriter(new FileWriter(outputCsv))) {
            // write header
            w.writeNext(new String[] { "id", "context", "value", "doc", "arm", "arm_id", "label" });
            for (AnnotatedAttributeValuePair bct : instances) {
                // potentially skip the empty-arm interventions
                if (noEmptyArm && bct.getArm().equals(Arm.EMPTY))
                    continue;
                // TODO: potentially only pick one arm per doc
                // extend the context of the intervention
                String value = postProcessStrings(bct.getValue());
                String newContext = postProcessContext(contextExtractor.getContext(bct), bct.getValue());
                int label = getLabel(bct.getArm());
                w.writeNext(new String[] {
                        String.valueOf(id++),
                        newContext,
                        value,
                        bct.getDocName(),
                        bct.getArm().getStandardName(),
                        bct.getArm().getId(),
                        String.valueOf(label)
                });
            }
        }
    }

    private void writeArmEntailmentDataset(File outputCsv, Collection<AnnotatedAttributeValuePair> instances) throws IOException {
        int id = 0;
        outputCsv.getParentFile().mkdirs();
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = new AttributeValueCollection<>(instances);
        try (CSVWriter w = new CSVWriter(new FileWriter(outputCsv))) {
            // write header
            w.writeNext(new String[] { "id", "context", "value", "context+arm", "doc", "arm", "arm_id", "label" });
            for (AnnotatedAttributeValuePair avp : instances) {
                // get all the named arms relevant to that AVP
                Set<Arm> arms = avps.getArmifiedPairsInDoc(avp.getDocName()).keySet().stream()
                        .filter(arm -> !arm.equals(Arm.EMPTY))
                        .collect(Collectors.toSet());
                // extend the context of the value
                String value = postProcessStrings(avp.getValue());
                /*
                String newContext = postProcessContext(contextExtractor.getContext(bct), bct.getValue());
                int label = getLabel(bct.getArm());
                w.writeNext(new String[] {
                        String.valueOf(id++),
                        newContext,
                        value,
                        bct.getDocName(),
                        bct.getArm().getStandardName(),
                        bct.getArm().getId(),
                        String.valueOf(label)
                });
                */
            }
        }
    }

    private int getLabel(Arm arm) {
        // arm can be either EMPTY, MAIN, or CONTROL
        if (arm.equals(Arm.CONTROL)) {
            return 0;
        } else if (arm.equals(Arm.MAIN)) {
            return 1;
        } else {
            return 2;
        }
    }

    private String postProcessStrings(String context) {
        context = context.replaceAll("\n", " ");
        return context;
    }

    private String postProcessContext(String context, String value) {
        // sbd and add the Bert tokens
        String[] sentences = sbd.sentDetect(context);
        return StringUtils.join(Arrays.stream(sentences)
                //.map(s -> markValue(s, value)) // mark the value inside the context
                .map(s -> "[CLS] " + s + " [SEP]") // add the BERT tokens
                .map(s -> postProcessStrings(s))
                .collect(Collectors.toList())
        , " ");
    }

    private String markValue(String context, String value) {
        return StringUtils.replace(context, value, "[SVAL] " + value + " [EVAL]");
    }

    public static void main(String[] args) throws IOException {
        CsvGenerator gen = new CsvGenerator(false, true, 1, 0);
        gen.writeCsvDataset(new File("output/twoarmclassif_train.csv"), new File("output/twoarmclassif_val.csv"));
        System.out.println("Done.");
    }
}
