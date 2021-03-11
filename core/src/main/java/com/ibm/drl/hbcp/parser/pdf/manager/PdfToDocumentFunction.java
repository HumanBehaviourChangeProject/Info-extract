package com.ibm.drl.hbcp.parser.pdf.manager;

import com.ibm.drl.hbcp.parser.pdf.Document;

import java.io.File;
import java.io.IOException;

public interface PdfToDocumentFunction {

    Document getDocument(File pdf) throws IOException;
}
