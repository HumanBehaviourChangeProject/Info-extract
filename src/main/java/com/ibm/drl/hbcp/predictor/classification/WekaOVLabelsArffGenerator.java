/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.classification;

import com.ibm.drl.hbcp.predictor.PredictionWorkflowManager;

/**
 *
 * @author debforit
 */
public class WekaOVLabelsArffGenerator {
    public static void main(String[] args) throws Exception {
        PredictionWorkflowManager wfm = new PredictionWorkflowManager(1.0f);
        wfm.getInstancesManager().writeArffFiles("prediction/weka/train_ovlabels.arff", "prediction/weka/test_ovlabels");  // to play around directly in Weka (if desired)
    }
    
}
