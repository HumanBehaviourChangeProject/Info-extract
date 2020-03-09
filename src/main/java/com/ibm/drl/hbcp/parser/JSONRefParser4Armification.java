package com.ibm.drl.hbcp.parser;

import java.io.File;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Properties;

/**
 * A strict copy of JSONRefParser. To remove.
 * @author marting
 */
@Deprecated
public class JSONRefParser4Armification extends JSONRefParser {

    public JSONRefParser4Armification(File jsonFile) throws IOException {
        super(jsonFile);
    }

    public JSONRefParser4Armification(Properties props) throws IOException {
        super(props);
    }

    public JSONRefParser4Armification(String propFileName) throws IOException {
        super(propFileName);
    }

    public JSONRefParser4Armification(URL url) throws IOException, URISyntaxException {
        super(url);
    }
}
