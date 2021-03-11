/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.crossvalid.AttributeValueNodes;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.EvaluationMetric;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.LooseClassificationAccuracy;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.MeanAbsoluteError;
import com.ibm.drl.hbcp.predictor.evaluation.metrics.RMSE;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.Query;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

/**
 * This class evaluates predictions. The evaluation logic is abstracted out
 * and depends on the type of the {@link EvaluationLogic} object.
 * @author dganguly
 */
public class Evaluator {
    private static final Logger LOGGER = LoggerFactory.getLogger(Evaluator.class);
    private static final List<EvaluationMetric> evaluationMetrics = Lists.newArrayList(
            new RMSE(),
            new MeanAbsoluteError(),
            new LooseClassificationAccuracy()
    );

    private final String predictedOutcomeValueAttributeId;
    protected Properties prop;
    AttributeValueNodes queryNodes;
    GroundTruth groundTruth;
    EvaluationLogic evaluationLogic;
    HashMap<String, AttributeType> filterTypes;
    String outputType;
    
    static HashMap<String, EvaluationLogic> evalLogicMap;
    
    public Evaluator(Properties prop, AttribNodeRelations test, AttribNodeRelations train) {
        this.prop = prop;
        predictedOutcomeValueAttributeId = prop.getProperty("prediction.attribtype.predictable.output");
        //queryNodes = new QueryNodes(prop, test);
        queryNodes = new AttributeValueNodes(prop, test);
        AttributeValueNodes gtOutNodes = new AttributeValueNodes(prop, test, train);// gtTestRels!=null? new AttributeValueNodes(prop, gtTestRels) : null;
        groundTruth = new GroundTruth(prop, queryNodes, gtOutNodes, prop.getProperty("prediction.output.type"));
        
        evaluationLogic = getEvaluationLogic(prop, groundTruth);
        
        filterTypes = new HashMap<>();
        filterTypes.put(AttributeType.POPULATION.getShortString(), AttributeType.POPULATION);
        filterTypes.put(AttributeType.INTERVENTION.getShortString(), AttributeType.INTERVENTION);
        filterTypes.put(AttributeType.OUTCOME.getShortString(), AttributeType.OUTCOME);
        filterTypes.put(AttributeType.OUTCOME_VALUE.getShortString(), AttributeType.OUTCOME_VALUE);
        outputType = prop.getProperty("prediction.output.type");
    }
    
    static EvaluationLogic getEvaluationLogic(Properties prop, GroundTruth groundTruth) {
        if (evalLogicMap == null) {
            evalLogicMap = new HashMap<>();
            evalLogicMap.put("text", new TextSimEvaluationLogic(groundTruth));
            evalLogicMap.put("regressionvalue", new RegressionValueEvaluationLogic(groundTruth));
            evalLogicMap.put("average_prec", new BCTEvaluationLogic(groundTruth));
        }
        
        String evalType = prop.getProperty("prediction.output.type").equals("V")? "regressionvalue" : "average_prec";
        return evalLogicMap.get(evalType);
    }

    AttributeType wantedType() {
        return filterTypes.get(outputType);
    }
    
    // ensure unique attribute (the most similar one) in result
    // assume that list is already sorted
    // returns a map (keyed by attribute id) each element being a list of retrieved
    // tuples (attrib, score).
    Map<String, List<SearchResult>> groupByAttribs(List<SearchResult> topK) {
        Map<String, List<SearchResult>> resMapGroupedByAttrib = new HashMap<>();
        for (SearchResult sr: topK) {
            String attribId = sr.node.getId();
            List<SearchResult> seenList = resMapGroupedByAttrib.get(attribId);
            if (seenList == null) {
                seenList = new ArrayList<>();
                resMapGroupedByAttrib.put(attribId, seenList);
            }
            seenList.add(sr);
        }
        return resMapGroupedByAttrib;
    }
    
    String resTuple(List<SearchResult> results) {
        StringBuffer buff = new StringBuffer("Results: ");
        for (SearchResult res : results) {
            buff.append(res).append("#");
        }
        if (buff.length() > 0)
            buff.deleteCharAt(buff.length()-1);

        return buff.toString();
    }
    
    /**
     * Takes as input node vector objects, executes queries and prints out
     * aggregated evaluation metrics.
     * @param nodeVecs
     */
    public Map<String, Double> predictAndEvaluate(NodeVecs nodeVecs) {
        int topK = Integer.parseInt(prop.getProperty("prediction.numwanted", "1"));

        List<Query> testQueries = queryNodes.buildQueries();

        int numQueries = testQueries.size();
        float sim = 0;
        int nQueriesEvaluated = 0;
        
        Map<String, int[]> counter = new HashMap<>();
        List<PredictionTuple> rtuples = new ArrayList<>();
        
        for (int qid=0; qid < numQueries; qid++) {
            Query testQuery = testQueries.get(qid);
            
            System.out.println("Test Query: " + testQuery);

            AndQuery testAndQuery = (AndQuery)testQueries.get(qid);

            if (Boolean.parseBoolean(prop.getProperty("prediction.flattenquery"))) {
                // first flatten the query
                testAndQuery = AndQuery.flatten(testAndQuery);
            }
            // filter the target of the query
            testAndQuery = testAndQuery.withFilter(avp ->
                    avp.getAttribute().getId().equals(predictedOutcomeValueAttributeId) // make sure we target the correct outcome value
                            && avp.getNumericValue() != null // we don't care about non-numerical OV
                            && avp.getNumericValue() <= 100.0); // remove noisy values outside of 0-100%
            //testAndQuery = new AndQuerySimple(testAndQuery);
            System.out.println("Test Query: " + testAndQuery);
            // run the query
            List<SearchResult> results = getResults(testAndQuery, nodeVecs, topK);

            //testQuery = testQuery.filteredWith(result -> result.node.getAttribute().getType() == wantedType());  // set the filter
            
            Map<String, List<SearchResult>> groupedResults = groupByAttribs(results);            
            groupedResults = collapseToWtAvg(groupedResults);

            for (List<SearchResult> perAttribList: groupedResults.values()) {
                System.out.println(resTuple(perAttribList));
                for (SearchResult sr : perAttribList) {
                    String stStr = sr.toString();
                    int[] count = counter.get(stStr);
                    if (count == null) {
                        count = new int[1];
                        counter.put(stStr, count);
                    }
                    count[0]++;
                }
                
                // float this_sim = evaluationLogic.evaluate(qid, perAttribList);
                PredictionTuple rtuple = evaluationLogic.evaluate(qid, perAttribList.get(0));
                if (!rtuple.valid()) {
                    LOGGER.warn("Reference or prediction is not valid (less than 0): {}  -- {}", rtuple.getRef(), rtuple.getPred());
                    continue;
                }
                System.out.println("Reference:\t" + rtuple.getRef() + "\tPrediction\t" + rtuple.getPred());

                rtuples.add(rtuple);
                //sim += this_sim;
            }
            nQueriesEvaluated++;
        }
        
        /*
        if (nQueriesEvaluated > 0)
        System.out.println("Mean-average " + evaluationLogic.getMetricName() + ": " + Math.sqrt(sim/(float)nQueriesEvaluated));
        */
        try {            
            // Print out the result tuples in a file
            FileWriter fw = new FileWriter("res.tsv");
            BufferedWriter bw = new BufferedWriter(fw);
            
            for (PredictionTuple rt: rtuples) {
                bw.write(rt.getRef() + "\t" + rt.getPred());
                bw.newLine();
            }
            bw.close();
            fw.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
        
        
        Map<String, Double> res = new HashMap<>();
        for (EvaluationMetric metric : evaluationMetrics) {
            String metricName = metric.toString();
            double evaluationResult = metric.compute(rtuples);
            System.out.println(metricName + ": " + evaluationResult);
            res.put(metricName, evaluationResult);
        }
        
        /*
        // print distribution of all results
        for (Entry<String, int[]> entry : counter.entrySet()) {
            System.out.println(entry.getKey() + "\t" + entry.getValue()[0]);
        }
        */

        return res;
    }

    protected List<SearchResult> getResults(AndQuery query, NodeVecs vectors, int topK) {
        return query.searchTopK(vectors, topK);
    }

    Map<String, List<SearchResult>> collapseToWtAvg(Map<String, List<SearchResult>> resMap) {
        Map<String, List<SearchResult>> collapsedMap = new HashMap<>();
        
        for (List<SearchResult> perAttribList: resMap.values()) {
            collapsedMap.put(perAttribList.get(0).node.getId(), collapseToWtAvg(perAttribList));
        }
        
        return collapsedMap;
    }
    
    List<SearchResult> collapseToWtAvg(List<SearchResult> list) {
        boolean collapseToWeightedAvg = Boolean.parseBoolean(prop.getProperty("predictor.collapsetowtavg", "false"));
        if (collapseToWeightedAvg == false)
            return list.subList(0, 1);  // only the top one

        // Else an weighted average of values
        float z = 0;
        for (SearchResult res: list) {
            z += res.score;
        }

        float wtAvg = 0, v;
        for (SearchResult res: list) {
            if (res.node==null || res.node.getNumericValue()==null)
                continue;
            v = res.node.getNumericValue().floatValue();
            wtAvg += (float)(v * res.score/z);
        }

        if (list.size() > 0)
            wtAvg /= list.size();

        AttributeValueNode collapsedNode = new AttributeValueNode(
                new AttributeValuePair(list.get(0).node.getAttribute(), String.valueOf(wtAvg)));
        SearchResult collapsedSearchRes = new SearchResult(collapsedNode, list.get(0).score);
        List<SearchResult> collapsedList = new ArrayList<>();
        collapsedList.add(collapsedSearchRes);
        return collapsedList;
    }

    public static List<SearchResult> collapseToWeightedAverage(List<SearchResult> list) {
        if (list.isEmpty()) return list;
        // filter out the values that we can't convert to something numeric
        List<SearchResult> numericResults = list.stream()
                .filter(sr -> sr.getNode() != null && sr.getNode().isNumeric()) //marting: I don't see why the node would be null here
                .collect(Collectors.toList());
        // norm
        double scoreSum = numericResults.stream().map(SearchResult::getScore).reduce(0.0, Double::sum);
        if (scoreSum == 0.0) return list; // means every score was 0.0
        // weighted sum
        double weightedSum = numericResults.stream()
                .map(sr -> sr.getNode().getNumericValue() * sr.getScore())
                .reduce(0.0, Double::sum);
        double weightedAverage = weightedSum / scoreSum;

        SearchResult firstResult = list.get(0);
        AttributeValueNode collapsedNode = new AttributeValueNode(
                new AttributeValuePair(firstResult.getNode().getAttribute(), String.valueOf(weightedAverage)));
        SearchResult collapsedSearchRes = new SearchResult(collapsedNode, firstResult.getScore(), firstResult.getFollowUp());
        return Lists.newArrayList(collapsedSearchRes);
    }
}
