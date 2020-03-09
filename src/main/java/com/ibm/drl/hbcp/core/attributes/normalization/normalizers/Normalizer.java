package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

/**
 * Transforms the value of an attribute into another value,
 * "normalized" with the assumption that further processing is facilitated in some way.
 * @author marting
 * @param <AVPair> The attribute-value pair type handled by the normalizer
 */
public interface Normalizer<AVPair extends AttributeValuePair> {

    /**
     * Normalize the original value of an attribute extracted from a document
     * @param pair The attribute-value pair as originally extracted from the document
     * @return The normalized value
     */
    String getNormalizedValue(AVPair pair);

    default Normalizer<AVPair> then(Normalizer<AttributeValuePair> normalizer) {
        return new ChainNormalizer<>(this, normalizer);
    }
}
