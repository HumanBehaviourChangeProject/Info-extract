/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * A base class for any evaluator.
 * @author dganguly
 */
public abstract class BaseEvaluationLogic implements EvaluationLogic {

    GroundTruth gt;
    Logger logger;
    
    public BaseEvaluationLogic(GroundTruth gt) {
        this.gt = gt;
        logger = LoggerFactory.getLogger(this.getClass());
    }
    
    public List<AttributeValueNode> getGTList(int qid, List<SearchResult> srchResList) {
            
        List<AttributeValueNode> gtlist = gt.groundTruth(qid);
        
        Collections.sort(gtlist);
        
        Collections.sort(srchResList, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult thisRes, SearchResult thatRes) {
                return Integer.parseInt(thisRes.node.getId()) - Integer.parseInt(thatRes.node.getId());
            }
        });

        // Printing the GT list
        StringBuffer buff = new StringBuffer(String.format("GT (%d): ", qid));
        for (AttributeValueNode a: gtlist) {
            buff.append(a.getId()).append(":").append(a.getValue()).append(", ");
        }
        System.out.println(buff.toString());
        
        return gtlist;
    }
}
