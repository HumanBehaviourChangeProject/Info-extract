package com.ibm.drl.hbcp.predictor.nodes;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.util.Properties;

import com.ibm.drl.hbcp.predictor.graph.PredictableOutputs;
import org.junit.BeforeClass;
import org.junit.Test;

public class PredictableOutputsTest {

    private static PredictableOutputs predictableOutputs;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        Properties props = new Properties();
        props.load(new FileReader("test.properties"));
        String predOutcomeIdsStr = props.getProperty("prediction.attribtype.predictable.output");
        //String[] predOutcomeIds = predOutcomeIdsStr.split("\\s*,\\s*");
        predictableOutputs = new PredictableOutputs(predOutcomeIdsStr);
    }

    @Test
    public void testContains() {
        assertTrue(predictableOutputs.contains("3909808"));
        assertTrue(predictableOutputs.contains("dummyAttId1"));
        assertTrue(predictableOutputs.contains("dummyAttId2"));
        assertTrue(predictableOutputs.contains("dummyAttId3"));
        assertFalse(predictableOutputs.contains("dummyAttId4"));
        assertFalse(predictableOutputs.contains("007"));
    }

}
