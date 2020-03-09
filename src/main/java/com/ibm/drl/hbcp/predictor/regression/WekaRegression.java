package com.ibm.drl.hbcp.predictor.regression;

import com.ibm.drl.hbcp.predictor.DataInstance;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.util.ParsingUtils;
import weka.classifiers.Classifier;
import weka.classifiers.functions.SMOreg;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.Filter;
import weka.filters.unsupervised.attribute.Normalize;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Trains and outputs predictions on a train-test split with a Weka classifier.
 * The default classifier is an SVM but this can be overridden in subclasses.
 *
 * @author mgleize
 */
public class WekaRegression {

    private final WekaDataset dataset;

    public WekaRegression(WekaDataset dataset) {
        this.dataset = dataset;
    }

    public WekaRegression(List<DataInstance> trainInstances, List<DataInstance> testInstances) throws IOException {
        this(new WekaDataset(trainInstances, testInstances));
    }

    /*
        C is the reguralization parameter --- higher the value, looser the fit
        gamma is the width of the kernel parameter
    */
    public Classifier train(float C, float gamma) throws Exception {
        Instances train = WekaDataset.buildWekaInstances(dataset.getHeaders(), dataset.getTrainData());
        applyFilters(train);
        Classifier classifier = newClassifier(C, gamma);
        classifier.buildClassifier(train);
        return classifier;
    }
    
    public Classifier train() throws Exception {
        return train(1, 0.1f);  // default values
    }

    public List<PredictionTuple> predict(Classifier classifier) throws Exception {
        Instances test = WekaDataset.buildWekaInstances(dataset.getHeaders(), dataset.getTestData());
        applyFilters(test);
        List<PredictionTuple> res = new ArrayList<>();
        // parallel iteration on the Instances and the List<List<String>>
        int i = 0;
        for (Instance instance : test) {
            List<String> correspondingRow = dataset.getTestData().get(i++);
            double reference = ParsingUtils.parseFirstDouble(correspondingRow.get(correspondingRow.size() - 1));
            double prediction = classifier.classifyInstance(instance);
            PredictionTuple tuple = new PredictionTuple(reference, prediction);
            res.add(tuple);
        }
        return res;
    }

    protected Classifier newClassifier(float C, float gamma) throws Exception {
        SMOreg model = new SMOreg();
        String option = String.format("-C %f -K \"weka.classifiers.functions.supportVector.RBFKernel -G %f\"", C, gamma);
        model.setOptions(weka.core.Utils.splitOptions(option));
        return model;
    }

    protected void applyFilters(Instances data) throws Exception {
        // from the old code
        if (false) {
            // normalize numeric values --- always a good idea!
            Normalize normalizerFilter = new Normalize();
            normalizerFilter.setInputFormat(data);
            data = Filter.useFilter(data, normalizerFilter);
        }
    }
}
