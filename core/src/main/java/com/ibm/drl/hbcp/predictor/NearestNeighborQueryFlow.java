package com.ibm.drl.hbcp.predictor;

import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.data.DataSplitter;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.predictor.graph.QueryRunner;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import com.ibm.drl.hbcp.util.Props;

public class NearestNeighborQueryFlow extends PredictionWorkflow {

    protected NearestNeighborQueryFlow(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                       AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                       DataSplitter splitter,
                                       Properties props) throws IOException {
        super(values, annotations, splitter, props);
    }

    public List<PredictionTuple> run() throws IOException {
        RelationGraphBuilder rgb = getRelationGraphBuilder(props, getTrainAVPs());
        AttribNodeRelations graph = rgb.getGraph(true);
        Node2Vec node2Vec = new Node2Vec(graph, props);

        NodeVecs nodeVecs = node2Vec.getNodeVectors();

        QueryRunner queryRunner = new QueryRunner(nodeVecs);
        List<PredictionTuple> predictions = queryRunner.predict(getTestInstances());
        return predictions;
    }

    protected RelationGraphBuilder getRelationGraphBuilder(Properties props, AttributeValueCollection<ArmifiedAttributeValuePair> values) {
        return new RelationGraphBuilder(props, values);
    }

    public static void main(String[] args) throws IOException {
        Properties extraProps = new Properties();
        if (args.length > 0) {
            extraProps.load(new FileReader(args[0])); // overriding arguments
        }
        Properties props = Props.overrideProps(Props.loadProperties(), extraProps);
        JSONRefParser refParser = new JSONRefParser(props);
        
        
        NearestNeighborQueryFlow flow = new NearestNeighborQueryFlow(
                refParser.getAttributeValuePairs(),
                refParser.getAttributeValuePairs(),
                new TrainTestSplitter(0.8),
                props
        );
        List<PredictionTuple> predictions = flow.run();
        printAndSaveResults(predictions);        
        
    }
}
