package com.ibm.drl.hbcp.predictor.queries;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;

import java.util.List;

/**
 * Asks for comparable nodes to the provided node in constructor,
 * with the constraint that their numeric value should satisfy a predicate (to be provided by implementations).
 *
 * @author marting
 */
public abstract class NodeNumericConditionQuery extends NodeQuery {

    public NodeNumericConditionQuery(AttributeValueNode node) throws NumberFormatException {
        super(node);
        if (!node.isNumeric()) throw new NumberFormatException("Expected numeric node in query: " + node);
    }

    public boolean isValidMatchingNode(AttributeValueNode candidateNode) {
        if (candidateNode.getAttribute().getType() == this.node.getAttribute().getType() && candidateNode.getId().equals(this.node.getId())) {
            if (candidateNode.isNumeric()) {
                return isValidNumericValue(candidateNode.getNumericValue(), this.node.getNumericValue());
            }
        }
        return false;
    }

    @Override
    public List<SearchResult> search(NodeVecs vecs) {
        return Lists.newArrayList(super.filteredWith(sr -> isValidMatchingNode(sr.node)).search(vecs));
    }

    public abstract boolean isValidNumericValue(double candidateNodeValue, double queryNodeValue);
}
