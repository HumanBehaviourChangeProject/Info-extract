package com.ibm.drl.hbcp.predictor.evaluation.metrics;

import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;

import java.util.List;
import java.util.stream.Collectors;

/**
 * An evaluation metric computed as a mean of measures on individual pairs of predicted and expected values.
 *
 * @author mgleize
 */
public interface MeanMetric extends EvaluationMetric {

    /** Compares an individual pair of a predicted and a expected value */
    double compute(double predicted, double expected);

    @Override
    default double compute(List<PredictionTuple> predictionsAndReferences) {
        List<PredictionTuple> validPairs = predictionsAndReferences.stream()
                .filter(PredictionTuple::valid)
                .collect(Collectors.toList());
        if (validPairs.isEmpty()) return 0.0;
        return aggregate(validPairs.stream()
                .map(pair -> compute(pair.getPred(), pair.getRef()))
                .collect(Collectors.toList()));
    }

    /** Aggregates a list of individual measures
     *
     * @param individualComparisons assumes that this list is not empty
     * @return an aggregate
     */
    default double aggregate(List<Double> individualComparisons) {
        // The default implementation provided is the mean/average
        return individualComparisons.stream().reduce(0.0, Double::sum) / individualComparisons.size();
    }
}
