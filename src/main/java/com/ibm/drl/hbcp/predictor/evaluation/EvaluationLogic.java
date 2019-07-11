/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.util.List;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

/**
 * Interface for evaluator. Each concrete evaluator must implement the 'evaluate' and the 'getMetricName' methods.
 * @author dganguly
 */
public interface EvaluationLogic {
    float evaluate(int qid, List<SearchResult> srchResList);
    public String getMetricName();
}
