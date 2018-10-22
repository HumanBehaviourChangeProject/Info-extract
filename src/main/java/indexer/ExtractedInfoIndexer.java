/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import extractor.InformationUnit;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

/**
 * Class to manage loading/storing of extracted values for each attribute.
 * The functionality makes use of a Lucene index to achieve this. The front-end
 * makes use of this data load/store functionality.
 * 
 * @author dganguly
 */
public class ExtractedInfoIndexer extends AbstractIndexer {
    IndexWriter writer;

    public ExtractedInfoIndexer(String propFile) throws Exception {
        super(propFile);
        indexDir = BaseDirInfo.getPath(prop.getProperty("ie.index"));
        createIndexDir(indexDir);
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        
        File indexDirDocs = new File(indexDir);                
        writer = new IndexWriter(FSDirectory.open(indexDirDocs.toPath()), iwcfg);        
    }
    
    public void close() throws Exception {
        writer.close();
    }
    
    /**
     * Adds a record (extracted value information) in the index.
     * @param iu An information unit object that is to be stored.
     */
    public void addRecord(InformationUnit iu) throws Exception {
        Document doc = iu.constructIndexRecord();
        writer.addDocument(doc);
    }    
}
