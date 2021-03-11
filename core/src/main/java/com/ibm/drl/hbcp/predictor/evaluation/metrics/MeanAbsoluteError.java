package com.ibm.drl.hbcp.predictor.evaluation.metrics;

public class MeanAbsoluteError implements MeanMetric {

    @Override
    public double compute(double predicted, double expected) {
        return Math.abs(predicted - expected);
    }

    @Override
    public String toString() {
        return "MAE";
    }
}
