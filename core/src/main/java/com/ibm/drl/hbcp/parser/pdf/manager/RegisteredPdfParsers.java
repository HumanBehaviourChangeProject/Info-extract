package com.ibm.drl.hbcp.parser.pdf.manager;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

public class RegisteredPdfParsers {

    public enum PdfParser {
        GROBID,
        ABBYY,
        TEXTRACT
    }

    public static Map<PdfParser, PdfToDocumentFunction> getParsers(Properties props) {
        Map<PdfParser, PdfToDocumentFunction> res = new HashMap<>();
        res.put(PdfParser.GROBID, new PdfToGrobidParse());
        res.put(PdfParser.ABBYY, new PdfToAbbyyParse(props));
        res.put(PdfParser.TEXTRACT, new PdfToTextractParse(props));
        return res;
    }

}
