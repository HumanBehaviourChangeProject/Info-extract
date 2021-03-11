/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.predictor.crossvalid.AttributeValueNodes;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;

/**
 * This class contains the ground-truth values (of the entities whose values
 * are to be predicted). For the time being, the code supports C, I and O (outcome
 * qualifiers) as input features.
 * 
 * @author dganguly
 */
public class GroundTruth {
    List<AttributeValueNode> groundTruth[];
    HashSet<String> queryAttribs;
    AttributeValueNodes queryNodes;
    int numQueries;
    
    static Logger logger = LoggerFactory.getLogger(GroundTruth.class);
    
    public GroundTruth(Properties prop, AttributeValueNodes queryNodes) {
        this.queryNodes = queryNodes;
        numQueries = queryNodes.cnodeMap().keySet().size();
        groundTruth = new ArrayList[numQueries];
        
        this.build();
    }

    Set<String> getAllDocNames(List<Map<String, Set<AttributeValueNode>>> inputs) {
        Set<String> docNames = new HashSet<>();

        // collect the doc names from each input set
        for (Map<String, Set<AttributeValueNode>> imap: inputs) {
            docNames.addAll(imap.keySet());
        }
        return docNames;
    }
    
    public GroundTruth(Properties prop, AttributeValueNodes queryNodes, AttributeValueNodes queryNodesGT, String predictionType) {
        
        this.queryNodes = queryNodes;

        List<Map<String, Set<AttributeValueNode>>> inputs = new ArrayList<>(3);
        Map<String, Set<AttributeValueNode>> output = null;
        
        if (false);
        else if (predictionType.equals("V")) {  // predict V given rest
            inputs.add(queryNodes.cnodeMap());
            inputs.add(queryNodes.inodeMap());
            inputs.add(queryNodes.onodeMap());
            output = queryNodesGT==null? queryNodes.ovnodeMap() : queryNodesGT.ovnodeMap();
        }
        else if (predictionType.equals("I")) { // predict I given rest
            inputs.add(queryNodes.cnodeMap());
            inputs.add(queryNodes.onodeMap());
            inputs.add(queryNodes.ovnodeMap());
            output = queryNodesGT==null? queryNodes.inodeMap() : queryNodesGT.inodeMap();
        }
        
        numQueries = getAllDocNames(inputs).size();
        groundTruth = new ArrayList[numQueries];
        
        this.build(inputs, output);
    }

    /**
     * A generalized version which takes as input a list of given input features.
     */ 
    final void build(List<Map<String, Set<AttributeValueNode>>> inputs, Map<String, Set<AttributeValueNode>> output) {
        
        int queryIndex = 0;
        Set<String> docNames = getAllDocNames(inputs);

        for (String docName: docNames) {  // iterate over documents

            // construct ground-truth
            groundTruth[queryIndex] = new ArrayList<>();

            // Add OVs and Os
            Set<AttributeValueNode> alist = output.get(docName);
            if (alist != null) {
                groundTruth[queryIndex].addAll(alist);
            }

            queryIndex++;
        }
    }
    
    /**
     * Iterates over 4-tuples (C, I, O, OV) in each document and builds the ground truth
     * comprised of the O and OV values.
     */
    final void build() {
        
        int queryIndex = 0;                
        for (String docName: queryNodes.cnodeMap().keySet()) {  // iterate over documents

            // construct ground-truth
            groundTruth[queryIndex] = new ArrayList<>();

            // Add OVs and Os
            Set<AttributeValueNode> alist = queryNodes.ovnodeMap().get(docName);
            if (alist != null) {
                groundTruth[queryIndex].addAll(alist);
            }

            if (!queryNodes.includeONodesInQuery()) {  // if the O nodes are in query they aren't in GT and vice-versa
                alist = queryNodes.onodeMap().get(docName);
                if (alist != null) {
                    groundTruth[queryIndex].addAll(alist);
                }
            }            
            queryIndex++;
        }
    }

    public List<AttributeValueNode> groundTruth(int i) { return groundTruth[i]; }
}
