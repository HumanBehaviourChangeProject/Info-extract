package com.ibm.drl.hbcp.experiments.prediction.noncontrolprediction;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.EvaluationMetric;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.LooseClassificationAccuracy;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.MeanAbsoluteError;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.RMSE;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class SimpleEvaluator {

    public static final List<EvaluationMetric> METRICS = Lists.newArrayList(
            new RMSE(),
            new MeanAbsoluteError(),
            new LooseClassificationAccuracy());

    public static Map<EvaluationMetric, Double> evaluate(List<PredictionTuple> predictions) {
        Map<EvaluationMetric, Double> res = new HashMap<>();
        for (EvaluationMetric metric : METRICS) {
//            String metricName = metric.toString();
            double evaluationResult = metric.compute(predictions);
            res.put(metric, evaluationResult);
        }
        return res;
    }

}
