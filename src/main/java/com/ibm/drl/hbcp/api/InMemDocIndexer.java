/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.api;

import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import java.io.InputStream;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;

/**
 * This class builds an in-memory index from a supplied pdf document. It first
 * extracts the text from the pdf, splits it up into sentences or passages of
 * of pre-configured lengths (in terms of number of words) and adds these retrievable
 * units into an in-memory transient index for similarity computation using Lucene's standard retrieval models.
 *
 * @author dganguly
 */
public class InMemDocIndexer {

    Analyzer analyzer;
    InputStream fstream;
    String fileName;

    public static final int PSEUDO_DOCID = 0;   // there's only one document to be added
    
    public InMemDocIndexer(InputStream fstream, String fileName) {
        analyzer = PaperIndexer.constructAnalyzer(this.getClass().getClassLoader().getResource("stop.txt").getPath());
        this.fstream = fstream;
        this.fileName = fileName;
    }
        
    /**
     * Used by the API to extract information from any document
     * uploaded from the web interface
     *
     * @param windowSize   Size of a passage in terms of the number of words.
     * @return A pointer to an in-memory Lucene index (a 'Directory' object).
    */
    Directory indexFile(int windowSize) throws Exception {
        
        String extension = FilenameUtils.getExtension(fileName);
        if (!extension.equalsIgnoreCase("pdf"))
            return null;
        
        Directory ramdir = new RAMDirectory();                
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);
    
        ResearchDoc researchDoc = new ResearchDoc(fstream);
        researchDoc.extractInfoFromDOM();
        
        Document doc = researchDoc.constructDoc(PSEUDO_DOCID, fileName);
        writer.addDocument(doc);
        
        writer.commit();
        writer.close();
        return writer.getDirectory();
    }
}
