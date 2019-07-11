package com.ibm.drl.hbcp.predictor.graph;

import java.io.FileReader;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;

/**
 * Reads the property file and displays a list of predictable outputs.
 * @author dganguly
 */
public class PredictableOutputs {

    private final List<String> predOutcomeAttribIds;

    public PredictableOutputs(String predOutcomeIds) {
        String[] tokens = predOutcomeIds.split("\\s*,\\s*");
        predOutcomeAttribIds = Arrays.asList(tokens);
    }

    public boolean contains(String attributeId) {
        return predOutcomeAttribIds.contains(attributeId);
    }

    public static void main(String[] args) {
        // read properties to get predictable output attribute ids
        if (args.length == 0) {
            args = new String[1];
            args[0] = "init.properties";
            System.err.println("Using default properties: " + args[0]);
        }
        Properties props = new Properties();
        try {
            props.load(new FileReader(BaseDirInfo.getPath(args[0])));
            String predOutcomeIdsStr = props.getProperty("prediction.attribtype.predictable.output");
            PredictableOutputs predictableOutputs = new PredictableOutputs(predOutcomeIdsStr);
            System.out.println("Prop file includes 3909808 as predictable output: " + predictableOutputs.contains("3909808"));
        } catch (IOException e) {
            e.printStackTrace();
        }
    
        
    }

}
