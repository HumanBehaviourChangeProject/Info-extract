/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.util.List;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

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

    @Override
    public float evaluate(int qid, List<SearchResult> srchResList) {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    @Override
    public String getMetricName() {
        return "RMSE";
    }
    
}
