package com.ibm.drl.hbcp.extraction.indexing.docparsing;

import java.io.IOException;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.extraction.indexing.DocumentInputStream;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.parser.pdf.Page;
import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyXmlParser;

public class AbbyyXmlOutputDocParser implements ResearchDocParser {
    @Override
    public ResearchDoc parse(DocumentInputStream documentInputStream) throws IOException {
        /* TODO: this constructor does nothing important at the moment, but if it changes this could break
         * TODO: because ABBYY output is not regular raw PDF. */
        ResearchDoc researchDoc = new ResearchDoc(documentInputStream.getInputStream());
        // this is what would be done for regular PDFs, but we don't do that here
        // researchDoc.extractInfoFromDOM();
        // parse the ABBYY XML
        AbbyyXmlParser parsedAbbyy = new AbbyyXmlParser(documentInputStream.getInputStream());
        // set the text of the research document
        researchDoc.setPpText(parsedAbbyy.toText());
        researchDoc.setPlainText(researchDoc.getPpText());
        // set pages
        researchDoc.setPages(parsedAbbyy.getDocument().getPages().stream()
                .map(Page::getValue)
                .collect(Collectors.toList()));
        return researchDoc;
    }
}
