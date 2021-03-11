package com.ibm.drl.hbcp.predictor;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;
import com.ibm.drl.hbcp.predictor.data.DataSplitter;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.regression.WekaRegression;
import com.ibm.drl.hbcp.util.Props;

public class WekaRegressionFlow extends PredictionWorkflow {

    protected WekaRegressionFlow(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                 AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                 DataSplitter splitter,
                                 Properties props) throws IOException {
        super(values, annotations, splitter, props);
    }

    public List<PredictionTuple> run() throws Exception {
        WekaRegression wekaRegress = new WekaRegression(getTrainInstances(), getTestInstances());
        weka.classifiers.Classifier model = wekaRegress.train(Float.parseFloat(props.getProperty("svm.c", "0.1")), Float.parseFloat(props.getProperty("svm.gamma", "0.1")));
        List<PredictionTuple> predictions = wekaRegress.predict(model);
        return predictions;
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
        
        AttributeValueCollection extractedValues = CrossValidationSplitter.obtainAttribVals(props);
        WekaRegressionFlow flow = new WekaRegressionFlow(
                extractedValues,
                refParser.getAttributeValuePairs(),
                new TrainTestSplitter(0.8),
                props
        );
        flow.getInstancesManager().writeArffFiles("prediction/weka/training.arff", "prediction/weka/test.arff");  // to play around directly in Weka (if desired)
        List<PredictionTuple> predictions = flow.run();
        printAndSaveResults(predictions);
    }
}
