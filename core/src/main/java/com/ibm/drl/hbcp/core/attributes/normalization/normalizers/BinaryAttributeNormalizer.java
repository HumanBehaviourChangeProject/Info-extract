package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

/**
 * Turns any value of a binary attribute (presence/absence) into a 1. If it exists, it's 1.
 * @author marting
 */
public class BinaryAttributeNormalizer implements Normalizer<AttributeValuePair> {
    @Override
    public String getNormalizedValue(AttributeValuePair pair) {
        return "1";
    }
}
