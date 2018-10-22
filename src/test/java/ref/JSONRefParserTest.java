/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ref;

import java.io.File;
import java.io.FileReader;
import java.net.URL;
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
public class JSONRefParserTest {
    
    static final String PROP_FILE = "init.properties";
    
    public JSONRefParserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() {
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of loadCodeSets method, of class JSONRefParser.
     */
    @Test
    public void testLoadCodeSets() {
        
        try {
            System.out.println("Testing loading of codesets");

            for (int i=0; i < JSONRefParser.CodeSetTreeNames.length; i++) {

                JSONRefParser instance = new JSONRefParser(PROP_FILE);
                instance.parse();
                instance.trees[i]=instance.loadCodeSet(i);

                assertNotNull(instance.trees[i]);
                assertNotNull(instance.trees[i].root);
                assertNotNull(instance.trees[i].root.children);
                assertTrue("Tree should have at least one child node", instance.trees[i].root.children.size() > 0);
            }
        }
        catch (Exception ex) {
            fail(ex.getMessage());            
        }
    }

    @Test
    public void testLoadCodeSetsFromURL() {
        
        try {
            System.out.println("Testing loading of codesets from URL resource");

            URL refJsonUrl = JSONRefParser.class.getClassLoader().
                            getResource("Sprint1_Codeset1.json");
            
            JSONRefParser instance = new JSONRefParser();
            instance.buildCodeSetsFromURL(refJsonUrl);

            for (int i=JSONRefParser.POPULATION; i<=JSONRefParser.OUTCOME; i++) {
                assertNotNull(instance.trees[i]);
                assertNotNull(instance.trees[i].root);
                assertNotNull(instance.trees[i].root.children);
                assertTrue("Tree should have at least one child node",
                        instance.trees[i].root.children.size() > 0);
            }
        }
        catch (Exception ex) {
            fail(ex.getMessage());            
        }
    }
    
    /**
     * Test of groupByDocs method, of class JSONRefParser.
     */
    @Test
    public void testGroupByDocs() {
        
        try {
            System.out.println("Testing loading of reference values...");
            
            for (int i=0; i < JSONRefParser.CodeSetTreeNames.length; i++) {

                JSONRefParser instance = new JSONRefParser(PROP_FILE);
                instance.parse();
                instance.trees[i]=instance.loadCodeSet(i);
                instance.groupByDocs(instance.trees[i]);

                assertNotNull(instance.trees[i].cache);
                assertTrue("No annotated values loaded", instance.trees[i].cache.size() > 0);
            }
        }
        catch (Exception ex) {
            fail(ex.getMessage());            
        }
        
    }
    
    /**
     * Test of merging json annotation files
     */
    @Test
    public void testConvertCode1Annotation2Code2() {
        
        try {
            System.out.println("Testing convert codeset1 to codeset 2...");
            JSONRefParser instance = new JSONRefParser(PROP_FILE);
            instance.parse();
            instance.trees[0]=instance.loadCodeSet(0);
            instance.groupByDocs(instance.trees[0]);
            //new merged json file does't contain the minAge(3587807) from codeset1
            assertNull(instance.trees[0].getNode("3587807"));
        }
        catch (Exception ex) {
            fail(ex.getMessage());            
        }
        
    }


    

}
