package com.ibm.drl.hbcp.experiments.prediction;

import com.beust.jcommander.internal.Lists;
import com.ibm.drl.hbcp.api.PredictorController;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.predictor.api.RankedResults;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import lombok.Data;
import org.apache.commons.lang3.Range;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.stream.DoubleStream;

public class PredictionParameterSearcher {

    private final Attribute attribute;
    private final PredictorController predictor;

    public static final double OUTCOME_EQUALITY_PRECISION = 0.01;

    public PredictionParameterSearcher(Attribute attribute, PredictorController predictor) {
        this.attribute = attribute;
        this.predictor = predictor;
    }

    public <E extends Comparable<E>> List<Pair<Range<E>, Double>> search(APICall baseQuery, Iterator<E> valuesOfExtraParameter) {
        List<Pair<Range<E>, Double>> res = new ArrayList<>();
        E start = null;
        E end = null;
        double previousOutcome = Double.NEGATIVE_INFINITY;
        while (valuesOfExtraParameter.hasNext()) {
            E value = valuesOfExtraParameter.next();
            // compute the new outcome
            APICall query = baseQuery.with(new AttributeValuePair(attribute, value.toString()));
            RankedResults<SearchResult> results = predictor.predictOutcome(query.getPopulationAttributes(),
                    query.getInterventionAttributes(),
                    query.getExperimentalSettingAttributes(),
                    10, query.isUseAnnotations(), query.isUseEffectSize(), false);
            if (results.getResults().isEmpty()) {
                // the query will likely return an empty result, we exit with an exception
                throw new RuntimeException("No result for query: " + query);
            }
            double outcome = Double.parseDouble(results.getResults().get(0).getNode().getValue());
            //System.out.println("Predicted outcome: " + outcome);
            if (!areOutcomeEqual(previousOutcome, outcome)) {
                // we store the previous range
                if (Double.isFinite(previousOutcome)) {
                    res.add(Pair.of(Range.between(start, end), outcome));
                }
                start = value;
                end = start;
                previousOutcome = outcome;
            } else {
                // we update the end
                end = value;
            }
        }
        // add the last range
        res.add(Pair.of(Range.between(start, end), previousOutcome));
        return res;
    }

    private boolean areOutcomeEqual(double outcome1, double outcome2) {
        return Math.abs(outcome1 - outcome2) < OUTCOME_EQUALITY_PRECISION;
    }

    @Data
    public static class APICall {
        private final List<String> populationAttributes;
        private final List<String> interventionAttributes;
        private final List<String> experimentalSettingAttributes;
        private final boolean useAnnotations;
        private final boolean useEffectSize;

        public APICall with(AttributeValuePair extraParam) {
            AttributeValueNode newQueryNode = new AttributeValueNode(extraParam);
            List<String> newPopA = populationAttributes;
            List<String> newInterA = interventionAttributes;
            List<String> newExpA = experimentalSettingAttributes;
            switch (extraParam.getAttribute().getType().getShortString()) {
                case "S":
                case "O":
                    newExpA = new ArrayList<>(newExpA);
                    newExpA.add(newQueryNode.toString());
                    break;
                case "C":
                    newPopA = new ArrayList<>(newPopA);
                    newPopA.add(newQueryNode.toString());
                    break;
                case "I":
                    newInterA = new ArrayList<>(newInterA);
                    newInterA.add(newQueryNode.toString());
                    break;
            }
            return new APICall(newPopA, newInterA, newExpA, useAnnotations, useEffectSize);
        }
    }

    public static void main(String[] args) throws Exception {
        PredictorController predictor = new PredictorController();
        Attribute longestFollowup = Attributes.get().getFromName("Longest follow up");
        PredictionParameterSearcher searcher = new PredictionParameterSearcher(longestFollowup, predictor);
        // enter your query here
        APICall baseQuery = new APICall(
                // population attributes
                Lists.newArrayList(
                        AttributeValueNode.create("Minimum age", 18),
                        AttributeValueNode.create("All female", 1)
                ),
                // BCTs
                Lists.newArrayList(
                        AttributeValueNode.create("1.1.Goal setting (behavior)", 1)
                ),
                // experimental settings (where longest follow up will be added automatically, also, so leave empty)
                Lists.newArrayList(),
                true,
                true);
        // define a range of longest followups: this is iterating from 1 to 300 by increments of 1
        DoubleStream followups = DoubleStream.iterate(1.0, n -> n + 1).limit(300);
        // perform the search
        List<Pair<Range<Double>, Double>> res = searcher.search(baseQuery, followups.iterator());
        System.out.println("The query yields the following outcomes for various values of an extra '" + longestFollowup.getName() + "' parameter:");
        for (Pair<Range<Double>, Double> rangeAndValue : res) {
            System.out.println(rangeAndValue.getKey() + " : " + rangeAndValue.getValue());
        }
    }
}
