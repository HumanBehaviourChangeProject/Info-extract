package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.experiments.flair.ExtractPrediction;

import java.io.File;
import java.util.List;

public class IAAF1Evaluation extends InterAnnotatorAgreementEvaluation {

    public IAAF1Evaluation(F1Base f1Metric) {
        super(f1Metric);
    }

    @Override
    protected List<Double> getAggregateMetric(List<List<Double>> metricVectors) {
        return ((F1Base)metric).aggregate(metricVectors);
    }

    public static void main(String[] args) throws Exception {
        String[] evalNames = { "F1_BCTs", "F1_values", "F1_complex", "F1_all" };
        F1Base[] metrics = { new F1BctAttributes(), new F1TextAttributes(), new F1ComplexAttributes(), new F1AllAttributes() };
        for (int i = 0; i < evalNames.length; i++) {
            IAAF1Evaluation eval = new IAAF1Evaluation(metrics[i]);
            // add unsupervised baseline
            eval.addResults("Baseline", InterAnnotatorAgreementEvaluation.loadExtractedResults());
            // add Flair extractions
            eval.addResults("Flair", ExtractPrediction.armify(InterAnnotatorAgreementEvaluation.loadFlairResults(new File("../data/precomputed/flairExtraction"))));
            // print results
            eval.writeAverageMetricPerAnnotatorPerAttribute(
                    new File("output/" + evalNames[i] + "_all_eval.csv"),
                    new String[] { "precision", "recall", "f1" },
                    new int[] { 0, 1, 2 }
            );
            eval.writeAverageSingleMetricPerAttribute(
                    new File("output/" + evalNames[i] + "_perattribute_f1.csv"),
                    2
            );
        }
    }
}
