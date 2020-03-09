package com.ibm.drl.hbcp.experiments.prediction.noncontrolprediction;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.PredictionWorkflowManager;
import com.ibm.drl.hbcp.predictor.WekaRegressionFlow;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.EvaluationMetric;
import com.ibm.drl.hbcp.predictor.regression.WekaRegression;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.tuple.Pair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class NonControlPredictionFlow {

    public static void main(String[] args) throws Exception {
        // load all the annotations and clean/normalize them as usual
        Properties props = Props.loadProperties();
        JSONRefParser parser = new JSONRefParser(props);
        NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> avps = WekaRegressionFlow.prepareAttributeValuePairs(parser.getAttributeValuePairs(), props);
        Map<String, String> controlArmAnnotations = getControlArmAnnotations(new File("data/jsons/Smoking_AllAnnotations_01Apr19.json_ControlGroupArmNames.txt"));
        List<String> docnames = getDocnames(avps, controlArmAnnotations);
        List<PredictionTuple> predictions = new ArrayList<>();
        List<PredictionTuple> predictionsBaseline = new ArrayList<>();
        // splitting
        for (Pair<List<String>, List<String>> trainTestSplit : SimpleSplitter.getKFoldCrossValidationSplits(docnames, 10)) {
            List<String> train = trainTestSplit.getLeft();
            List<String> test = trainTestSplit.getRight();
            // produce the instances
            NonControlPredictionInstanceCreator creator = new NonControlPredictionInstanceCreator(
                    AttributeValueCollection.cast(avps),
                    controlArmAnnotations,
                    train,
                    test
            );
            // create the weka dataset
            WekaDatasetMultiArms dataset = new WekaDatasetMultiArms(creator.getTrain(), creator.getTest());
            WekaRegression regression = new WekaRegression(dataset);
            weka.classifiers.Classifier model = regression.train();
            predictions.addAll(regression.predict(model));
            // try a very simple baseline (copy the OV of the control arm)
            predictionsBaseline.addAll(CopyControlGroupBaseline.predict(creator.getTest()));
        }
        System.out.println("Weka");
        PredictionWorkflowManager.printAndSaveResults(predictions);
        System.out.println("Baseline");
        PredictionWorkflowManager.printAndSaveResults(predictionsBaseline);
    }

    private static List<String> getDocnames(NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> avps, Map<String, String> controlArmAnnotations) throws IOException {
        // restrict the collection only to annotated papers (2-arm papers with control arm annotated)
        // also, distribute the empty arm (important for this experiment)
        AttributeValueCollection<ArmifiedAttributeValuePair> collection = new AttributeValueCollection<>(avps.distributeEmptyArm().stream()
                .filter(aavp -> controlArmAnnotations.containsKey(aavp.getDocName()))
                .collect(Collectors.toList()));
        // split the doc set
        return new ArrayList<>(collection.getDocNames());
    }

    private static Map<String, String> getControlArmAnnotations(File controlArmAnnotationFile) throws IOException {
        Map<String, String> res = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(controlArmAnnotationFile))) {
            String docname;
            while ((docname = br.readLine()) != null) {
                String controlArmName = br.readLine();
                res.put(docname, controlArmName);
            }
        }
        return res;
    }
}
