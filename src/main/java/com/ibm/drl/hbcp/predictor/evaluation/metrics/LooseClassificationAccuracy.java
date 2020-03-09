package com.ibm.drl.hbcp.predictor.evaluation.metrics;

/**
 * Evaluates a regression algorithm as for a classification problem.
 * Returns 1.0 if the predicted value is "close enough" to the expected value, 0.0 otherwise.
 *
 * @author mgleize
 */
public class LooseClassificationAccuracy implements MeanMetric {

    // We allow the prediction to deviate at most 10% of the expected value
    private static final double DEFAULT_EPSILON_RATIO = 0.1; // 2x the expected

    @Override
    public double compute(double predicted, double expected) {
        if (difference(predicted, expected) <= allowedDifference(predicted, expected)) {
            return 1.0;
        } else {
            return 0.0;
        }
    }

    private double difference(double predicted, double expected) {
        return Math.abs(predicted - expected);
    }

    private double allowedDifference(double predicted, double expected) {
        if (expected > 0) {
            return DEFAULT_EPSILON_RATIO * expected;
        } else {
            // if we predict up to 0.1%, this is considered a valid zero prediction
            return DEFAULT_EPSILON_RATIO;
        }
    }

    @Override
    public String toString() {
        return "Loose Classification Accuracy";
    }
}
