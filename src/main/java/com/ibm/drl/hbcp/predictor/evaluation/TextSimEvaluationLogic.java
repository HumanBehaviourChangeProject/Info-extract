/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import com.ibm.drl.hbcp.extractor.DocVector;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

/**
 * Treats a ground-truth as a text string and evaluates how similar is the
 * predicted value to the true one. Uses character tri-grams for similarity computation.
 * 
 * @author dganguly
 */
public class TextSimEvaluationLogic extends BaseEvaluationLogic {

    public TextSimEvaluationLogic(GroundTruth gt) {
        super(gt);
    }

    /**
     * Sorts the result list and computes average cosine similarity with the ground-truth items
     * @param qid  An id of the query -- a specific test instance.
     * @param srchResList List of top-K similar nodes predicted.
     * @return 
     */
    @Override
    public float evaluate(int qid, List<SearchResult> srchResList) {
        List<AttributeValueNode> gtlist = gt.groundTruth(qid);
        
        Collections.sort(gtlist);
        
        Collections.sort(srchResList, new Comparator<SearchResult>() {
            @Override
            public int compare(SearchResult thisRes, SearchResult thatRes) {
                return Integer.parseInt(thisRes.node.getId()) - Integer.parseInt(thatRes.node.getId());
            }
        });

        StringBuffer buff = new StringBuffer(String.format("GT (%d): ", qid));
        for (AttributeValueNode a: gtlist) {
            buff.append(a.getId()).append(":").append(a.getValue()).append(", ");
        }
        System.out.println(buff.toString());
        
        float sim = 0;
        int i = 0, j = 0, gtlen = gtlist.size(), srchResLen = srchResList.size();
        float this_sim = 0;
        
        for (i=0; i < gtlen; i++) {   // for all the relevant items
            for (j=0; j < srchResLen; j++) {  // for retrieved items
                this_sim = cosineSim(gtlist.get(i), srchResList.get(j).node);
                sim += this_sim;
            }
        }
        
        return sim/(float)(gtlen);
    }
    
    float cosineSim(AttributeValueNode a, AttributeValueNode b) {
        if (!a.getId().equals(b.getId()))
            return 0;
        
        DocVector da = new DocVector(a.getValue(), 3);
        DocVector db = new DocVector(b.getValue(), 3);
        
        return da.cosineSim(db);
    }    

    @Override
    public String getMetricName() {
        return "cos-sim";
    }
}
