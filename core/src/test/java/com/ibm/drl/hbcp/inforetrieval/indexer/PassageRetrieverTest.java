/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;

import org.apache.lucene.search.TopDocs;
import org.apache.tika.exception.TikaException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.xml.sax.SAXException;

/**
 *
 * @author dganguly
 */
public class PassageRetrieverTest {

    static PaperIndexer indexer;
    
    public PassageRetrieverTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException, TikaException, SAXException {
        System.out.println("Creating index before IE");
        indexer = new PaperIndexer("test.properties");
        indexer.processAll();
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException {
        indexer.removeIndexDirs();
    }
    
    /**
     * Test of retrieve method, of class PassageRetriever.
     */
    @Test
    public void testRetrieve() throws Exception {
        System.out.println("Testing retrieve() of PassageRetriever...");
        String query = "age";
        
        try (PassageRetriever instance = new PassageRetriever("test.properties")) {
            TopDocs result = instance.retrieve(query, 10);

            assertNotNull(result);
            assertNotEquals(0, result.scoreDocs.length);
        }
    }

    /**
     * Test of analyze method, of class PassageRetriever.
     */
    @Test
    public void testAnalyze() throws Exception {
        System.out.println("Testing analyze() of PassageRetriever...");
        String query = "This, is a test.";
        try (PassageRetriever instance = new PassageRetriever("test.properties")) {
            assertNotNull(instance);

            String expResult = "test ";  // should see this after stopword removal and stemming
            String result = instance.analyze(query);
            assertEquals(expResult, result);
        }
    }
}
