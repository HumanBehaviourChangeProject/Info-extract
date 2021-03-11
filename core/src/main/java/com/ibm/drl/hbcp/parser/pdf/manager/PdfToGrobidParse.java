package com.ibm.drl.hbcp.parser.pdf.manager;

import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.grobid.GrobidParser;

import java.io.File;
import java.io.IOException;

public class PdfToGrobidParse implements PdfToDocumentFunction {
    @Override
    public Document getDocument(File pdf) throws IOException {
        try {
            return new GrobidParser(pdf).getDocument();
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    @Override
    public String toString() {
        return "Grobid";
    }
}
