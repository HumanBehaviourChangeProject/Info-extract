package com.ibm.drl.hbcp.parser.pdf;

/** The output of a PDF parser/analyzer
 *
 * @author marting
 * */
public interface PdfAnalysisOutput {

    Document getDocument();

    default String toText() {
        return getDocument().getValue();
    }
}
