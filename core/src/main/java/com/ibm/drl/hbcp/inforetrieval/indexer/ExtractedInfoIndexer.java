/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

/**
 * Class to manage loading/storing of extracted values for each attribute.
 * The functionality makes use of a Lucene index to achieve this. The front-end
 * makes use of this data load/store functionality.
 *
 * @author dganguly
 */
public class ExtractedInfoIndexer extends AbstractIndexer implements Closeable {

    IndexWriter writer;

    public ExtractedInfoIndexer(String propFile) throws IOException {
        super(propFile);
        indexDir = BaseDirInfo.getPath(prop.getProperty("ie.index"));
        createIndexDir(indexDir);
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        
        File indexDirDocs = new File(indexDir);
        writer = new IndexWriter(FSDirectory.open(indexDirDocs.toPath()), iwcfg);
    }

    @Override
    public void close() throws IOException {
        writer.close();
    }

    /**
     * Adds a document in the index.
     * @param doc A value converted to a Document.
     */
    public void addDocument(Document doc) throws IOException {
        writer.addDocument(doc);
    }
}
