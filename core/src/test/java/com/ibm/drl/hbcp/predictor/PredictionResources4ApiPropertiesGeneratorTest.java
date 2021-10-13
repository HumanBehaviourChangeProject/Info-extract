package com.ibm.drl.hbcp.predictor;

import org.junit.Test;

import java.io.IOException;
import java.util.Properties;

import static org.junit.Assert.assertFalse;

public class PredictionResources4ApiPropertiesGeneratorTest {

    @Test
    public void testPropGenerationInDefaultScenario() throws IOException {
        // at time of writing these are actually the parameters for our core Docker container
        Properties props = PredictionResources4ApiPropertiesGenerator.generate(100, 10, 0.1, 0.9);
        //System.out.println(props);
        for (Object value : props.values()) {
            assertFalse(value.toString().contains("$"));
        }
    }
}
