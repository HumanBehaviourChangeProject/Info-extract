package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.predictor.graph.QueryRunner;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import com.ibm.drl.hbcp.util.Props;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import java.io.FileReader;

public class NearestNeighborQueryFlow {

    private final Properties extraProps;

    public NearestNeighborQueryFlow(Properties extraPropreties) {
        extraProps = extraPropreties;
    }

    public NearestNeighborQueryFlow() {
        this(new Properties());
    }

    public void run() throws IOException {
        PredictionWorkflowManager wfm = new PredictionWorkflowManager(0.8);

        Properties props = wfm.getProps();
        props = Props.overrideProps(props, extraProps);

        RelationGraphBuilder rgb = getRelationGraphBuilder(props, wfm.getTrainAVPs());
        AttribNodeRelations graph = rgb.getGraph(true);
        Node2Vec node2Vec = new Node2Vec(graph, props);

        NodeVecs nodeVecs = node2Vec.getNodeVectors();

        QueryRunner queryRunner = new QueryRunner(nodeVecs);
        List<PredictionTuple> predictions = queryRunner.predict(wfm.getTestInstances());

        PredictionWorkflowManager.printAndSaveResults(predictions);
    }

    protected RelationGraphBuilder getRelationGraphBuilder(Properties props, AttributeValueCollection<ArmifiedAttributeValuePair> values) {
        return new RelationGraphBuilder(props, values);
    }

    public static void main(String[] args) throws IOException {
        Properties extraProps = new Properties();
        
        if (args.length > 0) {
            extraProps.load(new FileReader(args[0])); // overriding arguments
        }
        NearestNeighborQueryFlow flow = new NearestNeighborQueryFlow(extraProps);
        flow.run();
    }
}
