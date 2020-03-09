/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import java.io.File;
import java.io.IOException;
import java.util.Properties;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;

/**
 *
 * @author dganguly
 */
public class PaperIndexerTest {

    /**
     * Test of processAll method, of class PaperIndexer.
     */
    @Test
    public void testProcessAll() throws TikaException, SAXException, IOException, Exception {
        PaperIndexer indexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
        indexer.processAll();

        Properties prop = indexer.getProperties();
        String paraIndexPath = prop.getProperty("para.index");
        prop.setProperty("para.index", paraIndexPath + "/1");

        final String[] indexes = { "index", "para.index", "para.index.all" };

        // Test global index and para index
        for (String indexPath : indexes) {
            File indexDir = new File(BaseDirInfo.getPath(indexer.getProperties().getProperty(indexPath)));
            try (Directory dir = FSDirectory.open(indexDir.toPath());
                 IndexReader reader = DirectoryReader.open(dir)) {
                assertNotNull(reader);  // reader must not be null

                int nDocs = reader.numDocs();
                assertNotEquals(nDocs, 0); // Number of docs must not be zero
            }
        }

        // Since we put one document in the test folder, we should have one folder in the para index
        prop.setProperty("para.index", paraIndexPath);
        File[] dirs = new File(prop.getProperty("para.index")).listFiles();
        assertEquals(dirs.length, 1);

        // clean folders
       indexer.removeIndexDirs();
    }
}
