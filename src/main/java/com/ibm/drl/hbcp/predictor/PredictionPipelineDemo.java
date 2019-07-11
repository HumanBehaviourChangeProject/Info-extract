/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor;

import java.io.FileReader;
import java.util.List;
import java.util.Properties;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.predictor.graph.*;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.Query;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.util.Props;

/**
 * Testing class where you can run simple queries and display their results in a console.
 * @author fbonin
 */
public class PredictionPipelineDemo {
    
    private NodeVecs train(String propFile) throws Exception {
        Properties props = new Properties();
        props.load(new FileReader(BaseDirInfo.getPath("init.properties")));
        
        //build graph
        RelationGraphBuilder rgb = new RelationGraphBuilder(Props.loadProperties(propFile));
        AttribNodeRelations graph = rgb.getGraph();
        
        com.ibm.drl.hbcp.predictor.graph.Node2Vec node2Vec = new com.ibm.drl.hbcp.predictor.graph.Node2Vec(graph, props);
        NodeVecs nodeVecs = node2Vec.getNodeVectors();
        
        return nodeVecs;
    }

    private void run(Query query,  NodeVecs nodeVecs, int topK) {
        // runs the query
        List<SearchResult> results = query.searchTopK(nodeVecs, topK);
        int i = 0;
        for (SearchResult res : results) {
            i++;
            System.out.println("#" + i);
            System.out.println(res);
        }
    }

    public static void main(String[] args) throws Exception {
        //String propFile="init.properties";
        if (args.length == 0) {
            args = new String[1];
            System.err.println("Usage: java PredictionPipelineDemo <prop-file>");
            args[0] = "init.properties";
        }
        
        PredictionPipelineDemo pp = new PredictionPipelineDemo();
        NodeVecs nodeVecs= pp.train(args[0]);
        // 
        Properties props = Props.loadProperties(args[0]);
        PredictableOutputs pOs = new PredictableOutputs(props.getProperty("prediction.attribtype.predictable.output"));
        
        // create a query
        Query query = new AndQuery(Lists.newArrayList(new NodeQuery(AttributeValueNode.parse("C:4507564:25.99")), new NodeQuery(AttributeValueNode.parse("I:3675717:1.0"))))
                .filter(result -> result.node.getAttribute().getType() == AttributeType.OUTCOME);

        pp.run(query, nodeVecs, 10);

        // ... but only with output/outcome that we consider 'predictable'  (none at the moment)
        query = new AndQuery(Lists.newArrayList(new NodeQuery(AttributeValueNode.parse("C:4507564:25.99")), new NodeQuery(AttributeValueNode.parse("I:3675717:1.0"))))
                .filter(result -> pOs.contains(result.node.getAttribute().getId()));
        pp.run(query, nodeVecs, 10);
}
    
    
}
