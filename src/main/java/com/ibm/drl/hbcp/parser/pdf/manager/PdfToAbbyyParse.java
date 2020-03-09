package com.ibm.drl.hbcp.parser.pdf.manager;

import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyXmlParser;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class PdfToAbbyyParse implements PdfToDocumentFunction {

    private final File abbyyFolder;

    public static final String ABBYY_FOLDER_PROPERTY = "coll.extracted.abbyy";

    public PdfToAbbyyParse(File abbyyFolder) {
        this.abbyyFolder = abbyyFolder;
    }

    public PdfToAbbyyParse(Properties props) {
        this(new File(props.getProperty(ABBYY_FOLDER_PROPERTY)));
    }

    @Override
    public Document getDocument(File pdf) throws IOException {
        // get the correct ABBYY XML file
        File abbyyXml = new File(abbyyFolder, pdf.getName() + ".xml");
        // parse that xml
        AbbyyXmlParser parser = new AbbyyXmlParser(abbyyXml);
        return parser.getDocument();
    }

    @Override
    public String toString() {
        return "Abbyy";
    }
}
