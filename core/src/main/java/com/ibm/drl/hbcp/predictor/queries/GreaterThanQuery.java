package com.ibm.drl.hbcp.predictor.queries;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;

/**
 * Asks for comparable nodes to the provided node in constructor,
 * with the constraint that nodes should have a greater (or equal) attribute value than the constructor parameter.
 *
 * @author marting
 */
public class GreaterThanQuery extends NodeNumericConditionQuery {

    public GreaterThanQuery(AttributeValueNode node) throws NumberFormatException {
        super(node);
    }

    @Override
    public boolean isValidNumericValue(double candidateNodeValue, double queryNodeValue) {
        return candidateNodeValue >= queryNodeValue;
    }
}
