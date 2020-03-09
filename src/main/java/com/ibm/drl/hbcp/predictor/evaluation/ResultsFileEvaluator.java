/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.EvaluationMetric;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.LooseClassificationAccuracy;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.MeanAbsoluteError;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.RMSE;
import java.io.File;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import org.apache.commons.io.FileUtils;
import org.apache.commons.math3.stat.correlation.KendallsCorrelation;
import org.apache.commons.math3.stat.correlation.SpearmansCorrelation;

/**
 *
 * @author dganguly
 */
public class ResultsFileEvaluator {
    List<PredictionTuple> rtuples;
    
    private static final List<EvaluationMetric> evaluationMetrics = Lists.newArrayList(
            new RMSE(),
            new MeanAbsoluteError(),
            new LooseClassificationAccuracy()
    );

    public ResultsFileEvaluator(String tsvFile) throws IOException {
        
        List<String> lines = FileUtils.readLines(new File(tsvFile), Charset.defaultCharset());
        rtuples = new ArrayList<>(lines.size());
        
        for (String line: lines) {
            String[] tokens = line.split("\\s+");
            double ref = Double.parseDouble(tokens[0]);
            double pred = Double.parseDouble(tokens[1]);
            rtuples.add(new PredictionTuple(ref, pred));
        }
    }
    
    void evaluate() {
        for (EvaluationMetric metric : evaluationMetrics) {
            String metricName = metric.toString();
            double evaluationResult = metric.compute(rtuples);
            System.out.println(metricName + ": " + evaluationResult);
        }
        
        KendallsCorrelation kcorr = new KendallsCorrelation();
        SpearmansCorrelation scorr = new SpearmansCorrelation();
        
        double[] xArray = new double[rtuples.size()];
        double[] yArray = new double[rtuples.size()];
        int i = 0;
        
        for (PredictionTuple t: rtuples) {
            xArray[i] = t.getRef();
            yArray[i] = t.getPred();
            i++;
        }
        
        System.out.println(String.format("Kendal's tau: %.4f", kcorr.correlation(xArray, yArray)));
        System.out.println(String.format("Spearman's rho: %.4f", scorr.correlation(xArray, yArray)));
    }
    
    /* Takes a file of tab separated ref and predicted values, and reports the metrics */
    public static void main(String[] args) {
        if (args.length < 1) {
            System.err.println("usage: java ResultsFileEvaluator <tsv res file>");
            args = new String[1];
            args[0] = "prediction/python-nb/predictions.txt";
        }
        
        try {
            ResultsFileEvaluator re = new ResultsFileEvaluator(args[0]);
            re.evaluate();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
}
