package com.ibm.drl.hbcp.core.attributes.normalization;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;

/**
 * An attribute-value pair with a normalized value (i.e. a new value replacing the original one).
 * @author marting
 */
public class NormalizedAttributeValuePair extends ArmifiedAttributeValuePair {
    // this is where you store the original object (so that if it contains extra information, you can get it back)
    protected final ArmifiedAttributeValuePair original;
    protected final String normalizedValue;

    public NormalizedAttributeValuePair(ArmifiedAttributeValuePair pair, String normalizedValue) {
        // note here the "normalizedValue" passed as value in the super constructor
        super(pair.getAttribute(), normalizedValue, pair.getDocNames().iterator().next(), pair.getArm());
        original = pair;
        this.normalizedValue = normalizedValue;
    }

    public final String getNormalizedValue() { return normalizedValue; }

    public final ArmifiedAttributeValuePair getOriginal() { return original; }
}
