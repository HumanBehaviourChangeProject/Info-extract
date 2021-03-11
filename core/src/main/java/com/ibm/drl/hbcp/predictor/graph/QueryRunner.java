package com.ibm.drl.hbcp.predictor.graph;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.DataInstance;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.util.ParsingUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Runs the graph-based NN query method on provided test sets and provides prediction that can then be evaluated.
 *
 * @author mgleize
 * */
public class QueryRunner {

    /** the output of a Node2Vec call on a graph */
    private final NodeVecs nodeVectors;
    private final int topK;
    private final boolean flattenQuery;

    public QueryRunner(NodeVecs nodeVectors, int topK, boolean flattenQuery) {
        this.nodeVectors = nodeVectors;
        this.topK = topK;
        this.flattenQuery = flattenQuery;
    }
    public QueryRunner(NodeVecs nodeVectors) { this(nodeVectors, 10, true); }

    /** Returns predictions made by the NN method on the specified test instances */
    public List<PredictionTuple> predict(List<DataInstance> testInstances) {
        return testInstances.stream()
                .map(this::predict)
                .collect(Collectors.toList());
    }

    private PredictionTuple predict(DataInstance testInstance) {
        AndQuery query = buildQuery(testInstance, flattenQuery);
        List<SearchResult> results = getResults(query, nodeVectors, topK);
        results = collapseToWeightedAverage(results);
        // it's possible that the query returns no result
        if (results.isEmpty()) {
            // TODO: handle that better
            System.err.println("Query didn't yield any result (likely due to text value not found in the train instances) so 0.0 will be predicted by default!");
            return new PredictionTuple(testInstance.getYNumeric(), 0.0);
        } else {
            double topPrediction = 5; // default prediction
            try {
                // at this point the top SearchResult exists and has for sure a numeric value
                topPrediction = ParsingUtils.parseFirstDouble(results.get(0).getNode().getValue());
            }
            catch (Exception ex) { }
            return new PredictionTuple(testInstance.getYNumeric(), topPrediction);
        }
    }

    private AndQuery buildQuery(DataInstance dataInstance, boolean flattenQuery) {
        AndQuery testAndQuery = new AndQuery(oneQueryPerType(dataInstance));
        if (flattenQuery) {
            // first flatten the query
            testAndQuery = AndQuery.flatten(testAndQuery);
        }
        // filter the target of the query
        testAndQuery = testAndQuery.withFilter(avp ->
                avp.getAttribute().isOutcomeValue() // make sure we target the correct outcome value
                        && avp.getNumericValue() != null // we don't care about non-numerical OV
                        && avp.getNumericValue() <= 100.0); // remove noisy values outside of 0-100%
        return testAndQuery;
    }

    private List<AndQuery> oneQueryPerType(DataInstance dataInstance) {
        // remove anything in the instance that we haven't seen in training
        List<ArmifiedAttributeValuePair> filteredInput = dataInstance.getX().stream()
                .filter(avp -> nodeVectors.getAttributeIds().contains(avp.getAttribute().getId()))
                .collect(Collectors.toList());
        // split the values by attribute type
        Map<AttributeType, List<ArmifiedAttributeValuePair>> avpsPerType = Maps.toMap(
                filteredInput.stream()
                    .map(avp -> avp.getAttribute().getType())
                    .collect(Collectors.toList()),
                type -> dataInstance.getX().stream()
                    .filter(avp -> avp.getAttribute().getType() == type)
                    .collect(Collectors.toList()));
        // build an AndQuery for each
        return avpsPerType.values().stream()
                .map(avps -> new AndQuery(avps.stream()
                        .map(avp -> getSuccessfulNodeQuery(avp))
                        .filter(successfulQuery -> successfulQuery.isPresent())
                        .map(option -> option.get())
                        .collect(Collectors.toList())))
                .collect(Collectors.toList());
    }

    /**
     * Builds an individual NodeQuery for one value of the instance, returns it after checking
     * that it yields results (finds a match in the train vectors), otherwise returns nothing */
    private Optional<NodeQuery> getSuccessfulNodeQuery(ArmifiedAttributeValuePair avp) {
        NodeQuery query = new NodeQuery(new AttributeValueNode(avp));
        // we're going to peek at what the AndQuery will see, if it's empty we don't include this constraint
        List<SearchResult> results = query.searchTopK(nodeVectors, 1);
        return results.isEmpty() ? Optional.empty() : Optional.of(query);
    }

    private List<SearchResult> getResults(AndQuery query, NodeVecs vectors, int topK) {
        return query.searchTopK(vectors, topK);
    }

    private static List<SearchResult> collapseToWeightedAverage(List<SearchResult> list) {
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
        SearchResult collapsedSearchRes = new SearchResult(collapsedNode, firstResult.getScore());
        return Lists.newArrayList(collapsedSearchRes);
    }
}
