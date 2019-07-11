package com.ibm.drl.hbcp.util;

import static org.junit.Assert.*;

import org.junit.BeforeClass;
import org.junit.Test;

public class AttributeIdLookupTest {

    public static String[] INPUT = {
            "4507433"
    };

    public static String[] EXPECTED_NAMES = {
            "Minimum Age"
    };

    @BeforeClass
    public static void testSuccessfulCreation() {
        assertTrue(AttributeIdLookup.getInstance().isInitialized());
    }

    @Test
    public void testAttributeNameCorrectness() {
        for (int i = 0; i < INPUT.length; i++) {
            assertEquals(EXPECTED_NAMES[i], AttributeIdLookup.getInstance().getAttributeName(INPUT[i]));
        }
    }
}
