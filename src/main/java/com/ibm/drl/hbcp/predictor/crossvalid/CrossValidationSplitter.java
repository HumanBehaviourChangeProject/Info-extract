/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.crossvalid;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations.AttribNodeRelation;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import com.ibm.drl.hbcp.predictor.queries.Query;

/**
 * Splits the graph produced from JSON annotations into a train graph and a test graph.
 * The train graph will be used to run node2vec and learn the vectors.
 * The test graph will use these vectors in combination with queries to find out how accurate the vectors for unseen edges.
 * @author dganguly
 */
public class CrossValidationSplitter {
    AttribNodeRelations graph;
    float trainPercentage;  // Easier to implement than folding... select a percentage for training and the rest for testing
    Properties prop;
    
    AttribNodeRelations train;
    AttribNodeRelations test;

    Set<String> queryAttribs;
    
    Map<String, List<AttribNodeRelation>> grpByDocNames;
    List<ArmifiedAttributeValuePair> groundTruth[];
    List<Query> testQueries;
    
    Random random;
    static final int SEED = 123456;
    static Logger logger = LoggerFactory.getLogger(CrossValidationSplitter.class);

    public CrossValidationSplitter(Properties prop, AttribNodeRelations graph) {
        this.prop = prop;
        this.graph = graph;
        this.trainPercentage = Float.parseFloat(prop.getProperty("prediction.train.ratio", "0.7"));
        random = new Random(SEED);
        
        queryAttribs = new HashSet<>();
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.population"));
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.outcomes"));
        queryAttribs.addAll(Normalizers.getAttributeIdsFromProperty(prop, "prediction.testquery.outcomevalues"));
    }
    
    void groupByDocNames() {        
        grpByDocNames = graph
                .getNodeRelations().values()
                .stream().collect(
                    Collectors.groupingBy(AttribNodeRelation::getDocName)
                );                
    }
    
    /*
        Check how many value instances are there... for the o/p type of node...
        Partition the set of value instances into train:test for prediction...
    */
    public void split() {
        
        groupByDocNames();
        
        List<String> docNames = grpByDocNames.keySet().stream().collect(Collectors.toList());
        Collections.shuffle(docNames, random);

        int trainIndexEnd = (int)(trainPercentage * docNames.size());
        int count = 0;
        train = new AttribNodeRelations();
        test = new AttribNodeRelations();
        AttribNodeRelations trainOrTestPtr = train;
        
        for (String docName: docNames) {
            List<AttribNodeRelation> alist = grpByDocNames.get(docName);
            for (AttribNodeRelation a: alist) {
                trainOrTestPtr.add(a);
            }    
            count++;
            if (count == trainIndexEnd) {
                trainOrTestPtr = test;
            }
        }
    }
    
    public AttribNodeRelations getTrainingSet() { return train; } 
    public AttribNodeRelations getTestSet() { return test; }

}
