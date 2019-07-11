/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.util.List;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

/**
 * This evaluation is also applicable for numerical values. Relaxes the evaluation
 * by using multiple categories instead of RMSE on the values (which can sometimes
 * be over-penalizing).
 * 
 * @author dganguly
 */
public class RangeClassifierEvaluationLogic extends BaseEvaluationLogic {

    public RangeClassifierEvaluationLogic(GroundTruth gt) {
        super(gt);
    }

    @Override
    public float evaluate(int qid, List<SearchResult> srchResList) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMetricName() {
        return "Accuracy";
    }
    
}
