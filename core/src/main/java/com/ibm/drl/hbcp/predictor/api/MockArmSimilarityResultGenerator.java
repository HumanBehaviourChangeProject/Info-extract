package com.ibm.drl.hbcp.predictor.api;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.api.RelevantPredictionQueryAttributes;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;

public class MockArmSimilarityResultGenerator {

    // fixed seed, not too important
    private final Random random = new Random(0);
    private final List<String> docNames;
    private final AttributeValueCollection<AnnotatedAttributeValuePair> annotations;

    public MockArmSimilarityResultGenerator() throws IOException {
        annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        docNames = new ArrayList<>(annotations.getDocNames());
    }

    public List<ArmSimilarityResult> generateTopK(int k) {
        // select k random different docs
        List<String> kDocNames = getKRandomElements(k, docNames);
        // generate k scores
        List<ArmSimilarityResult> res = new ArrayList<>();
        for (int i = 0; i < k; i++) {
            double score = (double)i / k;
            String docName = kDocNames.get(i);
            res.add(generate(docName, score));
        }
        // sort by descending score
        res.sort(Comparator.comparing(ArmSimilarityResult::getScore).reversed());
        return res;
    }

    public ArmSimilarityResult generate() {
        return generate(docNames.get(random.nextInt(docNames.size())), random.nextDouble());
    }

    private ArmSimilarityResult generate(String docName, double score) {

        // select the first arm for that doc
        Arm arm;
        List<Attribute> bcts;
        double outcomeValue;
        try {
            arm = annotations.getArmifiedPairsInDoc(docName).keySet().iterator().next();
            bcts = annotations.getArmifiedPairsInDoc(docName).get(arm).stream()
                    .filter(aavp -> aavp.getAttribute().getType() == AttributeType.INTERVENTION)
                    .map(aavp -> aavp.getAttribute())
                    .collect(Collectors.toList());
            outcomeValue = getOutcomeValue(annotations.getOutcomeValue(docName, arm));
        } catch (NoSuchElementException e) {
            arm = Arm.MAIN;
            bcts = new ArrayList<>();
            outcomeValue = -1.0;
        }
        // generate the booleans using the score
        boolean isSimilarPopulation = random.nextDouble() < score;
        boolean isSimilarIntervention = random.nextDouble() < score;
        // generate the attribute similarities in a similar way
        SortedMap<Attribute, Double> attributeSimilarities = new TreeMap<>();
        for (Attribute attribute : RelevantPredictionQueryAttributes.getForType(AttributeType.POPULATION)) {
            double sim = (0.5 + random.nextDouble() / 2) * score;
            attributeSimilarities.put(attribute, sim);
        }
        // split the BCTs into common / onlyInArm
        int common = random.nextInt(bcts.size() + 1);
        Set<Attribute> commonInterventions = new HashSet<>(bcts.subList(0, common));
        Set<Attribute> onlyInArm = new HashSet<>(bcts.subList(common, bcts.size()));
        Set<Attribute> onlyInQuery = new HashSet<>();
        return new ArmSimilarityResult(arm, docName, docName, score, outcomeValue, "6 months", attributeSimilarities,
                commonInterventions, onlyInQuery, onlyInArm,
                isSimilarPopulation, isSimilarIntervention);
    }

    private <E> List<E> getKRandomElements(int k, List<E> elements) {
        List<E> shuffled = new ArrayList<>(elements);
        Collections.shuffle(shuffled);
        return shuffled.subList(0, Math.min(shuffled.size(), k));
    }

    private double getOutcomeValue(Optional<Double> maybeOutcomeValue) {
        return maybeOutcomeValue.orElse(-1.0);
    }
}
