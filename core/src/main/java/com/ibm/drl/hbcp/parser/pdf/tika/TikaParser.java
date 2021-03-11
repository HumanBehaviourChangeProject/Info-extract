package com.ibm.drl.hbcp.parser.pdf.tika;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.parser.pdf.*;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

public class TikaParser implements PdfAnalysisOutput {

    private final Document doc;

    public TikaParser(InputStream pdfFile) throws IOException, TikaException, SAXException {
        ResearchDoc researchDoc = new ResearchDoc(pdfFile);
        researchDoc.extractInfoFromDOM();
        doc = new Document() {
            @Override
            public List<? extends Page> getPages() {
                return researchDoc.getPages().stream().map(page -> new Page() {
                    @Override
                    public List<? extends Block> getBlocks() {
                        return Lists.newArrayList(
                                new SimpleTextBlock(page)
                        );
                    }
                }).collect(Collectors.toList());
            }
        };
    }

    @Override
    public Document getDocument() {
        return doc;
    }
}
