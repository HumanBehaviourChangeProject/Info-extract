package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.ValueType;

public class F1BctAttributes extends F1Base {

    @Override
    protected String getNormalForm(ArmifiedAttributeValuePair value) {
        return "1";
    }

    @Override
    public boolean isRelevantAttribute(Attribute attribute) {
        return ValueType.isPresenceType(attribute);
    }
}
