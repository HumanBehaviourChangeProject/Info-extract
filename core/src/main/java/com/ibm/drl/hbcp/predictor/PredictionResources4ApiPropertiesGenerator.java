package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.util.FileUtils;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Files;
import java.util.Properties;

/**
 * Reads the "predictionresources4api.sh" script file, loads its property template,
 * assign the correct values to all the variables,
 * and finally returns properties for use in the API setup to retrain prediction models on the fly.
 *
 * @author mgleize
 */
public class PredictionResources4ApiPropertiesGenerator {

    public static final String SCRIPT_RESOURCE_PATH = "scripts/predictionresources4api.sh";
    public static final String EOF1 = "EOF1";
    public static final String EMBFILE = "embfile4api";
    public static final String GNODE_FILE = "resources/" + EMBFILE + ".vec";
    public static final String INODE_FILE = "resources/" + EMBFILE + ".per_instance.vec";
    public static final String MERGED_NODE_FILE = "resources/" + EMBFILE + ".merged.vec";
    public static final String TRAIN_FILE = "resources/train4api.tsv";

    public static Properties generate(int dimension, int node2VecWindow, double node2VecP, double node2VecQ) throws IOException {
        // prepare the variables to replace in the script with their values
        String[][] variablesAndValues = {
                { "DIM", String.valueOf(dimension) },
                { "WINDOW", String.valueOf(node2VecWindow) },
                { "P1", String.valueOf(node2VecP) },
                { "Q1", String.valueOf(node2VecQ) },
                { "MODE", "both" },
                { "TYPE", "r" },
                { "QUANTA", "0" },
                { "LARGER_CONTEXT", "false" },
                { "EMBFILE", EMBFILE },
                { "USE_TEXT", "true" },
                { "USE_NODES", "true" },
                { "GNODE_FILE", GNODE_FILE },
                { "INODE_FILE", INODE_FILE },
                { "MERGED_NODE_FILE", MERGED_NODE_FILE },
                { "SEQ_DIR", "resources" },
                { "TRAIN_SEQ_FILE", TRAIN_FILE },
                { "TEST_SEQ_FILE", "resources/test4api.tsv" },
        };
        // read the property file from the script
        String propertyStringTemplate = getPropStringTemplate();
        // replace each variable with their value
        String propertyString = replaceVariablesWithValues(propertyStringTemplate, variablesAndValues);
        // return the properties
        Properties props = new Properties();
        props.load(new StringReader(propertyString));
        return props;
    }

    private static String getPropStringTemplate() throws IOException {
        // read the script file
        File script = FileUtils.potentiallyGetAsResource(new File(SCRIPT_RESOURCE_PATH));
        String fullScriptString = StringUtils.join(Files.readAllLines(script.toPath()), "\n");
        // grab whatever is inbetween the two "EOF1", it should be a property file
        int start = fullScriptString.indexOf(EOF1);
        if (start < 0)
            throw new IOException("Script file " + script + " does not contain a single EOF1, so probably no properties either.");
        start += EOF1.length();
        int end = fullScriptString.indexOf(EOF1, start);
        if (end < 0)
            throw new IOException("Script file " + script + " does not contain a second EOF1, so it is probably badly formatted.");
        return fullScriptString.substring(start, end);
    }

    private static String replaceVariablesWithValues(String propString, String[][] variableAndValuePairs) {
        String[] searchList = new String[variableAndValuePairs.length];
        String[] replacementList = new String[variableAndValuePairs.length];
        for (int i = 0; i < variableAndValuePairs.length; i++) {
            searchList[i] = "$" + variableAndValuePairs[i][0];
            replacementList[i] = variableAndValuePairs[i][1];
        }
        return StringUtils.replaceEach(propString, searchList, replacementList);
    }

}
