/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.*;
import extractor.InformationExtractor;
import extractor.InformationUnit;
import extractor.PopulationMinAge;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import ref.JSONRefParser;

/**
 *
 * @author dganguly
 */
public class ExtractedInfoIndexerTest {
    static ExtractedInfoIndexer extractedInfoIndexer;
    static PaperIndexer docIndexer;
    
    public ExtractedInfoIndexerTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        // Set up the test environment by creating small doc and extracted info index
        try {
            System.out.println("Creating index before IE");
            docIndexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
            docIndexer.processAll();            
            
            extractedInfoIndexer = new ExtractedInfoIndexer(BaseDirInfo.getPath("test.properties"));
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        // Clear everything that we created.
        try {
            docIndexer.removeIndexDirs();
            extractedInfoIndexer.removeIndexDir(extractedInfoIndexer.getIndexDir());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Test of addRecord method, of class ExtractedInfoIndexer.
     */
    @Test
    public void testAddRecord() throws Exception {

        try {
            System.out.println("Testing saving of extracted information");
            
            String propFile = BaseDirInfo.getPath("test.properties");            
            InformationExtractor instance = new InformationExtractor(propFile);

            InformationUnit iu =
                    new PopulationMinAge(instance, ResearchDoc.FIELD_CONTENT,
                        JSONRefParser.POPULATION
                    );
            int docId = 0;

            InformationUnit result = instance.extractInformationFromDoc(docId, iu);
            extractedInfoIndexer.addRecord(result);
            
            extractedInfoIndexer.close();
            
            File indexDir = new File(extractedInfoIndexer.getIndexDir());
            assertNotNull(indexDir);
            assertTrue(indexDir.exists());
            assertTrue(indexDir.listFiles().length>0);            
        }
        catch (Exception ex) {
            // TODO review the generated test code and remove the default call to fail.
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

}
