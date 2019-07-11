package com.ibm.drl.hbcp.predictor.queries;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A simple query that asks for the closest existing node of the same type/id.
 * @author marting
 */
public class NodeQuery implements Query {
    public final AttributeValueNode node;
    protected final Double numericValue;

    public NodeQuery(AttributeValueNode node) {
        this.node = node;
        numericValue = NodeVecs.getNumericValue(node.toString());
    }

    @Override
    public List<SearchResult> search(NodeVecs vecs) {
        // they're already sorted, btw
        List<String> nodeIds = vecs.getClosestAttributeInstances(node);
        return nodeIds.stream()
                .map(AttributeValueNode::parse)
                .map(nodeId -> new SearchResult(nodeId, getSimilarityScore(nodeId)))
                .collect(Collectors.toList());
    }

    protected double getSimilarityScore(AttributeValueNode other) {
        if (numericValue == null) {
            // it means the node we're looking for isn't numeric,
            // so any result returned by getClosestAttributeInstances is exact
            return 1.0;
        }
        // at this point any result returned by getClosestAttributeInstances is always numeric
        Double otherValue = NodeVecs.getNumericValue(other.toString());
        return 1 - Math.abs(numericValue - otherValue);
    }

    @Override
    public String toString() {
        return "Node(" + node + ")";
    }
}
