/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

/**
 * A base class for any evaluator.
 * @author dganguly
 */
public abstract class BaseEvaluationLogic implements EvaluationLogic {

    GroundTruth gt;
    
    public BaseEvaluationLogic(GroundTruth gt) {
        this.gt = gt;
    }
}
