package com.ibm.drl.hbcp.predictor.queries;

import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.text.similarity.LevenshteinDistance;

import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;

/**
 * A simple query that asks for the closest existing node of the same type/id.
 * @author marting
 */
public class NodeQuery implements Query {
    public final AttributeValueNode node;
    protected final Double numericValue;

    // unlimited compare (see if we need to set a threshold performance wise)
    private static final LevenshteinDistance textDistance = new LevenshteinDistance();

    public NodeQuery(AttributeValueNode node) {
        this.node = node;
        numericValue = NodeVecs.getNumericValue(node.toString());
    }

    @Override
    public List<SearchResult> search(NodeVecs vecs) {
        List<String> nodeIds = vecs.getClosestAttributeInstances(node);
        return nodeIds.stream()
                .map(AttributeValueNode::parse)
                .map(nodeId -> new SearchResult(nodeId, getSimilarityScore(nodeId, vecs)))
                .sorted(Comparator.reverseOrder())
                .collect(Collectors.toList());
    }

    protected double getSimilarityScore(AttributeValueNode other, NodeVecs vecs) {
        if (numericValue == null) {
            // NodeVecs can actually return text nodes that are completely different from the query text, so we need
            // to measure how different
            //return 1.0;
            return getSimilarityScoreText(node.getValue(), other.getValue());
        }
        // at this point any result returned by getClosestAttributeInstances is always numeric
        Double otherValue = NodeVecs.getNumericValue(other.toString());
        if (otherValue == null) {
            // TODO: this shouldn't happen, but it does. It be like that sometimes
            return 0.0;
        }
        if (numericValue.equals(otherValue)) {
            // this gets rid of the case where both are 0;
            return 1.0;
        }
        //double relativeDifferenceScore = Math.min(numericValue, otherValue) / Math.max(numericValue, otherValue);
        double normalizedDifferenceScore = 1.0 - Math.abs(numericValue - otherValue) / vecs.getMaxValue(node.getAttribute());
        return normalizedDifferenceScore;
    }

    private double getSimilarityScoreText(String resultText, String queryText) {
        // a distance in the integers: [0, +nfnty]
        int editDistance = textDistance.apply(resultText, queryText);
        // we turn it into a soft similarity score between [0; 1]
        double res = Math.exp(- (double)editDistance / (resultText.length() + queryText.length() + 1));
        return res;
    }

    private double getSimilarityScoreTextContains(String resultText, String queryText) {
        resultText = resultText.trim().toLowerCase();
        queryText = queryText.trim().toLowerCase();
        if (resultText.equals(queryText)) {
            return 1.0;
        } else if (resultText.contains(queryText) || queryText.contains(resultText)) {
            return 0.5;
        } else {
            return 0.5;
        }
    }

    @Override
    public String toString() {
        return "Node(" + node + ")";
    }
}
