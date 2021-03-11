package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static org.junit.Assert.*;

public class AvcKrippendorffAlphaAgreementTest {

    private static final String smallJsonPath = "data/test/jsons/armified_testfile.json";

    private AvcKrippendorffAlphaAgreement avcKrippendorffAlphaAgreement;

    @Before
    public void setUp() {
        avcKrippendorffAlphaAgreement = new AvcKrippendorffAlphaAgreement();
    }

    @Test
    public void evaluateSimplePresenceType() {
        List<ArmifiedAttributeValuePair> pairs1 = new ArrayList<>();
        List<ArmifiedAttributeValuePair> pairs2 = new ArrayList<>();

        // missing annotation treated as 'negative' for presence attributes
        Attribute attribute = Attributes.get().getFromName("1.1.Goal setting (behavior)");
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc2", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc3", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc4", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc5", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc6", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc7", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc9", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc10", "arm1"));

        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc3", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc4", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc5", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc7", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc8", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc9", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc10", "arm1"));

        AttributeValueCollection<ArmifiedAttributeValuePair> avc1 = new AttributeValueCollection<>(pairs1);
        AttributeValueCollection<ArmifiedAttributeValuePair> avc2 = new AttributeValueCollection<>(pairs2);

        Map<Attribute, List<Double>> alphas = avcKrippendorffAlphaAgreement.evaluate(avc1, avc2);

        List<Double> attribAlpha = alphas.get(attribute);
        assertNotNull(attribAlpha);
        assertEquals(1, attribAlpha.size());
//        assertEquals(-0.117647058823, attribAlpha.get(0), 0.00001);  // if considering 'extracted' and 'reference' documents
        assertEquals(0.0, attribAlpha.get(0), 0.00001);  // if considering only 'reference' documents
    }

    @Test
    public void testSinglePresenceAttribute() {
        // missing annotation treated as 'negative' for presence attributes
        Attribute attribute = Attributes.get().getFromName("1.1.Goal setting (behavior)");
        Attribute attribute2 = Attributes.get().getFromName("1.2 Problem solving ");
        List<ArmifiedAttributeValuePair> pairs1 = new ArrayList<>();
        List<ArmifiedAttributeValuePair> pairs2 = new ArrayList<>();
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute2, "1", "doc1", "arm1"));
        AttributeValueCollection<ArmifiedAttributeValuePair> avc1 = new AttributeValueCollection<>(pairs1);
        AttributeValueCollection<ArmifiedAttributeValuePair> avc2 = new AttributeValueCollection<>(pairs2);

        Map<Attribute, List<Double>> alphas = avcKrippendorffAlphaAgreement.evaluate(avc1, avc2);

        List<Double> attribAlpha = alphas.get(attribute);
        assertNotNull(attribAlpha);
        assertEquals(1, attribAlpha.size());
        // not sure if technically alpha is well defined here, but we'll consider it perfect agreement
        assertEquals(1.0, attribAlpha.get(0), 0.00001);

//        attribAlpha = alphas.get(attribute2);
//        assertNotNull(attribAlpha);
//        assertEquals(0.0, attribAlpha.get(0), 0.00001);
    }

    @Test
    public void testAgreementWithOneLabel() {
        // Krippendorff alpha expects different labels, but if we see complete agreement with one label, we'll consider it agreement of 1.0
        // missing annotation treated as 'negative' for presence attributes
        Attribute attribute = Attributes.get().getFromName("Outcome value");
        List<ArmifiedAttributeValuePair> pairs1 = new ArrayList<>();
        List<ArmifiedAttributeValuePair> pairs2 = new ArrayList<>();

        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "2", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "3", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "4", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "5", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "6", "doc1", "arm1"));

        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "2", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "3", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "4", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "5", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "6", "doc1", "arm1"));
        AttributeValueCollection<ArmifiedAttributeValuePair> avc1 = new AttributeValueCollection<>(pairs1);
        AttributeValueCollection<ArmifiedAttributeValuePair> avc2 = new AttributeValueCollection<>(pairs2);

        Map<Attribute, List<Double>> alphas = avcKrippendorffAlphaAgreement.evaluate(avc1, avc2);

        List<Double> attribAlpha = alphas.get(attribute);
        assertNotNull(attribAlpha);
        assertEquals(1, attribAlpha.size());
        // not sure if technically alpha is well defined here, but we'll consider it perfect agreement
        assertEquals(1.0, attribAlpha.get(0), 0.00001);
    }

    @Test
    public void evaluateSimpleValueType() {
        List<ArmifiedAttributeValuePair> pairs1 = new ArrayList<>();
        List<ArmifiedAttributeValuePair> pairs2 = new ArrayList<>();

        // missing annotation treated as 'negative' for presence attributes
        Attribute attribute = Attributes.get().getFromName("Outcome value");
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "3.8", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "3.8", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "4.7", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "3", "doc1", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc2", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "2", "doc2", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc3", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc3", "arm1"));
        pairs1.add(new ArmifiedAttributeValuePair(attribute, "1", "doc4", "arm1"));

        pairs2.add(new ArmifiedAttributeValuePair(attribute, "3.8", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "4.7", "doc1", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc2", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc2", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "10", "doc3", "arm1"));  // would match using contains, but no annotation to match
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "1", "doc3", "arm1"));
        pairs2.add(new ArmifiedAttributeValuePair(attribute, "2", "doc3", "arm1"));

        AttributeValueCollection<ArmifiedAttributeValuePair> avc1 = new AttributeValueCollection<>(pairs1);
        AttributeValueCollection<ArmifiedAttributeValuePair> avc2 = new AttributeValueCollection<>(pairs2);

        Map<Attribute, List<Double>> alphas = avcKrippendorffAlphaAgreement.evaluate(avc1, avc2);

        List<Double> attribAlpha = alphas.get(attribute);
        assertNotNull(attribAlpha);
        assertEquals(1, attribAlpha.size());
//        assertEquals(-0.357142857142857, attribAlpha.get(0), 0.00001);  // if considering 'extracted' and 'reference' documents
        assertEquals(-0.307692307692308, attribAlpha.get(0), 0.00001);  // if considering only 'reference' documents

    }

    @Test
    public void evaluateWithTestJson() throws IOException {
        AttributeValueCollection<AnnotatedAttributeValuePair> avc1 = new JSONRefParser(new File(smallJsonPath)).getAttributeValuePairs();

        Attribute att1 = Attributes.get().getFromName("1.1.Goal setting (behavior)");
        Attribute att2 = Attributes.get().getFromName("1.2 Problem solving ");
        Attribute att3 = Attributes.get().getFromName("1.3 Goal setting (outcome)");
        Attribute att4 = Attributes.get().getFromName("Outcome value");
        List<ArmifiedAttributeValuePair> pairs2 = new ArrayList<>();
        pairs2.add(new ArmifiedAttributeValuePair(att1, "1", "Abrantes 2014.pdf", Arm.EMPTY));
        pairs2.add(new ArmifiedAttributeValuePair(att2, "1", "Abrantes 2014.pdf", Arm.EMPTY));
        pairs2.add(new ArmifiedAttributeValuePair(att3, "1", "Abrantes 2014.pdf", Arm.EMPTY));
        pairs2.add(new ArmifiedAttributeValuePair(att4, "7.4", "Abdullah 2005.pdf", "Health education control - HEC"));
        pairs2.add(new ArmifiedAttributeValuePair(att4, "12", "Abrantes 2014.pdf", "Health education control - HEC"));
        pairs2.add(new ArmifiedAttributeValuePair(att4, "123456789", "Abrantes 2014.pdf", "Health education control - HEC"));

        AttributeValueCollection<ArmifiedAttributeValuePair> avc2 = new AttributeValueCollection<>(pairs2);

        Map<Attribute, List<Double>> alphas = avcKrippendorffAlphaAgreement.evaluate(avc1, avc2);

        // BCT 1.1
        List<Double> attribAlpha = alphas.get(att1);
        assertNotNull(attribAlpha);
        assertEquals(1, attribAlpha.size());
        assertEquals(1.0, attribAlpha.get(0), 0.00001);
        // BCT 1.2
        attribAlpha = alphas.get(att2);
        assertNotNull(attribAlpha);
        assertEquals(1, attribAlpha.size());
        assertEquals(0.0, attribAlpha.get(0), 0.00001);
        // BCT 1.3
        attribAlpha = alphas.get(att3);
        assertNull(attribAlpha);  // attribute not in reference, so ignored
        // Outcome value
        attribAlpha = alphas.get(att4);
        assertNotNull(attribAlpha);
        assertEquals(1, attribAlpha.size());
        assertEquals(-0.285714285714286, attribAlpha.get(0), 0.00001);
    }
}