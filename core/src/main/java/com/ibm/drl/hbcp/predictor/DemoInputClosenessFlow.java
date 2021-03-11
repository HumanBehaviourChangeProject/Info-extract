package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.api.ArmSimilarityResult;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.similarity.QueryArmSimilarity;
import com.ibm.drl.hbcp.util.Props;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * A simple prediction system using the query arm similarity developed for the "prediction insight" API call (featured
 * in Prediction v2). We simply predict the output value of the top relevant document/arm.
 *
 * Should be bad (but give an idea of how relevant the returned list of documents is).
 *
 * @author mgleize
 */
public class DemoInputClosenessFlow extends PredictionWorkflow {

    private final QueryArmSimilarity armSimilarity;

    protected DemoInputClosenessFlow(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                     AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                     TrainTestSplitter splitter,
                                     Properties props) throws IOException {
        super(values, annotations, splitter, props);
        armSimilarity = new QueryArmSimilarity(getTrainAVPs(), Props.loadProperties());
    }

    public void run() throws Exception {
        List<PredictionTuple> predictions = predict();
        printAndSaveResults(predictions);
    }

    private List<PredictionTuple> predict() throws IOException {
        List<PredictionTuple> res = new ArrayList<>();
        for (DataInstance instance : getTestInstances()) {
            // turn into good old AVPs
            List<AttributeValuePair> avps = new ArrayList<>(instance.getX());
            // retrieve arm similarities
            List<ArmSimilarityResult> armSimilarityResults = armSimilarity.querySimilarityByArm(avps, null);
            // predict the first result
            if (!armSimilarityResults.isEmpty()) {
                ArmSimilarityResult best = armSimilarityResults.get(0);
                double prediction = best.getOutcomeValue();
                double ref = instance.getYNumeric();
                res.add(new PredictionTuple(ref, prediction));
            } else {
                // predict 0 if we don't have any results (which shouldn't happen too much anyway)
                System.err.println("0 predicted in " + DemoInputClosenessFlow.class.getName());
                res.add(new PredictionTuple(instance.getYNumeric(), 0.0));
            }
        }
        return res;
    }

    public static void main(String[] args) throws Exception {
        Properties extraProps = new Properties();
        if (args.length > 0) {
            extraProps.load(new FileReader(args[0])); // overriding arguments
            System.out.println("Additional properties: " + extraProps.toString());
        }
        Properties props = Props.loadProperties();
        props = Props.overrideProps(props, extraProps);
        JSONRefParser refParser = new JSONRefParser(props);
        DemoInputClosenessFlow flow = new DemoInputClosenessFlow(
                refParser.getAttributeValuePairs(), //ExtractPrediction.armify(ExtractPrediction.loadFlairExtraction()),
                refParser.getAttributeValuePairs(),
                new TrainTestSplitter(0.8),
                props
        );
        flow.run();
    }
}
