package com.ibm.drl.hbcp.experiments.ie.classification.bct;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.experiments.flair.GenerateTrainingData_NameAsCategory;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;
import weka.classifiers.Evaluation;
import weka.classifiers.functions.SMO;
import weka.core.Attribute;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;
import weka.filters.unsupervised.attribute.StringToWordVector;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * This is for an HBCP action item to compare normal binary classification to the binary classification from Flair
 * extraction.  We try the train-test splits used for Flair and then use SVM classification to classify if a
 * document has a BCT or not (not sure if we handle the armified case here).
 */
public class BinaryBctClassification {

    private final JSONRefParser referenceAnnotation;

    private BinaryBctClassification() throws IOException {
        // read annotation from json
        referenceAnnotation = new JSONRefParser(Props.getDefaultPropFilename());
    }

    public static void main(String[] args) throws Exception {
        GenerateTrainingData_NameAsCategory genTrainTest = new GenerateTrainingData_NameAsCategory();
        Map<String, List<String>> trainTestMap = genTrainTest.generateTrainingTestingFiles();

        BinaryBctClassification bctClassification = new BinaryBctClassification();
        List<String> bcts = bctClassification.getBcts();
        for (String bct : bcts) {
            bctClassification.singleBctClassification(trainTestMap, bct);
        }

    }

    private List<String> getBcts() {
        return Arrays.asList("1.1.Goal setting (behavior)", "1.2 Problem solving ","1.4 Action planning","2.2 Feedback on behaviour ",
                "2.3 Self-monitoring of behavior","3.1 Social support (unspecified)","5.1 Information about health consequences",
                "5.3 Information about social and environmental consequences","11.1 Pharmacological support ","11.2 Reduce negative emotions ",
                "4.1 Instruction on how to perform the behavior","4.5. Advise to change behavior","Aggregate patient role","Hospital facility",
                "Doctor-led primary care facility",
                "Smoking",
        "Self report",
        "Odds Ratio",
        "Face to face", "Distance",
        "Patch",
         "Individual",
        "Group-based",
        "Cognitive Behavioural Therapy",
         "Mindfulness",
        "Motivational Interviewing",
        "Brief advice",
        "Physical activity",
        "Pharmaceutical company funding",
        "Tobacco company funding",
        "Research grant funding",
        "No funding",
        "Pharmaceutical company competing interest",
        "Tobacco company competing interest",
        "Research grant competing interest",
        "No competing interest"
        );
//        return referenceAnnotation.getAttributes().stream()
//                .filter(e -> e.getType() == AttributeType.INTERVENTION)
//                .map(com.ibm.drl.hbcp.core.attributes.Attribute::getName)
//                .collect(Collectors.toList());
    }

    private void singleBctClassification(Map<String, List<String>> trainTestMap, String bct) throws Exception {
        // create raw string instances for train/test
        Instances trainStringInstances = createStringInstances(trainTestMap.get("train"), bct);
        Instances testStringInstances = createStringInstances(trainTestMap.get("test"), bct);

        // filter to convert string --> bag of words features
        StringToWordVector filter = new StringToWordVector(5000);
        filter.setInputFormat(trainStringInstances);
        filter.setIDFTransform(true);
        filter.setLowerCaseTokens(true);

        Instances trainVectorizedInstances = StringToWordVector.useFilter(trainStringInstances, filter);
        Instances testVectorizedInstances = StringToWordVector.useFilter(testStringInstances, filter);

        SMO classifier = new SMO();
//        classifier.setOptions(options);
        classifier.buildClassifier(trainVectorizedInstances);
        Evaluation evaluation = new Evaluation(trainVectorizedInstances);
        evaluation.evaluateModel(classifier, testVectorizedInstances);
        double negF1 = evaluation.fMeasure(0);
        if (Double.isNaN(negF1)) {
            negF1 = 0.0;
        }
        double posF1 = evaluation.fMeasure(1);
        if (Double.isNaN(posF1)) posF1 = 0.0;
        double macroF1 = (negF1 + posF1) / 2;  // average of positive and negative F1
        System.out.println(bct + "\t" + macroF1 + "\t" + posF1 + "\t" + negF1);
//        System.out.println(evaluation.toSummaryString());
//        double[][] confusionMatrix = evaluation.confusionMatrix();
//        System.out.println("" + confusionMatrix[0][0] + ", "+ confusionMatrix[0][1] + "," + confusionMatrix[1][0] + "," + confusionMatrix[1][1]);
    }

    private Instances createStringInstances(List<String> pdfNames, String bct) {
        AttributeValueCollection<AnnotatedAttributeValuePair> avc = referenceAnnotation.getAttributeValuePairs();
//        List<Instance> instances = new ArrayList<>();
        ArrayList<Attribute> attributeList = new ArrayList<>(2);
        attributeList.add(new Attribute("bctText", true));  // string attribute
//        attributeList.add(new Attribute("bct"));  // numeric attribute
        attributeList.add(new Attribute("bctLabel", Arrays.asList("0", "1")));  // nominal attribute
        Instances dataset = new Instances("", attributeList, pdfNames.size());
        dataset.setClassIndex(dataset.numAttributes()-1);
        for (String pdfName : pdfNames) {
            Multiset<AnnotatedAttributeValuePair> pairsInDoc = avc.getPairsInDoc(pdfName);
            // get all text?  All arm text?  Only BCT text?
            StringBuilder sb = new StringBuilder();
            boolean boolLabel = false;
            for (AnnotatedAttributeValuePair avp : pairsInDoc) {
                sb.append(avp.getContext()).append(" ");
                // try first with Goal Setting
                if (avp.getAttribute().getName().equals(bct)) {
                    boolLabel = true;
                }
            }
            Instance inst = new DenseInstance(2);
            inst.setDataset(dataset);
            inst.setValue(0, sb.toString());
//            inst.setValue(1, boolLabel ? 1 : 0);  // numeric
            inst.setValue(1, boolLabel ? "1" : "0");  // nominal
            dataset.add(inst);
        }
        return dataset;
    }
}
