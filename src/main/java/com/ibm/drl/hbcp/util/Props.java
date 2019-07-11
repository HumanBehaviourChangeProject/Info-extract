package com.ibm.drl.hbcp.util;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Methods to work with Properties and property files.
 * @author marting
 */
public class Props {

    /** Loads properties from a file. */
    public static Properties loadProperties(String propFilePath) throws IOException {
        Properties prop = new Properties();
        prop.load(new FileReader(propFilePath));
        return prop;
    }
}
