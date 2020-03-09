/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import static com.ibm.drl.hbcp.predictor.WekaRegressionFlow.prepareAttributeValuePairs;
import static com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter.createFromExtractedIndex;

import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.EvaluationMetric;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.LooseClassificationAccuracy;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.MeanAbsoluteError;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.RMSE;
import com.ibm.drl.hbcp.util.Props;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import lombok.Data;

/**
 *
 * @author dganguly
 */
@Data
public class PredictionWorkflowManager {
    final Properties props;
    List<DataInstance> trainInstances;
    List<DataInstance> testInstances;
    List<String> trainingDocs;
    List<String> testDocs;
    DataInstanceManager instancesManager;
    AttributeValueCollection<ArmifiedAttributeValuePair> trainAVPs;
    AttributeValueCollection<ArmifiedAttributeValuePair> normalizedAvps;
    
    public PredictionWorkflowManager(double trainTestRatio) throws IOException {
        props = Props.loadProperties();
        JSONRefParser jsonRefParser = new JSONRefParser(props);
        // get the annotations
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = jsonRefParser.getAttributeValuePairs();
        NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> normalizedAnnotations = prepareAttributeValuePairs(annotations, props);
        // the splitter works on the reference set so that we get all the valid instances
        CrossValidationSplitter trainTestSplitter = new CrossValidationSplitter(AttributeValueCollection.cast(normalizedAnnotations), trainTestRatio);
        // the train values can come either from the reference or from the extraction
        AttributeValueCollection<ArmifiedAttributeValuePair> allAVPs;
        if (props.getProperty("prediction.source").equals("gt")) {
            allAVPs = AttributeValueCollection.cast(annotations);
        } else {
            allAVPs = AttributeValueCollection.cast(createFromExtractedIndex(jsonRefParser, props));
        }
        allAVPs = AttributeValueCollection.cast(prepareAttributeValuePairs(allAVPs, props));
        // now build the final value set from the ref (test) and allAVPs (train)
        List<ArmifiedAttributeValuePair> finalAvps = allAVPs.stream()
                .filter(avp -> trainTestSplitter.getTrainingDocs().contains(avp.getDocName()))
                .collect(Collectors.toList());
        finalAvps.addAll(normalizedAnnotations.stream()
                .filter(avp -> trainTestSplitter.getTestDocs().contains(avp.getDocName()))
                .collect(Collectors.toList()));
        normalizedAvps = new AttributeValueCollection<>(finalAvps);
        instancesManager = new DataInstanceManager(AttributeValueCollection.cast(normalizedAvps),
                trainTestSplitter.getTrainingDocs(), trainTestSplitter.getTestDocs());
        trainInstances = instancesManager.createDataInstances(trainTestSplitter.getTrainingDocs());  // training split
        testInstances = instancesManager.createDataInstances(trainTestSplitter.getTestDocs());  // test split
        
        this.trainingDocs = trainTestSplitter.getTrainingDocs();
        this.testDocs = trainTestSplitter.getTestDocs();
        
        trainAVPs = AttributeValueCollection.cast(allAVPs.filterByDocs(trainingDocs));
    }
    
    public PredictionWorkflowManager(double trainTestRatio, String pubMedFile, String avgVecOutFile) throws IOException {
        this(trainTestRatio);
        instancesManager.initWithPreTrainedWordVecs(pubMedFile, avgVecOutFile);
    }
    
    public static void printAndSaveResults(List<PredictionTuple> predictions) {
        PredictionTuple.saveTuplesToFile(predictions); // save in file "res.tsv"

        System.out.println("Number of results: " + predictions.size());
        for (EvaluationMetric metric : Lists.newArrayList(
                new RMSE(),
                new MeanAbsoluteError(),
                new LooseClassificationAccuracy())) {
            String metricName = metric.toString();
            double evaluationResult = metric.compute(predictions);
            System.out.println(metricName + ": " + evaluationResult);
        }
    }
}
