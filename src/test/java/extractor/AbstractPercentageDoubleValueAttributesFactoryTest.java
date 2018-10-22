/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import static extractor.InformationExtractorTest.indexer;
import indexer.BaseDirInfo;
import indexer.PaperIndexer;
import java.util.List;
import org.junit.AfterClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.junit.BeforeClass;

/**
 *
 * @author dganguly
 */
public class AbstractPercentageDoubleValueAttributesFactoryTest {
    
    public AbstractPercentageDoubleValueAttributesFactoryTest() {
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
     * Test of createInformationUnits method, of class AbstractDetectPresenceAttributesFactory.
     */
    @Test
    public void testCreateInformationUnits() {
        try {
            System.out.println("Testing construction of TypeDetectAttribute objects");

            InformationExtractor extractor = new InformationExtractor(
                    BaseDirInfo.getPath("test.properties"));

            List<InformationUnit> valueObjs = AbstractPercentageDoubleValueAttributesFactory.createInformationUnits(extractor);
            
            for (InformationUnit iu: valueObjs) {
                PercentageDoubleValueUnit instance = (PercentageDoubleValueUnit)iu;
                assertNotNull(instance.numWanted());
                assertNotNull(instance.attribId);
                assertNotNull(instance.query);
            }
        }
        catch (Exception ex) {
            // TODO review the generated test code and remove the default call to fail.
            fail(ex.getMessage());
        }
    }
    
}
