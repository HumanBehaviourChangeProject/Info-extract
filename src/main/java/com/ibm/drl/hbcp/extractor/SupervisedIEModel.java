/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import org.apache.lucene.classification.SimpleNaiveBayesClassifier;

import java.io.FileOutputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * A model for a particular BCT
 * @author dganguly
 */
public class SupervisedIEModel implements Serializable {
    transient InformationUnit iu;
    String query;   // the attribite and the learned parameters - query    
    SimpleNaiveBayesClassifier classifier;
    
    public SupervisedIEModel(AbstractDetectPresenceAttribute iu, SimpleNaiveBayesClassifier classifier) {
        this.iu = iu;
        this.query = iu.query;
        this.classifier = classifier;
    }
    
    public String getQuery() { return query; }
    
    public SimpleNaiveBayesClassifier getClassifier() { return classifier; }
    
    public void save(String basePath) throws Exception {
        FileOutputStream fileOut = new FileOutputStream(getFileName(basePath, iu.attribId));        
        ObjectOutputStream out = new ObjectOutputStream(fileOut);
        out.writeObject(this);
        out.close();
        fileOut.close();    
    }
    
    static String getFileName(String basePath, String attribId) { return basePath + "/" + attribId + ".ie"; }    
}
