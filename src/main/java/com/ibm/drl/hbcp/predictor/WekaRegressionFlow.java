package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.regression.WekaRegression;
import com.ibm.drl.hbcp.util.Props;
import java.io.FileReader;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class WekaRegressionFlow {

    public static NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> prepareAttributeValuePairs(AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations, Properties props) {
        AttributeValueCollection<? extends ArmifiedAttributeValuePair> res = annotations;
        // first distribute the empty arm (give to each real arm its values)
        res = res.distributeEmptyArm();
        // filter out unused attribute types
        res = new AttributeValueCollection<>(res.stream()
                .filter(aavp -> {
                    AttributeType type = aavp.getAttribute().getType();
                    return type != AttributeType.ARM /*&& type != AttributeType.SETTING */ && type!= AttributeType.EFFECT;
                })
                .collect(Collectors.toList()));
        // apply the cleaners
        if (!res.isEmpty() && res.stream().findFirst().get() instanceof AnnotatedAttributeValuePair) {
            Cleaners cleaners = new Cleaners(props);
            res = cleaners.clean((AttributeValueCollection<AnnotatedAttributeValuePair>)res);
        }
        // apply the normalizers
        Normalizers normalizers = new Normalizers(props);
        return new NormalizedAttributeValueCollection<>(normalizers, AttributeValueCollection.cast(res));
    }

    public static void main(String[] args) throws Exception {
        Properties extraProps = new Properties();
        if (args.length > 0) {
            extraProps.load(new FileReader(args[0])); // overriding arguments
            System.out.println("Additional properties: " + extraProps.toString());
        }

        PredictionWorkflowManager wfm = new PredictionWorkflowManager(0.8f);
        Properties props = wfm.getProps();
        props = Props.overrideProps(props, extraProps);

        wfm.getInstancesManager().writeArffFiles("prediction/weka/training.arff", "prediction/weka/test.arff");  // to play around directly in Weka (if desired)
        WekaRegression wekaRegress = new WekaRegression(wfm.getTrainInstances(), wfm.getTestInstances());
        weka.classifiers.Classifier model = wekaRegress.train(Float.parseFloat(props.getProperty("svm.c", "0.1")), Float.parseFloat(props.getProperty("svm.gamma", "0.1")));
        List<PredictionTuple> predictions = wekaRegress.predict(model);

        wfm.printAndSaveResults(predictions);
    }
}
