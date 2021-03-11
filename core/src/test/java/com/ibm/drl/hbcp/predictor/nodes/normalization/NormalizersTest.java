package com.ibm.drl.hbcp.predictor.nodes.normalization;

import com.ibm.drl.hbcp.core.attributes.*;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.Properties;

import static org.junit.Assert.assertEquals;

/**
 * Test the static Normalizers class that delegates the normalization operation to all the correct Normalizer classes
 */
public class NormalizersTest {

    // fake attributes to normalize (their index in this table is also their ID string)
    private static final Attribute[] attributes = {
            new Attribute("0", AttributeType.POPULATION, "A numerical attribute"),
            new Attribute("1", AttributeType.OUTCOME_VALUE, "Another numerical attribute"),
            new Attribute("2", AttributeType.INTERVENTION, "A binary 'presence' attribute"),
            new Attribute("3", AttributeType.OUTCOME, "A followup attribute (week month)"),
            new Attribute("4", AttributeType.POPULATION, "A mixed-gender attribute"),
            new Attribute("5", AttributeType.OUTCOME, "A categorical attribute")
    };

    private static Properties props;

    @BeforeClass
    public static void buildProperties() {
        props = new Properties();
        // we assume the parsing of the property is correctly done, and put just one attribute in each property
        props.put("prediction.attribtype.numerical", "0");
        // we don't need to put attribute 1 in "prediction.attribtype.numerical", Outcome_values should by default be numeric
        // we don't need to put attribute 2 in "prediction.attribtype.annotation.presence", Interventions should by default be binary
        props.put("prediction.attribtype.week_month", "3");
        props.put("prediction.attribtype.mixed.gender", "4");
        props.put("prediction.categories.5", "smoker,speaker,none");
    }

    private static final ArmifiedAttributeValuePair[] avpToNormalize = {
            armified(new AttributeValuePair(attributes[0], "10.6 (st: 3.1)")),
            armified(new AttributeValuePair(attributes[1], "014.3% of people stopped smoking")),
            armified(new AttributeValuePair(attributes[2], "I can write anything I want here")),
            new ArmifiedAttributeValuePair(attributes[3], "2.3 weeks", "", "", "follow-up of 2.3 weeks"),
            armified(new AttributeValuePair(attributes[4], "Male46.2Female53.8")),
            armified(new AttributeValuePair(attributes[5], "4.3 smoking"))
    };

    private static final String[] expectedNormalizedValues = {
            "10.6",
            "14.3",
            "1",
            "2.3", // this is a number of weeks
            "M (46.2) F (53.8)",
            "smoker"
    };

    @Test
    public void testNormalization() {
        Normalizers normalizers = new Normalizers(props);
        // first check that the test is well built, with 1 expected value per test case
        assertEquals(avpToNormalize.length, expectedNormalizedValues.length);
        // then run and check each normalization case
        for (int i = 0; i < avpToNormalize.length; i++) {
            NormalizedAttributeValuePair navp = normalizers.normalize(avpToNormalize[i]);
            assertEquals(expectedNormalizedValues[i], navp.getNormalizedValue());
            assertEquals(expectedNormalizedValues[i], navp.getValue());
        }
    }

    /* Returns a dummy ArmifiedAVP from a regular AVP, we don't need a valid doc or arm in these tests */
    private static ArmifiedAttributeValuePair armified(AttributeValuePair avp) {
        return new ArmifiedAttributeValuePair(avp.getAttribute(), avp.getValue(), "", "");
    }
}
