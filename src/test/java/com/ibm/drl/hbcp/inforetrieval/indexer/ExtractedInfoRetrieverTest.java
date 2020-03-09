/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.apache.lucene.document.Document;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.TopDocs;
import org.apache.tika.exception.TikaException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.drl.hbcp.extractor.InformationExtractor;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import com.ibm.drl.hbcp.extractor.PopulationMinAge;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import org.xml.sax.SAXException;

import java.io.IOException;

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
    public static void setUpClass() throws TikaException, ParseException, SAXException, IOException {
        System.out.println("Creating index before IE");
        docIndexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
        docIndexer.processAll();


        insertRecord();
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException {
        docIndexer.removeIndexDirs();
        extractedInfoIndexer.removeIndexDir(extractedInfoIndexer.getIndexDir());
    }
    
    static void insertRecord() throws ParseException, IOException {
        System.out.println("Inserting into IE index");

        try (InformationExtractor instance = new InformationExtractor(propFile)) {
            InformationUnit iu =
                    new PopulationMinAge(instance, ResearchDoc.FIELD_CONTENT,
                            AttributeType.POPULATION
                    );

            InformationUnit result = instance.extractInformationFromDoc(docId, iu);
            extractedInfoIndexer = new ExtractedInfoIndexer(BaseDirInfo.getPath("test.properties"));
            extractedInfoIndexer.addRecord(result);

            extractedInfoIndexer.close();
        }
    }
    
    /**
     * Test of retrieveDocInfo method, of class ExtractedInfoRetriever.
     */
    @Test
    public void testRetrieveDocInfo() throws ParseException, IOException {
        System.out.println("Testing loading of per-document records from IE index");

        try (ExtractedInfoRetriever instance = new ExtractedInfoRetriever(propFile)) {
            String docName = instance.docReader.document(docId)
                    .get(ResearchDoc.FIELD_NAME);
            ResearchDoc result = instance.retrieveDocInfo(docName);

            assertNotNull(result);
            assertNotNull(result.title);
        }
    }

    /**
     * Test of retrieve method, of class ExtractedInfoRetriever.
     */
    @Test
    public void testRetrieve() throws Exception {
        System.out.println("Testing loading of per-document records from IE index");

        try (ExtractedInfoRetriever instance = new ExtractedInfoRetriever(propFile)) {
            String queryAttribId = instance.reader.document(docId)
                    .get(InformationUnit.ATTRIB_ID_FIELD);
            TopDocs result = instance.retrieve(queryAttribId);

            assertNotNull(result);
            assertEquals(1, result.scoreDocs.length);

            Document d = instance.reader.document(result.scoreDocs[0].doc);
            assertNotNull(d.get(InformationUnit.ATTRIB_ID_FIELD));
            assertTrue(d.get(InformationUnit.EXTRACTED_VALUE_FIELD).trim().length()>0);
        }
    }
}
