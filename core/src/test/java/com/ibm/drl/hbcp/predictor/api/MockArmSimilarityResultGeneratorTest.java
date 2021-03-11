package com.ibm.drl.hbcp.predictor.api;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.List;

public class MockArmSimilarityResultGeneratorTest {

    @Test
    public void testGenerateTop10Results() throws IOException {
        MockArmSimilarityResultGenerator gen = new MockArmSimilarityResultGenerator();
        List<ArmSimilarityResult> results = gen.generateTopK(10);
        // check that there are 10
        Assert.assertEquals(10, results.size());
        // check that the first two are different (generateTopK should guarantee that at least their scores are different)
        Assert.assertNotEquals(results.get(0), results.get(1));
    }
}
