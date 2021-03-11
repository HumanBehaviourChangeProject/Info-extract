/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.classification;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.DataInstanceManager;
import com.ibm.drl.hbcp.util.Props;

/**
 *
 * @author debforit
 */
public class WekaOVLabelsArffGenerator {
    public static void main(String[] args) throws Exception {
        AttributeValueCollection<ArmifiedAttributeValuePair> all = AttributeValueCollection.cast(new JSONRefParser(Props.loadProperties()).getAttributeValuePairs());
        DataInstanceManager data = new DataInstanceManager(all, all);
        data.writeArffFiles("prediction/weka/train_ovlabels.arff", "prediction/weka/test_ovlabels");  // to play around directly in Weka (if desired)
    }
    
}
