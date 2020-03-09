/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import static com.ibm.drl.hbcp.extractor.InformationExtractorTest.indexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;

import java.io.IOException;
import java.util.List;

import org.apache.tika.exception.TikaException;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.xml.sax.SAXException;

/**
 *
 * @author dganguly
 */
public class AbstractPercentageDoubleValueAttributesFactoryTest {
    
    public AbstractPercentageDoubleValueAttributesFactoryTest() {
    }

    @BeforeClass
    public static void setUpClass() throws IOException, TikaException, SAXException, Exception {
        System.out.println("Creating index before IE");
        indexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
        indexer.processAll();
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException, Exception {
        indexer.removeIndexDirs();
    }
        
    /**
     * Test of createInformationUnits method, of class AbstractDetectPresenceAttributesFactory.
     */
    @Test
    public void testCreateInformationUnits() throws IOException {
        System.out.println("Testing construction of TypeDetectAttribute objects");

        try (InformationExtractor extractor = new InformationExtractor(
                BaseDirInfo.getPath("test.properties"))) {
            List<InformationUnit> valueObjs = AbstractPercentageDoubleValueAttributesFactory.createInformationUnits(extractor);

            for (InformationUnit iu: valueObjs) {
                PercentageDoubleValueUnit instance = (PercentageDoubleValueUnit)iu;
                assertNotNull(instance.numWanted());
                assertNotNull(instance.attribId);
                assertNotNull(instance.query);
            }
        }
    }
    
}
