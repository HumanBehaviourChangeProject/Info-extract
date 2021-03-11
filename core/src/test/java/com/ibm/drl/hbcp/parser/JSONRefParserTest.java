package com.ibm.drl.hbcp.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * Unit tests for the new JSON parser, using a small dummy JSON with very few attributes and references.
 * Also tests the armification.
 *
 * @author marting
 */
public class JSONRefParserTest {

    private static final String smallJsonPath = "data/test/jsons/armified_testfile.json";

    private static JSONRefParser parser;

    private static final Set<String> EXPECTED_ATTRIBUTE_IDS = Sets.newHashSet(
        "1", "11", "111", "112", "113", "12", "121", "122", // some age and gender attributes
            "3689776", "3673270", "3673271", "3673272",  // some BCT attributes
            "5140146", // output value attribute
            "5730447" // the arm name attribute
    );

    private static final String DOC1 = "Abdullah 2005.pdf";
    private static final String DOC2 = "Abrantes 2014.pdf";

    private List<Pair<String, Set<AttributeValuePair>>> DOC_AND_EXPECTED_NONARM_AVPS = Lists.newArrayList(
            Pair.of(DOC1, Sets.newHashSet(
                    new AttributeValuePair(new Attribute("113", AttributeType.POPULATION, "Maximum Age"), "37 years old"),
                    new AttributeValuePair(new Attribute("122", AttributeType.POPULATION, "All female"), "all female indeed"),
                    new AttributeValuePair(new Attribute("5140146", AttributeType.OUTCOME_VALUE, "Outcome value"), "15.3%"),  // arm 17
                    new AttributeValuePair(new Attribute("5140146", AttributeType.OUTCOME_VALUE, "Outcome value"), "7.4%"),   // arm 18
                    new AttributeValuePair(new Attribute("3673272", AttributeType.INTERVENTION, "1.2 Problem solving "), "tips on things that they can do to overcome withdrawal symptoms and difficult situations") // arm 17
            )),
            Pair.of(DOC2, Sets.newHashSet(
                    new AttributeValuePair(new Attribute("111", AttributeType.POPULATION, "Mean Age"), "47.3 years"),
                    new AttributeValuePair(new Attribute("121", AttributeType.POPULATION, "All male"), "but still mostly male"), // arm 3
                    new AttributeValuePair(new Attribute("121", AttributeType.POPULATION, "All male"), "And way too manly"),  // arm 4
                    new AttributeValuePair(new Attribute("5140146", AttributeType.OUTCOME_VALUE, "Outcome value"), "26.7%"),  // arm 3
                    new AttributeValuePair(new Attribute("5140146", AttributeType.OUTCOME_VALUE, "Outcome value"), "12.9%"),  // arm 4
                    new AttributeValuePair(new Attribute("3673271", AttributeType.INTERVENTION, "1.1.Goal setting (behavior)"), "set a new quit date"),
                    new AttributeValuePair(new Attribute("3673272", AttributeType.INTERVENTION, "1.2 Problem solving "), "identifying high-risk situations and developing behavioral and cognitive strategies for coping w")
            ))
    );

    // of attribute of id 121
    private List<Pair<Arm, String>> ARM_AND_VALUE_IN_DOC2 = Lists.newArrayList(
            Pair.of(new Arm("3", "Aerobic Exercise - AE"), "but still mostly male"),
            Pair.of(new Arm("4", "Health education control - HEC"), "And way too manly")
    );

    // Arm names for arm 3 "Aerobic Exercise - AE " in Doc 2
    private List<String> ARM_NAMES_IN_DOC2 = Lists.newArrayList(
            "aerobic exercise (AE)",
            "an AE intervention", "The intervention", "the AE group", "(a) a 12-session, group AE intervention,", "the AE condition",
            "AE"
    );

    @BeforeClass
    public static void setUpClass() throws IOException {
        // constructor performs the actual parsing, throws IOException or other RuntimeExceptions if it fails
        parser = new JSONRefParser(new File(smallJsonPath));
    }

    @Test
    public void testCodeSetTypes() {
        Attributes attributes = parser.getAttributes();
        // only "Population", "Intervention" (BCTs), "Outcome value", and "Arms"
        assertEquals(4, attributes.groupedByType().keySet().size());
        assertTrue(attributes.groupedByType().containsKey(AttributeType.POPULATION));
        assertTrue(attributes.groupedByType().containsKey(AttributeType.ARM));
        assertTrue(attributes.groupedByType().containsKey(AttributeType.INTERVENTION));
        assertTrue(attributes.groupedByType().containsKey(AttributeType.OUTCOME_VALUE));
    }

    @Test
    public void testCodeSetAttributes() {
        Attributes attributes = parser.getAttributes();
        Set<String> allAttributeIds = attributes.groupedByType().values()
                .stream().flatMap(set -> set.stream().map(Attribute::getId)).collect(Collectors.toSet());
        assertEquals(EXPECTED_ATTRIBUTE_IDS, allAttributeIds);
    }

    @Test
    public void testNumberOfReferences() {
        // there are only 2 documents in that small JSON
        assertEquals(2, parser.getAttributeValuePairs().byDoc().keySet().size());
    }

    @Test
    public void testDocuments() {
        // there are only 3 documents in that small JSON
        // BUT ONE IS EMPTY (OF ANNOTATIONS) AND NOT ACTUALLY ADDED
        assertEquals(2, parser.getAttributeValuePairs().byDoc().keySet().size());
        // check the instances
        assertTrue(parser.getAttributeValuePairs().byDoc().containsKey(DOC1));
        assertTrue(parser.getAttributeValuePairs().byDoc().containsKey(DOC2));
    }

    @Test
    public void testReferences() {
        // checks number of non-arm instances
        assertEquals(12,
                parser.getAttributeValuePairs().getAllPairs().stream()
                        .filter(avp -> avp.getAttribute().getType() != AttributeType.ARM)
                        .count());
        // checks non-arm attribute instances
        for (Pair<String, Set<AttributeValuePair>> docAndExpectedAvps : DOC_AND_EXPECTED_NONARM_AVPS) {
            for (AttributeValuePair expectedAvp : docAndExpectedAvps.getRight()) {
                checkAVPContains(expectedAvp, docAndExpectedAvps.getLeft(), parser.getAttributeValuePairs());
            }
        }
    }

    private void checkAVPContains(AttributeValuePair expected, String docname, AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        assertTrue(avps.byDoc().get(docname).stream().anyMatch(aavp -> equalsAVP(expected, aavp)));
    }

    private boolean equalsAVP(AttributeValuePair expected, AnnotatedAttributeValuePair actual) {
        return expected.getAttribute().equals(actual.getAttribute())
                && expected.getValue().equals(actual.getValue());
    }

    @Test
    public void testArms() {
        // checks number of arm declaration instances: 4 declared in the json, + 1 "whole-study" empty arm
        Set<Arm> allArms = parser.getArmsInfo().values().stream().flatMap(Set::stream).collect(Collectors.toSet());
        assertEquals(5, allArms.size());
        // checks the armified 121 attribute in Doc 2, there are 2 different 121 for 2 different arms
        Map<Arm, Multiset<AnnotatedAttributeValuePair>> armifiedAVPs = parser.getAttributeValuePairs().getArmifiedPairsInDoc(DOC2);
        for (Pair<Arm, String> armAndValue : ARM_AND_VALUE_IN_DOC2) {
            assertEquals(armAndValue.getRight(), armifiedAVPs.get(armAndValue.getLeft()).stream()
                    .filter(aavp -> aavp.getAttribute().getId().equals("121")).iterator().next().getValue());
        }
        // checks the arm names in Doc 2, the arm "Aerobic Exercise - AE " has 3 details (in the JSON), but a total of 7 mentions
        assertTrue(parser.getArmsInfo().get(DOC2).contains(new Arm("3")));
        for (Arm arm : parser.getArmsInfo().get(DOC2)) {
            if (arm.getId().equals("3")) {
                String expectedStandardName = "Aerobic Exercise - AE";
                assertEquals(expectedStandardName, arm.getStandardName());
                List<String> expectedNames = new ArrayList<>(ARM_NAMES_IN_DOC2);
                expectedNames.add(expectedStandardName);
                assertEquals(expectedNames, arm.getAllNames());
            }
        }
    }
}
