package com.ibm.drl.hbcp.extraction.extractors.flair;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.extractors.ArmAssociator;
import com.ibm.drl.hbcp.extraction.extractors.EvaluatedExtractor;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.cleaning.ContextExtractor;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.Props;

import opennlp.tools.sentdetect.SentenceDetector;

/**
 * Information extractor for all entities, based on calls to a Flair REST API.
 *
 * Can take potentially minutes for a single document if Flair is running on CPU and not GPU.
 *
 * @author mgleize
 */
public class InformationExtractorFlair implements EvaluatedExtractor<NamedParsedDocument, ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>> {

    private final FlairServiceConnector flairServiceConnector = FlairServiceConnector.createForLocalService();
    private final ArmAssociator armAssociator = new ArmAssociator();
    private final ArmNameClusterer clusterer = new ArmNameClusterer(2);
    // TODO: I would like to use this instead of the code to generate sentences in FlairServiceConnector but it yields exceptions
    private final SentenceDetector sbd = ContextExtractor.newSentenceDetector();

    public List<List<CandidateInPassage<ArmifiedAttributeValuePair>>> extract(List<NamedParsedDocument> docs) {
        List<List<CandidateInPassage<ArmifiedAttributeValuePair>>> res = new ArrayList<>();
        // get the sentences (per doc)
        List<List<String>> sentences = docs.stream()
                .map(doc -> flairServiceConnector.generateTestingSentence(doc.getDocument()))
                .collect(Collectors.toList());
        // sentence counts for each doc (to split the Flair output at the end)
        List<Integer> sentenceCounts = sentences.stream().map(List::size).collect(Collectors.toList());
        // send the sentences to the Flair service and get the output in JSON format
        // THIS SHOULD CONTAIN ONLY 1 JSON LINE/OBJECT AT THIS POINT
        List<String> jsonOutputs = getFlairJsonOutput(sentences.stream().flatMap(Collection::stream).collect(Collectors.toList()));
        for (String jsonOutput : jsonOutputs) {
            // TODO: see if we can remove this quick&dirty fix
            // strip the output of the invalid JSON ("result": ...), for some reason there is a discrepancy here between Java/Python APIs
            jsonOutput = stripTopLevelJsonElement(jsonOutput);
            // convert the json output into actual AVPs
            try (StringReader reader = new StringReader(jsonOutput)) {
                res.addAll(flairServiceConnector.getFlairUnarmifiedCandidates(
                        docs.stream().map(NamedParsedDocument::getDocName).collect(Collectors.toList()),
                        reader,
                        sentenceCounts
                ));
            }
            // armify the result
            res = res.stream().map(this::armify).collect(Collectors.toList());
        }
        return res;
    }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(NamedParsedDocument doc) {
        return extract(Lists.newArrayList(doc)).get(0);
    }

    protected List<String> getFlairJsonOutput(List<String> sentences) {
        return flairServiceConnector.requestFlairResultsForSentences(sentences);
    }

    private String stripTopLevelJsonElement(String jsonOutput) {
        jsonOutput = jsonOutput.substring(0, jsonOutput.length() - 1);
        jsonOutput = jsonOutput.replace("{\"result\":", "");
        return jsonOutput;
    }

    protected List<CandidateInPassage<ArmifiedAttributeValuePair>> armify(List<CandidateInPassage<ArmifiedAttributeValuePair>> avps) {
        List<String> armNames = avps.stream()
                .map(CandidateInPassage::getAnswer)
                .filter(avp -> avp.getAttribute().equals(Attributes.get().getFromName("Arm name")))
                .map(avp -> avp.getValue())
                .distinct()
                .collect(Collectors.toList());
        if (armNames.size() < 2) {
            // don't armify
            return new ArrayList<>(avps);
        } else {
            List<CandidateInPassage<ArmifiedAttributeValuePair>> res = new ArrayList<>();
            AtomicInteger armId = new AtomicInteger(10);
            // cluster the arm names, assuming 2 arms
            Set<Set<String>> twoArms = clusterer.getArmNameClusters(armNames);
            /*
            Set<Set<String>> twoArms = armNames.stream()
                    .map(armName -> Sets.newHashSet(armName))
                    .collect(Collectors.toSet());
            //*/
            // create the arms
            List<Arm> arms = twoArms.stream()
                    .filter(possibleNames -> !possibleNames.isEmpty())
                    .map(possibleNames -> new Arm(String.valueOf(armId.getAndIncrement()), possibleNames.iterator().next(), new ArrayList<>(new HashSet<>(possibleNames))))
                    .collect(Collectors.toList());
            // armify with the default associator
            Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armifiedAvps = armAssociator.associate(avps, arms);
            // for debugging purposes
            List<Arm> usedArms = armifiedAvps.stream()
                    .map(cavp -> cavp.getAnswer().getArm())
                    .distinct()
                    .collect(Collectors.toList());
            return new ArrayList<>(armifiedAvps);
        }
    }

    @Override
    public List<? extends Evaluator<NamedParsedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        PredicateUnarmifiedEvaluator<NamedParsedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<NamedParsedDocument, ArmifiedAttributeValuePair>() {
                    @Override
                    public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                        return expected.getContext().contains(predicted.getValue());
                    }
                };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new MultiValueUnarmifiedEvaluator<>(predicateUnarmifiedEvaluator),
                new PredicateArmifiedEvaluator<NamedParsedDocument>(predicateUnarmifiedEvaluator)
        );
    }

    public static void main(String[] args) throws IOException {
        InformationExtractorFlair flairExtractor = new InformationExtractorFlair();
        PdfToDocumentFunction pdfParser = new ReparsePdfToDocument(Props.loadProperties());
        String docName = "Zhu 2012.pdf";
        Document doc = pdfParser.getDocument(new File(docName));
        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results = flairExtractor.extract(new NamedParsedDocument(docName, doc));
        System.out.println(results);
        System.out.println(results.size());
    }
}
