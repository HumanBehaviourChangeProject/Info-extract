/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author dganguly
 */
public class PassageRetrieverTest {
    static PaperIndexer indexer;
    
    public PassageRetrieverTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
        try {
            System.out.println("Creating index before IE");
            indexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
            indexer.processAll();            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        try {
            indexer.removeIndexDirs();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Test of retrieve method, of class PassageRetriever.
     */
    @Test
    public void testRetrieve() throws Exception {
        System.out.println("Testing retrieve() of PassageRetriever...");
        String query = "age";
        
        PassageRetriever instance = new PassageRetriever(BaseDirInfo.getPath("test.properties"));
        TopDocs result = instance.retrieve(query, 10);
        
        assertNotNull(result);
        assertNotEquals(0, result.scoreDocs.length);
    }

    /**
     * Test of analyze method, of class PassageRetriever.
     */
    @Test
    public void testAnalyze() throws Exception {
        System.out.println("Testing analyze() of PassageRetriever...");
        String query = "This, is a test.";
        PassageRetriever instance = new PassageRetriever(BaseDirInfo.getPath("test.properties"));
        assertNotNull(instance);
        
        String expResult = "test ";  // should see this after stopword removal and stemming
        String result = instance.analyze(query);
        assertEquals(expResult, result);
    }
}
