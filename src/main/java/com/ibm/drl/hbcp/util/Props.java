package com.ibm.drl.hbcp.util;

import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

/**
 * Methods to work with Properties and property files.
 * @author marting
 */
public class Props {

    /** Loads properties from a file. */
    public static Properties loadProperties(String propFilePath) throws IOException {
        Properties prop = new Properties();
        File path = FileUtils.potentiallyGetAsResource(new File(propFilePath));
        prop.load(new FileReader(path));
        return prop;
    }

    /** Loads properties from a file. */
    public static Properties loadProperties() throws IOException {
        return loadProperties(getDefaultPropFilename());
    }

    public static String getDefaultPropFilename() {
        return "init.properties";
    }

    public static Properties overrideProps(Properties originalProps, Properties extraProps) {
        Properties res = new Properties();
        // copy in the originals
        for (String key : originalProps.stringPropertyNames()) {
            res.setProperty(key, originalProps.getProperty(key));
        }
        // override
        for (String key : extraProps.stringPropertyNames()) {
            res.setProperty(key, extraProps.getProperty(key));
        }
        return res;
    }

    public static Properties overrideProps(Properties originalProps, List<Pair<String, String>> keyValuePairs) {
        Properties extraProps = new Properties();
        keyValuePairs.forEach(p -> extraProps.setProperty(p.getKey(), p.getValue()));
        return overrideProps(originalProps, extraProps);
    }
}
