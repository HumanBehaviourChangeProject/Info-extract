package com.ibm.drl.hbcp.predictor;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.parser.enriching.AnnotationOutcomesMiner;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.predictor.data.DataSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.EvaluationMetric;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.LooseClassificationAccuracy;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.MeanAbsoluteError;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.MedianBased7ClassesAccuracyMetric;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.RMSE;

import lombok.AccessLevel;
import lombok.Getter;

public class PredictionWorkflow {

    protected final Properties props;
    // replace this with a list to enable cross-validation
    protected final Pair<List<String>, List<String>> docnamesTrainTest;
    @Getter(AccessLevel.PROTECTED)
    private final DataInstanceManager instancesManager;
    @Getter(AccessLevel.PROTECTED)
    private final AttributeValueCollection<ArmifiedAttributeValuePair> trainAVPs;
    @Getter(AccessLevel.PROTECTED)
    private final List<DataInstance> trainInstances;
    @Getter(AccessLevel.PROTECTED)
    private final List<DataInstance> testInstances;

    protected PredictionWorkflow(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                 AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                 DataSplitter splitter, // this can be easily replaced with a generic DataSplitter for cross-validation
                                 Properties props) throws IOException {
        this.props = props;
        int nIntervals = Integer.parseInt(props.getProperty("prediction.graph.coarsening.normalization.numintervals", "0")); // 0 => no quantization
        
        // apply the same processing on both the sets of values to use in training, and the reference
        NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> preparedValues = prepareAttributeValuePairs(values, props, false, nIntervals);
        NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> preparedAnnotations = prepareAttributeValuePairs(annotations, props, false, nIntervals);
        
        // split the set of documents
        List<String> docnames = new ArrayList<>(preparedAnnotations.getDocNames());
        docnamesTrainTest = splitter.getTrainTestSplits(docnames).get(0);
        // create the sets of train and test values
        trainAVPs = AttributeValueCollection.cast(preparedValues.filterByDocs(docnamesTrainTest.getLeft()));
        AttributeValueCollection<ArmifiedAttributeValuePair> testValues = AttributeValueCollection.cast(preparedAnnotations.filterByDocs(docnamesTrainTest.getRight()));
        
        boolean quantized = Integer.parseInt(props.getProperty("prediction.graph.coarsening.normalization.numintervals")) > 0;        
        boolean useLargerContext = Boolean.parseBoolean(props.getProperty("prediction.use_larger_context", "false"));
        
        // create the instance manager
        instancesManager = new DataInstanceManager(trainAVPs, testValues, quantized, useLargerContext);
        
        // finally create the train and test instances
        trainInstances = instancesManager.createDataInstances(docnamesTrainTest.getLeft());
        testInstances = instancesManager.createDataInstances(docnamesTrainTest.getRight());
    }

    /** Applies a sequence of pre-processing on AVPs to make them usable in training and consistent across experiments */
    public static NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> prepareAttributeValuePairs(
            AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
            Properties props,
            boolean expandOutcomeValues, int nIntervals) throws IOException {
        AttributeValueCollection<? extends ArmifiedAttributeValuePair> res = annotations;
        if (expandOutcomeValues && !res.isEmpty() && res.stream().findFirst().get() instanceof AnnotatedAttributeValuePair) {
            // extend the outcome values
            AnnotationOutcomesMiner otherOutcomeMiner = new AnnotationOutcomesMiner(new ReparsePdfToDocument(props), new File(props.getProperty("coll")));
            res = otherOutcomeMiner.withOtherOutcomesInSeparateArms((AttributeValueCollection<AnnotatedAttributeValuePair>)res);
        }
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
            // this warning is checked just before, should be okay
            res = cleaners.clean((AttributeValueCollection<AnnotatedAttributeValuePair>)res);
        }
        // apply the normalizers
        Normalizers normalizers = new Normalizers(props);
        
        NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> normalizedAVPList =
            new NormalizedAttributeValueCollection<>(normalizers, nIntervals, AttributeValueCollection.cast(res));
                
        return normalizedAVPList;
    }

    /** Applies a sequence of pre-processing on AVPs to make them usable in training and consistent across experiments */
    public static NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> prepareAttributeValuePairs(
            AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
            Properties props, int nIntervals) throws IOException {
        return prepareAttributeValuePairs(annotations, props, false, nIntervals);
    }

    public static NormalizedAttributeValueCollection<ArmifiedAttributeValuePair> prepareAttributeValuePairs(
            AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
            Properties props) throws IOException {
        return prepareAttributeValuePairs(annotations, props, false, 0);
    }
    
    public static void printAndSaveResults(List<PredictionTuple> predictions) {
        PredictionTuple.saveTuplesToFile(predictions); // save in file "res.tsv"

        System.out.println("Number of results: " + predictions.size());
        for (EvaluationMetric metric : Lists.newArrayList(
                new RMSE(),
                new MeanAbsoluteError(),
                new LooseClassificationAccuracy(),
                new MedianBased7ClassesAccuracyMetric(predictions)
        )) {
            String metricName = metric.toString();
            double evaluationResult = metric.compute(predictions);
            System.out.println(metricName + ": " + evaluationResult);
        }
    }
}
