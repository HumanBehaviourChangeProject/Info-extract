package com.ibm.drl.hbcp.parser.pdf.manager;

import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.textract.TextractJsonParser;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Properties;

public class PdfToTextractParse implements PdfToDocumentFunction {

    private final File textractFolder;

    public PdfToTextractParse(Properties props) {
        textractFolder = new File(props.getProperty("coll.extracted.textract"));
    }

    @Override
    public Document getDocument(File pdf) throws IOException {
        // get the correct ABBYY XML file
        Path textractJson = Paths.get(
                textractFolder.getAbsolutePath(),
                pdf.getName().replaceAll(".pdf", ""),
                "apiResponse.json"
        );
        // parse that xml
        TextractJsonParser parser = new TextractJsonParser(textractJson.toFile());
        return parser.getDocument();
    }

    @Override
    public String toString() {
        return "PDF->Textract";
    }
}
