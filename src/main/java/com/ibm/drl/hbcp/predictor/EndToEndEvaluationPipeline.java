package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.predictor.graph.Graph;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.evaluation.Evaluator;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;

/**
 * Sets up a full pipeline for laboratory based com.ibm.drl.hbcp.experiments, where we evaluate
 * the quality of predictions by running cross-validation com.ibm.drl.hbcp.experiments.
 *
 * @author fbonin, dganguly, marting
 */
public class EndToEndEvaluationPipeline {

    public static void main(String[] args) throws Exception {
        Properties props = new Properties();
        props.load(new FileReader(BaseDirInfo.getPath("init.properties")));
        
        // Build graph from JSON
        RelationGraphBuilder rgb = new RelationGraphBuilder(props);
        AttribNodeRelations atRelations = rgb.getGraph();
        
        CrossValidationSplitter splitter = new CrossValidationSplitter(props, atRelations);
        splitter.split();
        
        Graph graph = splitter.getTrainingSet();
        
        // run Node2Vec
        Node2Vec node2Vec = new Node2Vec(graph, props);
        NodeVecs nodeVecs = node2Vec.getNodeVectors();
        
        Evaluator evaluator = new Evaluator(props, atRelations);
        evaluator.predictAndEvaluate(nodeVecs);
    }
}
