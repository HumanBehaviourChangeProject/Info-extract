/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import java.util.ArrayList;

import java.util.List;

/**
 * This class is to be used when we treat the output as a value. Only applicable for
 * outcome value nodes. Stub-code: Not implemented currently.
 * 
 * @author dganguly
 */
public class RegressionValueEvaluationLogic extends BaseEvaluationLogic {

    public RegressionValueEvaluationLogic(GroundTruth gt) {
        super(gt);
    }
    
    float computeAvgRef(List<AttributeValueNode> gtlist) {
        AttributeValueNode a;
        int gtlen = gtlist.size();
        float avg = 0;
        
        for (int i=0; i < gtlen; i++) {   // for all the relevant items
            a = gtlist.get(i);
            if (a==null || a.getNumericValue()==null)
                continue;
            
            avg += a.getNumericValue().floatValue();
        }
        return gtlen==0? 0 : avg/gtlen;
    }

    // To compute RMSE, the code flow should make suret that the
    // srchResList size is 1, i.e. only one value is predicted
    // per output attribute.
    public float evaluate(int qid, List<SearchResult> srchResList) {
        List<AttributeValueNode> gtlist = getGTList(qid, srchResList);
        
        int i = 0, j = 0, gtlen = gtlist.size(), srchResLen = srchResList.size();
        float rmse = 0, diff;
        float p, r;
        AttributeValueNode a, b;
        
        for (j=0; j < srchResLen; j++) {  // for retrieved items
            b = srchResList.get(j).node;
            if (b==null || b.getNumericValue()==null)
                continue;
            p = b.getNumericValue().floatValue();

            float mindiff = Float.MAX_VALUE;
            float bestPred = 0;
            int best_i = 0;
            
            for (i=0; i < gtlen; i++) {   // for all the relevant items
                a = gtlist.get(i);
                if (a==null || a.getNumericValue()==null)
                    continue;
                r = a.getNumericValue().floatValue();
                
                if (!a.getId().equals(b.getId()))
                    continue;
            
                diff = (p-r)*(p-r);
                if (diff < mindiff) {
                    mindiff = diff;
                    bestPred = p;
                    best_i = i;
                }
            }
            
            if (i > 0) {
                rmse = mindiff;
                System.out.println("Instance " + qid + ": " + b.getAttribute().getName() + " (predicted, ref): " +
                        String.format("%.4f, %.4f", bestPred, gtlist.get(best_i).getNumericValue().floatValue()));
            }
        }
        
        return (float)Math.sqrt(rmse);
    }

    @Override
    public String getMetricName() {
        return "RMSE";
    }

    @Override
    public PredictionTuple evaluate(int qid, SearchResult predResult) {
        AttributeValueNode a, b;
        
        List<SearchResult> predResults = new ArrayList<>();
        predResults.add(predResult);
        
        List<AttributeValueNode> gtlist = getGTList(qid, predResults);
        if (gtlist == null || gtlist.isEmpty())
            return new PredictionTuple(-1, -1);
        a = gtlist.get(0);
        if (a == null || a.getNumericValue()==null)
            return new PredictionTuple(-1, -1);
        
        double r = a.getNumericValue();
        
        b = predResult.node;
        if (b==null || b.getNumericValue()==null)
            return new PredictionTuple(qid, -1);
        
        double p = b.getNumericValue();
        return new PredictionTuple(r, p);
    }
    
}
