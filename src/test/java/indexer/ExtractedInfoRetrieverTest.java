/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import extractor.InformationExtractor;
import extractor.InformationUnit;
import extractor.PopulationMinAge;
import static indexer.ExtractedInfoIndexerTest.docIndexer;
import java.io.File;
import org.apache.lucene.document.Document;
import org.apache.lucene.search.TopDocs;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import ref.JSONRefParser;
import static indexer.ExtractedInfoIndexerTest.extractedInfoIndexer;

/**
 *
 * @author dganguly
 */
public class ExtractedInfoRetrieverTest {
    static ExtractedInfoIndexer extractedInfoIndexer;
    static PaperIndexer docIndexer;
    
    static String propFile = BaseDirInfo.getPath("test.properties");            
    static final int docId = 0;

    public ExtractedInfoRetrieverTest() {
    }

    @BeforeClass
    public static void setUpClass() {
        try {
            System.out.println("Creating index before IE");
            docIndexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
            docIndexer.processAll();            
            
            extractedInfoIndexer = new ExtractedInfoIndexer(BaseDirInfo.getPath("test.properties"));
            insertRecord();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @AfterClass
    public static void tearDownClass() {
        try {
            docIndexer.removeIndexDirs();
            extractedInfoIndexer.removeIndexDir(extractedInfoIndexer.getIndexDir());
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    static void insertRecord() throws Exception {
        System.out.println("Inserting into IE index");

        InformationExtractor instance = new InformationExtractor(propFile);

        InformationUnit iu =
                new PopulationMinAge(instance, ResearchDoc.FIELD_CONTENT,
                    JSONRefParser.POPULATION
                );

        InformationUnit result = instance.extractInformationFromDoc(docId, iu);
        extractedInfoIndexer.addRecord(result);

        extractedInfoIndexer.close();
    }
    
    /**
     * Test of retrieveDocInfo method, of class ExtractedInfoRetriever.
     */
    @Test
    public void testRetrieveDocInfo() throws Exception {
        
        try {
            System.out.println("Testing loading of per-document records from IE index");

            ExtractedInfoRetriever instance = new ExtractedInfoRetriever(propFile);
            String docName = instance.docReader.document(docId)
                        .get(ResearchDoc.FIELD_NAME);
            ResearchDoc result = instance.retrieveDocInfo(docName);
            
            assertNotNull(result);
            assertNotNull(result.title);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }

    /**
     * Test of retrieve method, of class ExtractedInfoRetriever.
     */
    @Test
    public void testRetrieve() throws Exception {
        try {
            System.out.println("Testing loading of per-document records from IE index");

            ExtractedInfoRetriever instance = new ExtractedInfoRetriever(propFile);
            String queryAttribId = instance.reader.document(docId)
                        .get(InformationUnit.ATTRIB_ID_FIELD);
            TopDocs result = instance.retrieve(queryAttribId);
            
            assertNotNull(result);
            assertEquals(1, result.scoreDocs.length);
            
            Document d = instance.reader.document(result.scoreDocs[0].doc);
            assertNotNull(d.get(InformationUnit.ATTRIB_ID_FIELD));
            assertTrue(d.get(InformationUnit.EXTRACTED_VALUE_FIELD).trim().length()>0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            fail(ex.getMessage());
        }
    }
}
