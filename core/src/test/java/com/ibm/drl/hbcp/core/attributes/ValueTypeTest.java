package com.ibm.drl.hbcp.core.attributes;

import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.FileUtils;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.*;

public class ValueTypeTest {

    private static final String jsonPath = "data/jsons/All_annotations_512papers_05March20.json";

    private static JSONRefParser parser;

    @Before
    public void setUp() throws Exception {
        parser = new JSONRefParser(FileUtils.potentiallyGetAsResource(new File(jsonPath)));
    }

    @Test
    public void checkPrioritizedValueTypes() {
        assertEquals(37, ValueType.PRESENCE_TYPE.size());
        assertEquals(39, ValueType.VALUE_TYPE.size());
        assertEquals(6, ValueType.COMPLEX_TYPE.size());

        Attributes attributes = parser.getAttributes();
        for (String attName : ValueType.PRESENCE_TYPE) {
            Attribute att = attributes.getFromName(attName);
            assertNotNull("Couldn't find " + attName, att);
            assertEquals(att.getName(), attName);
        }
        for (String attName : ValueType.VALUE_TYPE) {
            Attribute att = attributes.getFromName(attName);
            assertNotNull("Couldn't find " + attName, att);
            assertEquals(att.getName(), attName);
        }
        for (String attName : ValueType.COMPLEX_TYPE) {
            Attribute att = attributes.getFromName(attName);
            assertNotNull("Couldn't find " + attName, att);
            assertEquals(att.getName(), attName);
        }
    }

    @Test
    public void isPresenceType() {
        assertTrue(ValueType.isPresenceType(parser.getAttributes().getFromName("1.1.Goal setting (behavior)")));
        assertTrue(ValueType.isPresenceType(parser.getAttributes().getFromName("Smoking")));
        assertFalse(ValueType.isPresenceType(parser.getAttributes().getFromName("Effect size estimate")));
        assertFalse(ValueType.isPresenceType(parser.getAttributes().getFromName("Individual reasons for attrition")));
    }
}