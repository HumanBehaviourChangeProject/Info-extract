/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor;

import java.io.IOException;

/**
 *
 * @author debforit
 */
public class NodeAndWordSeqDataAsSingleDataset {
    public static void main(String[] args) throws IOException {
        String additionalProps = args.length>0? args[0]: null;
        boolean withWords = true;
        
        new ParametricGraphKerasDataPreparation().prepareData(1.0f, additionalProps, withWords);
    }
}
