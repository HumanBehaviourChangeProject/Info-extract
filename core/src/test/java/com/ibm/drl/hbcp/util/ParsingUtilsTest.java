package com.ibm.drl.hbcp.util;

import com.google.common.collect.Lists;
import lombok.Data;
import org.jetbrains.annotations.Nullable;
import org.junit.Test;

import java.util.List;

import static org.junit.Assert.assertEquals;

public class ParsingUtilsTest {

    private static final List<NumberStringTestPair> stringAndNumberStringPairs = Lists.newArrayList(
            new NumberStringTestPair("0", "0"),
            new NumberStringTestPair("abc", null),
            new NumberStringTestPair("123", "123"),
            new NumberStringTestPair("3.14", "3.14"),
            new NumberStringTestPair(".864", ".864"),
            new NumberStringTestPair(".", null),
            new NumberStringTestPair("-.314", "-.314"),
            new NumberStringTestPair("1,044", "1,044"),
            new NumberStringTestPair("1,044,123.56", "1,044,123.56"),
            new NumberStringTestPair("1,7464.0", "1"),
            new NumberStringTestPair("3a765", "3")
    );

    @Data
    private static class NumberStringTestPair {
        private final String rawString;
        private final @Nullable String expectedFirstNumberString;
    }

    @Test
    public void testNumberStringParsing() {
        for (NumberStringTestPair testPair : stringAndNumberStringPairs) {
            assertEquals(testPair.expectedFirstNumberString, parseNumberString(testPair.rawString + "extraText In The String"));
        }
    }

    private String parseNumberString(String rawString) {
        try {
            return ParsingUtils.parseFirstDoubleString(rawString);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
