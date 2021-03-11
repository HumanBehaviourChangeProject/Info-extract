package com.ibm.drl.hbcp.extraction.indexing.docparsing;

import com.ibm.drl.hbcp.extraction.indexing.DocumentInputStream;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.IOException;

public class PdfDocParser implements ResearchDocParser {
    @Override
    public ResearchDoc parse(DocumentInputStream documentInputStream) throws IOException {
        ResearchDoc researchDoc = new ResearchDoc(documentInputStream.getInputStream());
        try {
            researchDoc.extractInfoFromDOM();
        } catch (TikaException | SAXException e) {
            throw new IOException(e);
        }
        return researchDoc;
    }
}
