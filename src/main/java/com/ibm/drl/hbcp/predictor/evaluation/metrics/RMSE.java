package com.ibm.drl.hbcp.predictor.evaluation.metrics;

import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;

import java.util.List;

/* Computes the RMSE given pairs of (ref, pred) values.
*
* @author dganguly, mgleize
* */
public class RMSE implements MeanMetric {

    @Override
    public double compute(double predicted, double expected) {
        double diff = predicted - expected;
        return diff * diff;
    }

    @Override
    public double aggregate(List<Double> individualComparisons) {
        return Math.sqrt(individualComparisons.stream().reduce(0.0, Double::sum) / individualComparisons.size());
    }

    @Override
    public String toString() {
        return "RMSE";
    }
}
