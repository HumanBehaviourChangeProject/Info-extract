package com.ibm.drl.hbcp.extraction.indexing;

import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.List;

public class FileIndexManager extends InMemoryDocumentIndexManager {

    public FileIndexManager(File file) throws IOException, TikaException, SAXException {
        super(DocumentInputStream.fromFile(file), SlidingWindowIndexManager.buildParagraphBuilders());
    }

    public FileIndexManager(List<File> files) throws IOException, TikaException, SAXException {
        super(DocumentInputStream.fromFiles(files), SlidingWindowIndexManager.buildParagraphBuilders());
    }
}
