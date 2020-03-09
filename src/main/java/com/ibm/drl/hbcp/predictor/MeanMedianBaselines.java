package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.util.*;

/**
 * Baseline that takes the mean or median outcome vales from the training data and uses as prediction.
 *
 *
 */
public class MeanMedianBaselines {

    public static void main(String[] args) throws Exception {
        PredictionWorkflowManager wfm = new PredictionWorkflowManager(0.8);

        // collect output values
        List<Double> outputValues = new ArrayList<>();
        for (DataInstance instance : wfm.getTrainInstances()) {
            outputValues.add(instance.getYNumeric());
        }
        final double[] ovArray = outputValues.stream().mapToDouble(d -> d).toArray();
        DescriptiveStatistics stats = new DescriptiveStatistics(ovArray);
        final double ovMean = stats.getMean();
        final double ovStDev = stats.getStandardDeviation();
        final double median = stats.getPercentile(50);
        System.out.println("mean = " + ovMean);
        System.out.println("St.Dev. = " + ovStDev);
        System.out.println("median = " + median);

        System.out.println("Mean results");
        List<PredictionTuple> predictions = predict(wfm.getTestInstances(), ovMean);
        wfm.printAndSaveResults(predictions);
        System.out.println("Median results");
        predictions = predict(wfm.getTestInstances(), median);
        wfm.printAndSaveResults(predictions);
    }

    private static List<PredictionTuple> predict(List<DataInstance> testSet, double prediction) {
        final List<PredictionTuple> predTuples = new ArrayList<>();
        for (DataInstance testInstance : testSet) {
            PredictionTuple tuple = new PredictionTuple(testInstance.getYNumeric(), (float) prediction);
            predTuples.add(tuple);
        }
        return predTuples;
    }

}
