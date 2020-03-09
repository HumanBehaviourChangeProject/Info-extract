/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.Closeable;
import java.io.File;
import java.io.IOException;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.drl.hbcp.extractor.InformationExtractor;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import com.ibm.drl.hbcp.extractor.PopulationMinAge;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import org.xml.sax.SAXException;

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
    public static void setUpClass() throws TikaException, SAXException, IOException {
        // Set up the test environment by creating small doc and extracted info index
        System.out.println("Creating index before IE");
        docIndexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
        docIndexer.processAll();

        extractedInfoIndexer = new ExtractedInfoIndexer(BaseDirInfo.getPath("test.properties"));
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException {
        // Clear everything that we created.
        docIndexer.removeIndexDirs();
        extractedInfoIndexer.removeIndexDir(extractedInfoIndexer.getIndexDir());
    }
    
    /**
     * Test of addRecord method, of class ExtractedInfoIndexer.
     */
    @Test
    public void testAddRecord() throws ParseException, IOException {
        System.out.println("Testing saving of extracted information");

        String propFile = BaseDirInfo.getPath("test.properties");
        try (InformationExtractor instance = new InformationExtractor(propFile)) {
            InformationUnit iu =
                    new PopulationMinAge(instance, ResearchDoc.FIELD_CONTENT,
                            AttributeType.POPULATION
                    );
            int docId = 0;  // points to Ames 2014.pdf (which should extract minimum age)

            InformationUnit result = instance.extractInformationFromDoc(docId, iu);
            extractedInfoIndexer.addRecord(result);

            extractedInfoIndexer.close();

            File indexDir = new File(extractedInfoIndexer.getIndexDir());
            assertNotNull(indexDir);
            assertTrue(indexDir.exists());
            assertTrue(indexDir.listFiles().length>0);
        };
    }

}
