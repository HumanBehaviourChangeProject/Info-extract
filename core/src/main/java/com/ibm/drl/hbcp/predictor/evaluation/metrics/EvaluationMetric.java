package com.ibm.drl.hbcp.predictor.evaluation.metrics;

import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;

import java.util.List;

/** An evaluation metric for a regression algorithm. Compares a list of predicted and reference values.
 *
 * @author mgleize
 */
public interface EvaluationMetric {

    double compute(List<PredictionTuple> predictionsAndReferences);
}
