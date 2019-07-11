/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.predictor.crossvalid.QueryNodes;

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
    QueryNodes queryNodes;
    int numQueries;
    
    static Logger logger = LoggerFactory.getLogger(GroundTruth.class);
    
    public GroundTruth(Properties prop, QueryNodes queryNodes) {
        this.queryNodes = queryNodes;
        numQueries = queryNodes.cnodeMap().keySet().size();
        groundTruth = new ArrayList[numQueries];
    }
    
    /**
     * Iterates over 4-tuples (C, I, O, OV) in each document and builds the ground truth
     * comprised of the O and OV values.
     */
    void build() {
        
        int queryIndex = 0;                
        for (String docName: queryNodes.cnodeMap().keySet()) {  // iterate over documents

            // construct ground-truth
            groundTruth[queryIndex] = new ArrayList<>();

            // Add OVs and Os
            List<AttributeValueNode> alist = queryNodes.ovnodeMap().get(docName);
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
