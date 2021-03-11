package com.ibm.drl.hbcp.experiments.ie.evaluation;

import java.io.File;
import java.util.List;

public class IAAKrippAlphaEvaluation extends InterAnnotatorAgreementEvaluation {

    public IAAKrippAlphaEvaluation(Metric f1Metric) {
        super(f1Metric);
    }

    @Override
    protected List<Double> getAggregateMetric(List<List<Double>> metricVectors) {
        return getAverages(metricVectors);
    }

    public static void main(String[] args) throws Exception {
        String[] evalNames = { "KripAlpha" };
        Metric[] metrics = { new AvcKrippendorffAlphaAgreement() };
        for (int i = 0; i < evalNames.length; i++) {
            IAAKrippAlphaEvaluation eval = new IAAKrippAlphaEvaluation(metrics[i]);
            // add unsupervised baseline
            eval.addResults("Baseline", InterAnnotatorAgreementEvaluation.loadExtractedResults());
            // add Flair extractions
            eval.addResults("Flair", InterAnnotatorAgreementEvaluation.loadFlairResults(new File("../data/precomputed/flairExtraction")));
            // print results
            eval.writeAverageMetricPerAnnotatorPerAttribute(
                    new File("output/" + evalNames[i] + "_all_eval.csv"),
                    new String[] { "Krippendorff's alpha" },
                    new int[] { 0 }
            );
            eval.writeAverageSingleMetricPerAttribute(
                    new File("output/" + evalNames[i] + "_perattribute_kalpha.csv"),
                    0
            );
        }
    }

}
