package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.ValueType;

public class F1ComplexAttributes extends F1Base {

    @Override
    public boolean isRelevantAttribute(Attribute attribute) {
        return ValueType.isComplexType(attribute);
    }
}
