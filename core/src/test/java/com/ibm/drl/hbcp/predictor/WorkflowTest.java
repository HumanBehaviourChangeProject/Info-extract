package com.ibm.drl.hbcp.predictor;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import org.junit.Test;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.util.Props;

/**
 * Checks that the basic Java-only Workflow run correctly.
 */
public class WorkflowTest {

    @Test
    public void testWekaRegressionFlow() throws Exception {
        AttributeValueCollection<AnnotatedAttributeValuePair> annotationsSmall = getSmallAnnotationDataset();
        WekaRegressionFlow flow = new WekaRegressionFlow(
                annotationsSmall,
                annotationsSmall,
                new TrainTestSplitter(0.8),
                Props.loadProperties()
        );
        flow.run();
    }

    @Test
    public void testNearestNeighborQueryFlow() throws Exception {
        AttributeValueCollection<AnnotatedAttributeValuePair> annotationsSmall = getSmallAnnotationDataset();
        // build a NN flow using only this small set of annotated data (the full set takes a full minute)
        NearestNeighborQueryFlow flow = new NearestNeighborQueryFlow(
                annotationsSmall,
                annotationsSmall,
                new TrainTestSplitter(0.8),
                Props.loadProperties()
        );
        flow.run();
    }

    @Test
    public void testParametricGraphKerasDataPreparation() throws Exception {
        AttributeValueCollection<AnnotatedAttributeValuePair> annotationsSmall = getSmallAnnotationDataset();
        final String outNodeVecFileName = "output/prediction/graphs/nodevecs/refVecs.vec";
        
        for (boolean withWords : new boolean[] { false }) { //TODO make the test pass for "true" ??
            ParametricGraphKerasDataPreparation flow = new ParametricGraphKerasDataPreparation(
                    annotationsSmall,
                    annotationsSmall,
                    new TrainTestSplitter(0.8),
                    Props.loadProperties(),
                    "../data/pubmed/hbcpcontext.vecs", // mgleize: nothing has been committed for that
                    "output/testOutputVectors.vec",
                    withWords, null
            );
            flow.prepareData(outNodeVecFileName, "prediction/sentences/train.tsv", "prediction/sentences/test.tsv");
        }
    }

    private AttributeValueCollection<AnnotatedAttributeValuePair> getSmallAnnotationDataset() throws IOException {
        // use only 20 random docs in the annotations
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        Set<Map.Entry<String, Multiset<AnnotatedAttributeValuePair>>> annotations20docs = annotations.byDoc().entrySet().stream()
                .limit(20)
                .collect(Collectors.toSet());
        return new AttributeValueCollection<>(annotations20docs.stream()
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors.toSet()));
    }
}
