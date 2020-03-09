package com.ibm.drl.hbcp.core.wvec;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;

import java.util.Properties;

/**
 * A metric between values of two NodeIds, specifically from the mixed-gender-like attributes.
 * For categorical attributes, no relevant metric is defined (values are either equal or different).
 *
 * @author dganguly
 */
public class NodeMetric {
    Properties prop;
    String mixedGenderAttribId;

    public NodeMetric(Properties prop) {
        this.prop = prop;
        mixedGenderAttribId = prop.getProperty("prediction.attribtype.mixed.gender");
    }

    boolean isCategorical(AttributeValueNode x) {
        String val = prop.getProperty("prediction.categories." + x.getAttribute().getId());
        return (val != null);
    }

    float getDistance(AttributeValueNode query, String value) {
        if (isCategorical(query)) {
            return query.getValue().equals(value)? 0 : Float.MAX_VALUE;
        }
        else if (query.getAttribute().getId().equals(mixedGenderAttribId)) {
            MixedGenderAttribute mg_a = new MixedGenderAttribute(query.getValue());
            MixedGenderAttribute mg_b = new MixedGenderAttribute(value);
            return mg_a.getDistance(mg_b);
        }
        return 0;
    }
}
