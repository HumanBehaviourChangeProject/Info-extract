package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.Lists;

import java.util.List;

public class NEqualsArtifactCleanerTest extends SingleValueCleanerTest {

    @Override
    public List<CleaningTestPair> getTestPairs() {
        return Lists.newArrayList(
                makeTestPair("", "n=314","", "n=314"),
                makeTestPair("N=314", "", "N=314", ""),
                makeTestPair("", "n5314","", "n=314"),
                makeTestPair("N5314", "", "N=314", "")
        );
    }

    @Override
    public Cleaner newCleaner() {
        return new NEqualsArtifactCleaner();
    }

    private CleaningTestPair makeTestPair(String contextOriginal, String valueOriginal, String contextExpectedCleaned, String valueExpectedCleaned) {
        return new CleaningTestPair(
                SingleValueCleanerTest.makeAavp(contextOriginal, valueOriginal),
                SingleValueCleanerTest.makeAavp(contextExpectedCleaned, valueExpectedCleaned)
        );
    }
}
