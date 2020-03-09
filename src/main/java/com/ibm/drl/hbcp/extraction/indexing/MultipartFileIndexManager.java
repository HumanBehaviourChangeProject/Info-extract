package com.ibm.drl.hbcp.extraction.indexing;

import com.google.common.collect.Lists;
import org.apache.tika.exception.TikaException;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;

public class MultipartFileIndexManager extends InMemoryDocumentIndexManager {

    public MultipartFileIndexManager(MultipartFile file, String filename, String[] windowSizes) throws IOException, TikaException, SAXException {
        super(fromMultipartFile(file, filename), Lists.newArrayList(SlidingWindowIndexManager.buildParagraphBuilders(windowSizes)));
    }

    private static List<DocumentInputStream> fromMultipartFile(MultipartFile file, String filename) throws IOException {
        return Lists.newArrayList(new DocumentInputStream(file.getInputStream(), filename));
    }
}
