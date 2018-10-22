/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.util.Properties;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author dganguly
 */
public class PaperIndexerTest {
    
    public PaperIndexerTest() {
    }

    /**
     * Test of processAll method, of class PaperIndexer.
     */
    @Test
    public void testProcessAll() {
        
        try {
            PaperIndexer indexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
            indexer.processAll();
            
            Properties prop = indexer.getProperties();
            String paraIndexPath = prop.getProperty("para.index");
            prop.setProperty("para.index", paraIndexPath + "/1");
            
            final String[] indexes = { "index", "para.index", "para.index.all" };
            
            // Test global index and para index
            for (String indexPath : indexes) {
                File indexDir = new File(BaseDirInfo.getPath(indexer.getProperties().getProperty(indexPath)));
                IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));

                assertNotNull(reader);  // reader must not be null

                int nDocs = reader.numDocs();
                assertNotEquals(nDocs, 0); // Number of docs must not be zero

                reader.close();
            }
            
            // Since we put one document in the test folder, we should have one folder in the para index            
            prop.setProperty("para.index", paraIndexPath);
            File[] dirs = new File(prop.getProperty("para.index")).listFiles();
            assertEquals(dirs.length, 1);
            
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }        
    }
}
