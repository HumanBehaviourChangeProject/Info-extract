package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

/**
 * A succession of two normalizers applied one after the other.
 * @author marting
 * @param <AVPair> An attribute-value pair type
 */
public class ChainNormalizer<AVPair extends AttributeValuePair> implements Normalizer<AVPair> {

    private final Normalizer<AVPair> normalizer1;
    private final Normalizer<AttributeValuePair> normalizer2;

    public ChainNormalizer(Normalizer<AVPair> normalizer1, Normalizer<AttributeValuePair> normalizer2) {
        this.normalizer1 = normalizer1;
        this.normalizer2 = normalizer2;
    }

    @Override
    public String getNormalizedValue(AVPair pair) {
        return normalizer2.getNormalizedValue(new AttributeValuePair(pair.getAttribute(), normalizer1.getNormalizedValue(pair)));
    }
}
