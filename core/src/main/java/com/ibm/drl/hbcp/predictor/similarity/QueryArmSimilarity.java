package com.ibm.drl.hbcp.predictor.similarity;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AVPRetriever;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.predictor.PredictionWorkflow;
import com.ibm.drl.hbcp.predictor.api.ArmSimilarityResult;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.baselines.TranslatingRanker;
import com.ibm.drl.hbcp.util.Props;

public class QueryArmSimilarity {

    private final NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> alc;
    private final AVPRetriever avpRetriever;

    public QueryArmSimilarity(AttributeValueCollection<? extends ArmifiedAttributeValuePair> allAvps, Properties props) throws IOException {
        alc = PredictionWorkflow.prepareAttributeValuePairs(allAvps, props);
        // FIXME not sure if this constructor will be needed
        avpRetriever = new AVPRetriever(
                alc,
                alc,
                new TrainTestSplitter(0.8),
                Props.loadProperties()
        );
    }

    public List<ArmSimilarityResult> querySimilarityByArm(List<AttributeValuePair> queryAvps, @Nullable JSONRefParser parser) throws IOException {
        // create a fakely armified version of the query
        List<ArmifiedAttributeValuePair> armifiedQueryAvpsList = queryAvps.stream()
                .map(avp -> new ArmifiedAttributeValuePair(avp.getAttribute(), avp.getValue(), "", Arm.EMPTY))
                .collect(Collectors.toList());
        Multiset<ArmifiedAttributeValuePair> armifiedQueryAvps = ConcurrentHashMultiset.create(armifiedQueryAvpsList);
        // get list of arm similarities
        List<ArmSimilarityResult> armSimilarityResults = new ArrayList<>();

        final AttributeValueCollection<ArmifiedAttributeValuePair> queryAvc = new AttributeValueCollection<>(armifiedQueryAvps);

        for (String docName : alc.getDocNames()) {
            final Map<Arm, Multiset<NormalizedAttributeValuePair>> armifiedPairsInDoc = alc.getArmifiedPairsInDoc(docName);
            for (Map.Entry<Arm, Multiset<NormalizedAttributeValuePair>> armMultisetEntry : armifiedPairsInDoc.entrySet()) {
                // discard the candidate right away if it doesn't have an outcome value
                // mgleize: I don't like to bury this issue because it can hide other code issues but some annotations really don't contain OVs
                Optional<Double> outcomeOptional = alc.getOutcomeValue(docName, armMultisetEntry.getKey());
                if (!outcomeOptional.isPresent()) {
                    continue;
                }

                SortedMap<Attribute, Double> attributeSimilarities = avpRetriever.computeAttributeSimilarities(queryAvc,
                        new AttributeValueCollection<>(armMultisetEntry.getValue()));

                Set<Attribute> commonInterventions = getBctIntersection(armifiedQueryAvps, armMultisetEntry.getValue());
                Set<Attribute> onlyInQuery = getBctDifference(armifiedQueryAvps, armMultisetEntry.getValue());
                Set<Attribute> onlyInArm = getBctDifference(armMultisetEntry.getValue(), armifiedQueryAvps);

                boolean isSimilarPopulation = avpRetriever.isSimilarAttributeGroup(attributeSimilarities, queryAvc, AttributeType.POPULATION);
                boolean isSimilarIntervention = avpRetriever.isSimilarAttributeGroup(attributeSimilarities, queryAvc, AttributeType.INTERVENTION);
                double score = avpRetriever.aggregateScore(attributeSimilarities, queryAvc, a -> true);

                Optional<JSONRefParser.PdfInfo> titleOptional = parser != null ? parser.getDocInfo(docName) : Optional.empty();
                String title = titleOptional.isPresent() ? titleOptional.get().getTitleFirstAuthorAndDate() : "Missing title.";
                double outcomeValue =  outcomeOptional.get();  // TODO default outcome?
                String followUp = getFollowUp(armMultisetEntry.getValue());

                ArmSimilarityResult result = new ArmSimilarityResult(armMultisetEntry.getKey(), docName,
                        title, score, outcomeValue, followUp,
                        filter(attributeSimilarities, e -> e.getKey().getType() == AttributeType.POPULATION),
                        commonInterventions, onlyInQuery, onlyInArm,
                        isSimilarPopulation, isSimilarIntervention);
                armSimilarityResults.add(result);
            }
        }
        // order arms by score
        final List<ArmSimilarityResult> sortedResults = armSimilarityResults.stream()
                .sorted(Comparator.comparingDouble(ArmSimilarityResult::getScore).reversed())
                .collect(Collectors.toList());

        // remove duplicate documents keeping most similar arm as representative of the document
        Set<String> seenDocuments = new HashSet<>();
        List<ArmSimilarityResult> finalResults = new ArrayList<>();
        for (ArmSimilarityResult armSimilarityResult : sortedResults) {
            if (!seenDocuments.contains(armSimilarityResult.getDocName())) {
                finalResults.add(armSimilarityResult);
                seenDocuments.add(armSimilarityResult.getDocName());
            }
        }
        return finalResults;
    }

    private Set<Attribute> getBctIntersection(Multiset<? extends ArmifiedAttributeValuePair> avps1, Multiset<? extends ArmifiedAttributeValuePair> avps2) {
        final Set<Attribute> attributes1 = getBctAttributes(avps1);
        final Set<Attribute> attributes2 = getBctAttributes(avps2);
        attributes1.retainAll(attributes2);
        return attributes1;
    }

    private Set<Attribute> getBctDifference(Multiset<? extends ArmifiedAttributeValuePair> avps1, Multiset<? extends ArmifiedAttributeValuePair> avps2) {
        final Set<Attribute> attributes1 = getBctAttributes(avps1);
        final Set<Attribute> attributes2 = getBctAttributes(avps2);
        attributes1.removeAll(attributes2);
        return attributes1;
    }

    private Set<Attribute> getBctAttributes(Multiset<? extends ArmifiedAttributeValuePair> avps1) {
        return avps1.stream().map(ArmifiedAttributeValuePair::getAttribute)
                .filter(attribute -> attribute.getType().equals(AttributeType.INTERVENTION))
                .collect(Collectors.toSet());
    }

    private SortedMap<Attribute, Double> filter(SortedMap<Attribute, Double> attributeSims, Predicate<Map.Entry<Attribute, Double>> predicate) {
        SortedMap<Attribute, Double> res = new TreeMap<>();
        for (Map.Entry<Attribute, Double> entry : attributeSims.entrySet()) {
            if (predicate.test(entry))
                res.put(entry.getKey(), entry.getValue());
        }
        return res;
    }

    private String getFollowUp(Collection<NormalizedAttributeValuePair> avps) {
        Optional<NormalizedAttributeValuePair> followUp = avps.stream()
                .filter(avp -> avp.getAttribute().isTimepoint())
                .findFirst();
        return followUp.map(avp -> TranslatingRanker.reformatFollowUp(avp.getNormalizedValue())).orElse("N/A");
    }

    public static void main(String[] args) throws IOException {
        // build a query from a document or just make up a simple one
        final Properties props = Props.loadProperties();
        JSONRefParser parser = new JSONRefParser(props);
        parser.buildAll();
        AttributeValueCollection<AnnotatedAttributeValuePair> allAvps = parser.getAttributeValuePairs();

        final QueryArmSimilarity queryArmSimilarity = new QueryArmSimilarity(allAvps, props);

        // first distribute the empty arm (give to each real arm its values)
        allAvps = allAvps.distributeEmptyArm();
        // apply the cleaners
        if (!allAvps.isEmpty() && allAvps.stream().findFirst().get() instanceof AnnotatedAttributeValuePair) {
            Cleaners cleaners = new Cleaners(props);
            // this warning is checked just before, should be okay
            allAvps = cleaners.clean(allAvps);
        }
        final Map<Arm, Multiset<AnnotatedAttributeValuePair>> armifiedPairsInDoc = allAvps.getArmifiedPairsInDoc("Alessi 2014.pdf");
        final Optional<Multiset<AnnotatedAttributeValuePair>> first = armifiedPairsInDoc.values().stream().findFirst();
        if (first.isPresent()) {
            final Multiset<AnnotatedAttributeValuePair> aavps = first.get();
            List<ArmSimilarityResult> results = queryArmSimilarity.querySimilarityByArm(new ArrayList<>(aavps), parser);
            System.out.println("Top docs:");
            for (ArmSimilarityResult result : results) {
                System.out.println(result.getScore() + " : " + result.getDocName());
            }
        }
    }

}
