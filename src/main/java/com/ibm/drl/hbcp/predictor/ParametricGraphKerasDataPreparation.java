package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import com.ibm.drl.hbcp.util.Props;

import java.io.IOException;
import java.util.Properties;

import java.io.FileReader;

public class ParametricGraphKerasDataPreparation {

    public void prepareData(float trainingRatio, String additionalPropsFile, boolean withWords) throws IOException {
        boolean includeTextAttribs = false;
        
        final String PUBMED_FILE = "data/pubmed/hbcpcontext.vecs";
        // following is the o/p extended dictionary file
        final String CONCATENATED_NODE_WORD_FILE = "prediction/graphs/nodevecs/nodes_and_words.vec";
        
        Properties extraProps = new Properties();
        
        if (additionalPropsFile != null) {
            extraProps.load(new FileReader(additionalPropsFile)); // overriding arguments
        }

        PredictionWorkflowManager wfm = new PredictionWorkflowManager(trainingRatio, PUBMED_FILE, CONCATENATED_NODE_WORD_FILE);

        Properties props = wfm.getProps();
        props = Props.overrideProps(props, extraProps);
        
        final RelationGraphBuilder rgb = new RelationGraphBuilder(props, wfm.getTrainAVPs());
        final AttribNodeRelations graph = rgb.getGraph(true);
        Node2Vec node2Vec = new Node2Vec(graph, props);  // this also writes embedding vector to disk
        
        // Save the embedding vectors to be used in the Keras flow
        NodeVecs ndvecs = node2Vec.trainSaveAndGetNodeVecs("prediction/graphs/nodevecs/refVecs.vec");

        // dump files needed to run code on keras
        // changed the file dumping code to local folders because on servers. the /tmp
        // folder may not be accessible
        /*
        wfm.getInstancesManager().writeSparseVecsForSplit("prediction/sentences/train.tsv", wfm.getTrainingDocs(),
                includeTextAttribs, ndvecs, withWords);
        wfm.getInstancesManager().writeSparseVecsForSplit("prediction/sentences/test.tsv", wfm.getTestDocs(),
                includeTextAttribs, ndvecs, withWords);
        */
        
        wfm.getInstancesManager().writeTrainTestFiles(
                "prediction/sentences/train.tsv", wfm.getTrainingDocs(),
                "prediction/sentences/test.tsv", wfm.getTestDocs(),
                includeTextAttribs, ndvecs, withWords);

        // will need to evaluate after keras has run... so no evaluation here
    }
    
    public static void main(String[] args) throws IOException {
        String additionalProps = args.length>0? args[0]: null;
        boolean withWords = true;
        new ParametricGraphKerasDataPreparation().prepareData(0.8f, additionalProps, withWords);
    }
}
