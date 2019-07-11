package com.ibm.drl.hbcp.core.attributes.collection;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.junit.Assert;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Checks that all the aggregation features of AttributeValueCollection work correctly.
 * Also checks at the same time the equals/hashcode implementation of ArmifiedAttributeValuePairs (and parent classes)
 *
 * @author marting
 */
public class AttributeValueCollectionTest {

    public static Attribute[] attributes = {
            new Attribute("1", AttributeType.POPULATION, ""),
            new Attribute("2", AttributeType.INTERVENTION, ""),
            new Attribute("3", AttributeType.OUTCOME, ""),
            new Attribute("4", AttributeType.OUTCOME_VALUE, "")
    };

    public static List<ArmifiedAttributeValuePair> originalPairs = Lists.newArrayList(
            // same doc checks
            new ArmifiedAttributeValuePair(attributes[0], "ValueOf1", "Document1", "A"),
            new ArmifiedAttributeValuePair(attributes[0], "AnotherValueOf1", "Document1", "A"),
            new ArmifiedAttributeValuePair(attributes[1], "ValueOf2", "Document1", "A"),
            new ArmifiedAttributeValuePair(attributes[1], "ValueOf2InOtherArm", "Document1", "B"),
            // with another doc
            new ArmifiedAttributeValuePair(attributes[0], "ValueOf1InOtherDoc", "Document2", "Some arm"),
            // null checks
            new ArmifiedAttributeValuePair(null, "ValueOfNull", "DocumentNullId", "ArmNullId"),
            new ArmifiedAttributeValuePair(attributes[2], null, "DocumentNullValue", "ArmNullValue"),
            new ArmifiedAttributeValuePair(attributes[2], "ValueOf3", (String) null, "ArmNullDoc"),
            new ArmifiedAttributeValuePair(attributes[2], "ValueOf3", "DocumentNullArm", null),
            // multiset check (pairs can be in the collection twice)
            new ArmifiedAttributeValuePair(attributes[3], "ValueOf4", "Document4", "A"),
            new ArmifiedAttributeValuePair(attributes[3], "ValueOf4", "Document4", "A")
    );

    private static AttributeValueCollection<ArmifiedAttributeValuePair> collection;

    @BeforeClass
    public static void createCollection() {
        List<ArmifiedAttributeValuePair> pairs = originalPairs.stream()
                // filter out null ids
                .filter(avp -> avp.getAttribute() != null)
                // filter out null docnames
                .filter(avp -> avp.getDocName() != null)
                // filter out null arms
                .filter(avp -> avp.getArm() != null)
                .collect(Collectors.toList());
        collection = new AttributeValueCollection<>(pairs);
    }

    @Test
    public void testEnforcesNonNullFields() {
        List<ArmifiedAttributeValuePair> pairs = originalPairs;
        try {
            new AttributeValueCollection<>(pairs);
            Assert.fail("A null ID was allowed");
        } catch (NullPointerException eId) {
            pairs = pairs.stream().filter(avp -> avp.getAttribute() != null).collect(Collectors.toList());
            try {
                new AttributeValueCollection<>(pairs);
                Assert.fail("A null docname was allowed");
            } catch (NullPointerException eDoc) {
                pairs = pairs.stream().filter(avp -> avp.getDocName() != null).collect(Collectors.toList());
                try {
                    new AttributeValueCollection<>(pairs);
                    Assert.fail("A null arm was allowed");
                } catch (NullPointerException eArm) {
                    // this is where the run is supposed to end up
                }
            }
        }
    }

    @Test
    public void testIndexingById() {
        performMultisetCheck(collection.getPairsOfId("1"),
                Lists.newArrayList(
                        new ArmifiedAttributeValuePair(attributes[0], "ValueOf1", "Document1", "A"),
                        new ArmifiedAttributeValuePair(attributes[0], "AnotherValueOf1", "Document1", "A"),
                        new ArmifiedAttributeValuePair(attributes[0], "ValueOf1InOtherDoc", "Document2", "Some arm")
                ));
        performMultisetCheck(collection.getPairsOfId("2"),
                Lists.newArrayList(
                        new ArmifiedAttributeValuePair(attributes[1], "ValueOf2", "Document1", "A"),
                        new ArmifiedAttributeValuePair(attributes[1], "ValueOf2InOtherArm", "Document1", "B")
                ));
        // this is definitely failing if Multiset is replaced with a regular set
        performMultisetCheck(collection.getPairsOfId("4"),
                Lists.newArrayList(
                        new ArmifiedAttributeValuePair(attributes[3], "ValueOf4", "Document4", "A"),
                        new ArmifiedAttributeValuePair(attributes[3], "ValueOf4", "Document4", "A")
                ));
    }

    @Test
    public void testIndexingByDocname() {
        performMultisetCheck(collection.getPairsInDoc("Document2"),
                Lists.newArrayList(new ArmifiedAttributeValuePair(attributes[0], "ValueOf1InOtherDoc", "Document2", "Some arm")));
    }

    @Test
    public void testIndexingByArm() {
        Map<String, Multiset<ArmifiedAttributeValuePair>> armified = collection.getArmifiedPairsInDoc("Document1");
        performMultisetCheck(armified.get("A"),
                Lists.newArrayList(
                        new ArmifiedAttributeValuePair(attributes[0], "ValueOf1", "Document1", "A"),
                        new ArmifiedAttributeValuePair(attributes[0], "AnotherValueOf1", "Document1", "A"),
                        new ArmifiedAttributeValuePair(attributes[1], "ValueOf2", "Document1", "A")
                ));
        performMultisetCheck(armified.get("B"),
                Lists.newArrayList(
                        new ArmifiedAttributeValuePair(attributes[1], "ValueOf2InOtherArm", "Document1", "B")
                ));
    }

    private <T> void performMultisetCheck(Multiset<T> set, List<T> list) {
        Assert.assertEquals(list.size(), set.size());
        for (T element : list) {
            Assert.assertTrue(set.contains(element));
        }
        for (T element : set) {
            Assert.assertTrue(list.contains(element));
        }
    }

    private <T> void performSizeCheck(Multiset<T> set, int size) {
        Assert.assertEquals(set.size(), size);
    }
}
