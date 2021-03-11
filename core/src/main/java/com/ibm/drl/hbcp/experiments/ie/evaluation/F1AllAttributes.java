package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.ValueType;

public class F1AllAttributes extends F1Base {

    @Override
    protected String getNormalForm(ArmifiedAttributeValuePair value) {
        if (isBinaryAttribute(value.getAttribute())) {
            return "1";
        } else {
            return value.getValue();
        }
    }

    private boolean isBinaryAttribute(Attribute attribute) {
        return ValueType.isPresenceType(attribute) || attribute.getType() == AttributeType.INTERVENTION;
    }

    @Override
    public boolean isRelevantAttribute(Attribute attribute) {
        return ValueType.isPrioritizedEntity(attribute);
    }
}
