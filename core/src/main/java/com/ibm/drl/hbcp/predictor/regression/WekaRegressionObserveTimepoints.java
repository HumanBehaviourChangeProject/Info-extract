package com.ibm.drl.hbcp.predictor.regression;

import com.ibm.drl.hbcp.predictor.DataInstance;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.util.ParsingUtils;
import weka.classifiers.Classifier;
import weka.core.Attribute;
import weka.core.Instance;
import weka.core.Instances;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class extends the regular Weka regression and is just used to check if/how the outcome value prediction
 * changes with the same instances and only the length of the longest follow-up changing (when the outcome value is
 * measured).
 *
 * There is not a dedicated 'Flow' for this but if you modify {@code run() in }{@link com.ibm.drl.hbcp.predictor.WekaRegressionFlow}
 * you can use this instead of WekaRegression.
 *
 */
public class WekaRegressionObserveTimepoints extends WekaRegression {

    private static final double[] TIMEPOINTS = {12, 24, 52};

    public WekaRegressionObserveTimepoints(WekaDataset dataset) {
        super(dataset);
    }

    public WekaRegressionObserveTimepoints(List<DataInstance> trainInstances, List<DataInstance> testInstances) throws IOException {
        this(new WekaDataset(trainInstances, testInstances));
    }

    @Override
    public List<PredictionTuple> predict(Classifier classifier) throws Exception {
        BufferedWriter bw = new BufferedWriter(new FileWriter("observeTimepoints.csv"));
        bw.write("Reference,LongestFollowup,Prediction,12wk,24wk,52wk\n");
        Instances test = WekaDataset.buildWekaInstances(dataset.getHeaders(), dataset.getTestData());

        Attribute followupAtt = test.attribute("Longest follow up");
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
            if (followupAtt != null) {
                double followup = instance.value(followupAtt);
                bw.write(reference + "," + followup + "," + prediction);
                for (double timepoint : TIMEPOINTS) {
                    // replace followup with new timepoint
                    instance.setValue(followupAtt, timepoint);
                    prediction = classifier.classifyInstance(instance);
                    bw.write("," + prediction);
                }
                bw.write('\n');
            }
        }
        bw.close();
        return res;
    }

}
