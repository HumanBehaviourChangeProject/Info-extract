package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import lombok.Data;
import org.junit.Assert;
import org.junit.Test;

import java.util.List;

import static com.ibm.drl.hbcp.core.attributes.normalization.normalizers.TimepointNormalizer.WEEKS_IN_A_MONTH;

public class TimepointNormalizerTest {

    private static final List<ValueContextTest> testInstances = Lists.newArrayList(
            new ValueContextTest("6", "", String.valueOf(6 * WEEKS_IN_A_MONTH)),
            new ValueContextTest("52", "", "52.0"),
            new ValueContextTest("6", "weeks", "6.0"),
            new ValueContextTest("6", "6 weeks", "6.0"),
            new ValueContextTest("6", "6 weeks and 3 months", "6.0"),
            new ValueContextTest("6", "3 weeks and 6 months", String.valueOf(6 * WEEKS_IN_A_MONTH)),
            new ValueContextTest("6 months", "weeks", String.valueOf(6 * WEEKS_IN_A_MONTH)),
            new ValueContextTest("6", "6 weeks and 6 months", String.valueOf(6 * WEEKS_IN_A_MONTH))
    );

    private final TimepointNormalizer normalizer = new TimepointNormalizer();

    @Test
    public void testTimepointNormalization() {
        for (ValueContextTest testInstance : testInstances) {
            ArmifiedAttributeValuePair aavp = new ArmifiedAttributeValuePair(null, testInstance.getValue(), null, (Arm)null, testInstance.getContext());
            String normalizedValue = normalizer.getNormalizedValue(aavp);
            Assert.assertEquals(testInstance.toString(), testInstance.expectedNormalizedValue, normalizedValue);
        }
    }

    @Data
    private static class ValueContextTest {
        private final String value;
        private final String context;
        private final String expectedNormalizedValue;
    }
}
