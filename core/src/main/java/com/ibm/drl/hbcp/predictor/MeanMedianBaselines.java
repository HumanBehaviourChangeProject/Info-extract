package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import java.io.IOException;
import java.util.*;

/**
 * Baseline that takes the mean or median outcome vales from the training data and uses as prediction.
 *
 *
 */
public class MeanMedianBaselines extends PredictionWorkflow {

    protected MeanMedianBaselines(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                  AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                  TrainTestSplitter splitter, Properties props) throws IOException {
        super(values, annotations, splitter, props);
    }

    public void run() {
        // collect output values
        List<Double> outputValues = new ArrayList<>();
        for (DataInstance instance : getTrainInstances()) {
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
        List<PredictionTuple> predictions = predict(getTestInstances(), ovMean);
        printAndSaveResults(predictions);
        System.out.println("Median results");
        predictions = predict(getTestInstances(), median);
        printAndSaveResults(predictions);
    }

    private List<PredictionTuple> predict(List<DataInstance> testSet, double prediction) {
        final List<PredictionTuple> predTuples = new ArrayList<>();
        for (DataInstance testInstance : testSet) {
            PredictionTuple tuple = new PredictionTuple(testInstance.getYNumeric(), (float) prediction);
            predTuples.add(tuple);
        }
        return predTuples;
    }

    public static void main(String[] args) throws Exception {
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        MeanMedianBaselines baselines = new MeanMedianBaselines(
                annotations,
                annotations,
                new TrainTestSplitter(0.8),
                Props.loadProperties()
        );
        baselines.run();
    }

}
