package com.ibm.drl.hbcp.api;

import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.parser.Attributes;

public class RelevantPredictionQueryAttributesTest {

    private static final String BASIC_ATTRIBUTE_NAME = "Mean age";
    private static Attribute basicAttribute;

    @BeforeClass
    public static void fetchBasicAttribute() {
        basicAttribute = Attributes.get().getFromName(BASIC_ATTRIBUTE_NAME);
    }

    @Test
    public void testFetchBasicAttributeSuccess() {
        Assert.assertNotNull(basicAttribute);
    }

    @Test
    public void testBasicAttributePresence() {
        Assert.assertTrue("The repository contains at least the '" + BASIC_ATTRIBUTE_NAME + "' attribute",
                RelevantPredictionQueryAttributes.get().contains(basicAttribute));
    }

    @Test
    public void testTypeFiltering() {
        Assert.assertFalse(RelevantPredictionQueryAttributes.getForType(basicAttribute.getType()).isEmpty());
    }
}
