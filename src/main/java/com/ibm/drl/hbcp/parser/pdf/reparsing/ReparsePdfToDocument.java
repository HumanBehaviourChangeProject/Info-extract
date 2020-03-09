package com.ibm.drl.hbcp.parser.pdf.reparsing;

import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.util.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ReparsePdfToDocument implements PdfToDocumentFunction {

    private final File reparseJsonsFolder;

    public ReparsePdfToDocument(File outputJsonsFolder) {
        reparseJsonsFolder = outputJsonsFolder;
    }

    public ReparsePdfToDocument(Properties props) {
        this(FileUtils.potentiallyGetAsResource(new File(props.getProperty("coll.extracted.reparse"))));
    }

    @Override
    public Document getDocument(File pdf) throws IOException {
        File json = getJson(pdf);
        return new Reparser(json).getDocument();
    }

    private File getJson(File pdf) {
        return new File(reparseJsonsFolder, pdf.getName() + ".json");
    }
}
