/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

import java.util.List;

/**
 * This evaluation is also applicable for numerical values. Relaxes the evaluation
 * by using multiple categories instead of RMSE on the values (which can sometimes
 * be over-penalizing).
 * 
 * @author dganguly
 */
public class BCTEvaluationLogic extends BaseEvaluationLogic {

    public BCTEvaluationLogic(GroundTruth gt) {
        super(gt);
    }

    // Useful to evaluate BCTs. Since we report a list of BCTs as predicted
    // ones, we measure the mean average precision
    @Override
    public float evaluate(int qid, List<SearchResult> srchResList) {
        List<AttributeValueNode> gtlist = getGTList(qid, srchResList);
        
        int i = 0, j = 0, gtlen = gtlist.size(), srchResLen = srchResList.size();
        int p, r;
        AttributeValueNode a, b;
        float cutoff_prec = 0;
        float ap = 0;
        int num_rel_retrieved = 0;
        int num_rel = gtlen;
        
        for (i=0; i < gtlen; i++) {   // for all the relevant items
            a = gtlist.get(i);
            if (a==null || a.getNumericValue()==null)
                continue;
            r = a.getNumericValue().intValue();
            
            for (j=0; j < srchResLen; j++) {  // for retrieved items
                b = srchResList.get(j).node;
                if (b==null || b.getNumericValue()==null)
                    continue;
                p = b.getNumericValue().intValue();
                
                if (!(p==1 && r==1))
                    continue;                
                if (!a.getId().equals(b.getId()))
                    continue;
            
                num_rel_retrieved++;
                cutoff_prec = num_rel_retrieved/(float)(j+1);
                ap += cutoff_prec;
            }
        }
        if (num_rel > 0) {
            ap = ap/(float)num_rel;
            logger.info(String.format("AP (qid=%d, attrib=%s): %.4f", qid, gtlist.get(0).getId(), ap));
        }
        return ap;
    }
    
    @Override
    public String getMetricName() {
        return "Average Precision";
    }

    @Override
    public PredictionTuple evaluate(int qid, SearchResult predResult) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
}
