/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor;

import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Random;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;
import com.ibm.drl.hbcp.predictor.data.CVSplitter;
import com.ibm.drl.hbcp.predictor.data.DataSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.util.Props;

/**
 *
 * @author debforit
 */
public class WekaRegressionCVFlow extends WekaRegressionFlow {
    
    static final int SEED = 123456;
    static final int FOLDS = 5;
    static final Random RAND = new Random(SEED);
    
    public WekaRegressionCVFlow(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                DataSplitter splitter,
                                Properties props) throws IOException {
        super(values, annotations, splitter, props);
    }
    
    /*
    @Override
    public void run() throws Exception {
        WekaDataset dataset = new WekaDataset(getTrainInstances(), getTestInstances());
        WekaRegression wekaRegress = new WekaRegression(dataset);
        Instances train = WekaDataset.buildWekaInstances(dataset.getHeaders(), dataset.getTrainData());
        
        weka.classifiers.Classifier model = wekaRegress.train(Float.parseFloat(props.getProperty("svm.c", "0.1")), Float.parseFloat(props.getProperty("svm.gamma", "0.1")));

        Evaluation e = new Evaluation(train);
        
        e.crossValidateModel(model, train, FOLDS, RAND);
        System.out.println(e.toSummaryString());
    }
    */
    
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
        
        final int NUM_FOLDS = 5;
        CVSplitter splitter = new CVSplitter(NUM_FOLDS);
        List<PredictionTuple> predictions = new ArrayList<>();
        
        for (int i=0; i < NUM_FOLDS; i++) {
            WekaRegressionFlow flow = new WekaRegressionCVFlow(
                    extractedValues,
                    refParser.getAttributeValuePairs(),
                    splitter,
                    props
            );
            predictions.addAll(flow.run());
        }
        printAndSaveResults(predictions);        
    }
}
