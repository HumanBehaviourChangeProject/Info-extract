package com.ibm.drl.hbcp.predictor.nodes.normalization;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.normalizers.NumericNormalizer;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class NumericNormalizerTest {

    public final static String[] originalValues = {
            "10",
            "10.5",
            "10.5 (3.4)",
            "10 (3.4)",
            "hahahaha", // this is not numeric, should be returned as is
            "01",
            "00001",
            "01.01",
            "0.1.1" // looks more like a version number, but is can still be handled by the normalizer
    };

    public final static String[] normalizedValues = {
            "10",
            "10.5",
            "10.5",
            "10",
            "hahahaha",
            "1",
            "1",
            "1.01",
            "0.1"
    };

    @Test
    public void checkNormalization() {
        // check that all original values have a corresponding expected normalized value in the test
        assertEquals(originalValues.length, normalizedValues.length);
        // normalizes each value and checks the expected normalization
        NumericNormalizer normalizer = new NumericNormalizer();
        for (int i = 0; i < originalValues.length; i++) {
            assertEquals(normalizedValues[i], normalizer.getNormalizedValue(avp(originalValues[i])));
        }
    }

    private static AttributeValuePair avp(String value) {
        // we don't need any attribute for this normalizer
        return new AttributeValuePair(null, value);
    }
}
