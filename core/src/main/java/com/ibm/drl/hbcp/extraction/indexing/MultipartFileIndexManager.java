package com.ibm.drl.hbcp.extraction.indexing;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.extraction.indexing.docparsing.PdfDocParser;
import com.ibm.drl.hbcp.extraction.indexing.docparsing.ResearchDocParser;
import org.apache.tika.exception.TikaException;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

public class MultipartFileIndexManager extends InMemoryDocumentIndexManager {

    public MultipartFileIndexManager(MultipartFile file,
                                     ResearchDocParser docParser,
                                     String filename,
                                     String[] windowSizes) throws IOException, TikaException, SAXException {
        super(fromMultipartFile(file, filename), docParser, Lists.newArrayList(SlidingWindowIndexManager.buildParagraphBuilders(windowSizes)));
    }

    public MultipartFileIndexManager(MultipartFile file, String filename, String[] windowSizes) throws IOException, TikaException, SAXException {
        this(file, new PdfDocParser(), filename, windowSizes);
    }

    private static List<DocumentInputStream> fromMultipartFile(MultipartFile file, String filename) throws IOException {
        return Lists.newArrayList(new DocumentInputStream(file.getInputStream(), filename));
    }
}
