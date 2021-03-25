package com.ibm.drl.hbcp.predictor.api;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.ImmutableMultiset;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Draw from the smoking cessation annotations to provide the most frequent intervention (as a set of BCTs) reported on
 * @author mgleize
 */
public class RecommendedInterventions {

    private RecommendedInterventions() throws IOException {
        mostFrequentInterventions = getMostFrequentInterventionsInAnnotations();
    }

    // sets of BCT IDs
    private final List<Set<String>> mostFrequentInterventions;

    /** Returns at most 'max' of the interventions sorted by descending frequency */
    public List<Set<String>> getTopKMostFrequentInterventions(int max) {
        return mostFrequentInterventions.stream()
                .limit(max)
                .collect(Collectors.toList());
    }

    /** Transform a BC scenario with a set of all possible BCTs allowed, into a set of recommended scenarios with reasonable
     * combinations of BCTs taken from that set. */
    public List<Set<String>> getRecommendedInterventions(List<AttributeValuePair> possibleScenarios, int max) {
        // extract the BCTs from the possibleScenarios, they are what is "allowed" by the user
        Set<String> allowedBctIds = possibleScenarios.stream()
                .filter(avp -> avp.getAttribute().getType() == AttributeType.INTERVENTION)
                .map(avp -> avp.getAttribute().getId())
                .collect(Collectors.toSet());
        // filter pre-computed recommended interventions based on this
        return mostFrequentInterventions.stream()
                .filter(allowedBctIds::containsAll)
                .limit(max)
                // no need to sort, they're already sorted
                .collect(Collectors.toList());
    }

    /** Transform a BC scenario with a set of all possible BCTs allowed, into a set of recommended scenarios with reasonable
     * combinations of BCTs taken from that set. */
    public List<List<AttributeValuePair>> getRecommendedScenarios(List<AttributeValuePair> possibleScenarios, int max) {
        // get recommended compatible scenarios
        List<Set<String>> recommendedInterventions = getRecommendedInterventions(possibleScenarios, max);
        // for each of the recommended intervention, build a new complete scenario
        List<AttributeValuePair> nonInterventionAvps = possibleScenarios.stream()
                .filter(avp -> avp.getAttribute().getType() != AttributeType.INTERVENTION)
                .collect(Collectors.toList());
        List<List<AttributeValuePair>> res = new ArrayList<>();
        for (Set<String> recommendedIntervention : recommendedInterventions) {
            List<AttributeValuePair> recommendedScenario = new ArrayList<>(nonInterventionAvps);
            recommendedScenario.addAll(recommendedIntervention.stream()
                    .map(bctId -> new AttributeValuePair(Attributes.get().getFromId(bctId), "1"))
                    .collect(Collectors.toList()));
            res.add(recommendedScenario);
        }
        return res;
    }

    private static List<Set<String>> getMostFrequentInterventionsInAnnotations() throws IOException {
        // read annotations
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties())
                .getAttributeValuePairs()
                // do not forget to distribute the empty arms here
                .distributeEmptyArm();
        // get all interventions (through their ID sets) and their frequency
        Multiset<Set<String>> res = ConcurrentHashMultiset.create();
        for (String docName : annotations.getDocNames()) {
            for (Multiset<AnnotatedAttributeValuePair> armifiedAvps : annotations.getArmifiedPairsInDoc(docName).values()) {
                Set<String> bctIds = armifiedAvps.stream()
                        .filter(avp -> avp.getAttribute().getType() == AttributeType.INTERVENTION)
                        .map(avp -> avp.getAttribute().getId())
                        .collect(Collectors.toSet());
                if (!bctIds.isEmpty())
                    res.add(bctIds);
            }
        }
        // sort by frequency
        ImmutableMultiset<Set<String>> sortedRes = Multisets.copyHighestCountFirst(res);
        return new ArrayList<>(sortedRes.elementSet());
    }

    // implements the lazy-initialization thread-safe singleton pattern
    private static class LazyHolder {
        private static RecommendedInterventions buildInterventions() {
            try {
                return new RecommendedInterventions();
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("IOException when lazy-initializing singleton RecommendedInterventions", e);
            }
        }
        private static final RecommendedInterventions INSTANCE = buildInterventions();
    }

    /** Returns the Attributes collection for the JSON annotation file defined in the default properties */
    public static RecommendedInterventions get() {
        return RecommendedInterventions.LazyHolder.INSTANCE;
    }

    public static void main(String[] args) {
        for (Set<String> intervention : RecommendedInterventions.get().getTopKMostFrequentInterventions(20)) {
            System.out.println(intervention);
        }
    }
}
