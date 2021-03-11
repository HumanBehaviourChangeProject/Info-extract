package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import lombok.Data;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public abstract class SingleValueCleanerTest {

    protected abstract List<CleaningTestPair> getTestPairs();

    protected abstract Cleaner newCleaner();

    @Test
    public void testCleaning() {
        // build the cleaner
        Cleaner cleaner = newCleaner();
        // run all the test cases
        for (CleaningTestPair testPair : getTestPairs()) {
            List<AnnotatedAttributeValuePair> cleaned = cleaner.clean(Lists.newArrayList(testPair.getOriginal()));
            if (cleaned.isEmpty()) {
                throw new AssertionError("The cleaned list should not be empty.");
            } else {
                AnnotatedAttributeValuePair cleanedAvp = cleaned.get(0);
                assertEquals(testPair.getExpectedCleaned().getContext(), cleanedAvp.getContext());
                assertEquals(testPair.getExpectedCleaned().getValue(), cleanedAvp.getValue());
            }
        }
    }

    @Data
    static class CleaningTestPair {
        private final AnnotatedAttributeValuePair original;
        private final AnnotatedAttributeValuePair expectedCleaned;
    }

    static AnnotatedAttributeValuePair makeAavp(String context, String value) {
        return new AnnotatedAttributeValuePair(
                new Attribute("1", AttributeType.NONE, "Fake attribute"),
                value,
                "Fake doc 2008.pdf",
                Arm.MAIN,
                context,
                value,
                "Sprint1",
                1
        );
    }
}
