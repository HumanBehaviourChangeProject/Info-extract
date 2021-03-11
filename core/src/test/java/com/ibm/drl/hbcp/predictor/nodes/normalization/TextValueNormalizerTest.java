package com.ibm.drl.hbcp.predictor.nodes.normalization;

import com.ibm.drl.hbcp.core.attributes.*;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Test the categorical normalization.
 * A fairly big limitation of the current implementation of this normalizer is that BOTH values and categories have to be
 * bigger than length 3 for it to make sense. If they're shorter, they're considered as having no relevant n-grams, and by
 * default the first category will be selected (no matter the perceived textual similarities).
 *
 * @author marting
 */
public class TextValueNormalizerTest {

    // fake attributes to normalize
    // their index in this table is also their ID string
    // their name is the values they can take
    private static final Attribute[] attributes = {
            new Attribute("0", AttributeType.POPULATION, "1,0"), // will normalize to 1 (because too short)
            new Attribute("1", AttributeType.POPULATION, "smoker,speaker,none"), // standard
            new Attribute("2", AttributeType.POPULATION, "smoker,smoking,smok") // difficult
    };

    private static Properties props;

    @BeforeClass
    public static void buildProperties() {
        props = new Properties();
        for (int i = 0; i < attributes.length; i++) {
            props.put("prediction.categories." + i, attributes[i].getName());
        }
    }

    private static final AttributeValuePair[] avpToNormalize = {
            new AttributeValuePair(attributes[0], "1"),
            new AttributeValuePair(attributes[0], "0"),
            new AttributeValuePair(attributes[0], "110"),
            new AttributeValuePair(attributes[0], "11000"),
            new AttributeValuePair(attributes[1], "4.3 smoking"),
            new AttributeValuePair(attributes[1], "no"),
            new AttributeValuePair(attributes[1], "no fire without smoke"),
            new AttributeValuePair(attributes[1], "Speaker"),
            new AttributeValuePair(attributes[2], "smok"),
            new AttributeValuePair(attributes[2], "smoksmoksmok"),
            new AttributeValuePair(attributes[2], "4.3 smoking"),
            new AttributeValuePair(attributes[2], "4.3 smokeing"),
    };

    private static final String[] expectedNormalizedValues = {
            "1",
            "1",
            "1",
            "1",
            "smoker",
            "smoker", // should logically be "none", but "no" doesn't have any 3-gram so the first category is selected by default
            "smoker",
            "speaker",
            "smok",
            "smok",
            "smoking",
            "smoker"
    };

    @Test
    public void testNormalization() {
        Normalizers normalizers = new Normalizers(props);
        // first check that the test is well built, with 1 expected value per test case
        assertEquals(avpToNormalize.length, expectedNormalizedValues.length);
        // then run and check each normalization case
        for (int i = 0; i < avpToNormalize.length; i++) {
            NormalizedAttributeValuePair navp = normalizers.normalize(armified(avpToNormalize[i]));
            assertEquals(expectedNormalizedValues[i], navp.getNormalizedValue());
            assertEquals(expectedNormalizedValues[i], navp.getValue());
        }
    }

    /* Returns a dummy ArmifiedAVP from a regular AVP, we don't need a valid doc or arm in these tests */
    private static ArmifiedAttributeValuePair armified(AttributeValuePair avp) {
        return new ArmifiedAttributeValuePair(avp.getAttribute(), avp.getValue(), "", "");
    }
}