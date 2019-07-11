/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.predictor.crossvalid.QueryNodes;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.queries.Query;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;

/**
 * This class evaluates predictions. The evaluation logic is abstracted out
 * and depends on the type of the {@link EvaluationLogic} object.
 * @author dganguly
 */
public class Evaluator {
    QueryNodes queryNodes;
    GroundTruth groundTruth;
    EvaluationLogic evaluationLogic;
    
    static HashMap<String, EvaluationLogic> evalLogicMap;
    
    public Evaluator(Properties prop, AttribNodeRelations test) {
        queryNodes = new QueryNodes(prop, test);
        queryNodes.build();
        groundTruth = new GroundTruth(prop, queryNodes);
        groundTruth.build();
        
        evaluationLogic = getEvaluationLogic(prop, groundTruth);
    }
    
    static EvaluationLogic getEvaluationLogic(Properties prop, GroundTruth groundTruth) {
        if (evalLogicMap == null) {
            evalLogicMap = new HashMap<>();
            evalLogicMap.put("text", new TextSimEvaluationLogic(groundTruth));
            evalLogicMap.put("regressionvalue", new RegressionValueEvaluationLogic(groundTruth));
            evalLogicMap.put("classifyrange", new RangeClassifierEvaluationLogic(groundTruth));
        }
        
        String name = prop.getProperty("evaluation.type", "text");
        return evalLogicMap.get(name);
    }

    /**
     * Takes as input node vector objects, executes queries and prints out
     * aggregated evaluation metrics.
     * @param nodeVecs 
     */
    public void predictAndEvaluate(NodeVecs nodeVecs) {
        
        List<Query> testQueries = queryNodes.buildQueries();

        int numQueries = testQueries.size();
        float sim = 0;
        int nQueriesEvaluated = 0;
        
        for (int i=0; i < numQueries; i++) {
            Query testQuery = testQueries.get(i);
            
            System.out.println("Test Query: " + testQuery);
            testQuery = testQuery.filter(result ->
                    result.node.getAttribute().getType() == AttributeType.OUTCOME ||
                            result.node.getAttribute().getType() == AttributeType.OUTCOME_VALUE);  // set the filter

            List<SearchResult> results = testQuery.searchTopK(nodeVecs, 5);

            StringBuffer buff = new StringBuffer("Results: ");
            for (SearchResult res : results) {
                buff.append(res).append("#");
            }
            if (buff.length() > 0)
                buff.deleteCharAt(buff.length()-1);

            System.out.println(buff.toString());
            
            float this_sim = evaluationLogic.evaluate(i, results);
            sim += this_sim;
            nQueriesEvaluated++;
        }
        
        System.out.println("Mean average prediction sim: " + sim/(float)nQueriesEvaluated);
    }
}
