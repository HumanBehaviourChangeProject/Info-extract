/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.parser;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;
import java.util.stream.Collectors;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.util.Props;

/**
 * Unit tests designed for the old JSON parser, mostly processing real JSON data.
 * The tests should still be passed by the new JSON parser.
 *
 * @author dganguly
 */
public class OldJSONRefParserTest {
    private static Properties props;

    public OldJSONRefParserTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws IOException {
        props = Props.loadProperties("init.properties");
        // TODO: this test is only for the non-armified parser
        props.setProperty("armification", "false");
    }
    
    @AfterClass
    public static void tearDownClass() {
    }

    /**
     * Test of loadCodeSets method, of class JSONRefParser.
     */
    @Test
    public void testLoadCodeSets() throws IOException {
        System.out.println("Testing loading of codesets");

        for (int i : Arrays.stream(AttributeType.getSprint1234Types()).map(AttributeType::code).collect(Collectors.toList())) {

            JSONRefParser instance = new JSONRefParser(props);
            instance.parse();
            instance.trees[i]=instance.loadCodeSet(i);

            assertNotNull(instance.trees[i]);
            assertNotNull(instance.trees[i].root);
            assertNotNull(instance.trees[i].root.children);
            assertTrue("Tree should have at least one child node", instance.trees[i].root.children.size() > 0);
        }
    }

    /*
    @Test
    public void testLoadCodeSetsFromURL() throws IOException, URISyntaxException {
        
        System.out.println("Testing loading of codesets from URL resource");

        JSONRefParser instance = new JSONRefParser(FileUtils.potentiallyGetAsResource(new File("jsons/Smoking_AllAnnotations_01Apr19.json")));

        for (int i = 0; i < AttributeType.values().length; i++) {
            System.out.println("Testing tree " + i);
            if(i==8) break; // ignore the "New Prioritised Codeset" here
            assertNotNull(instance.trees[i]);
            assertNotNull(instance.trees[i].root);
            assertNotNull(instance.trees[i].root.children);
            assertTrue("Tree " + i + " should have at least one child node",
                    instance.trees[i].root.children.size() > 0);
        }
    }
    */
    
    /**
     * Test of groupByDocs method, of class JSONRefParser.
     */
    @Test
    public void testGroupByDocs() throws IOException {
        System.out.println("Testing loading of reference values...");

        for (int i : Arrays.stream(AttributeType.getSprint1234Types()).map(AttributeType::code).collect(Collectors.toList())) {

            JSONRefParser instance = new JSONRefParser(props);
            instance.parse();
            instance.trees[i]=instance.loadCodeSet(i);
            instance.groupByDocs(instance.trees[i]);

            assertNotNull(instance.trees[i].cache);
            assertTrue("No annotated values loaded", instance.trees[i].cache.size() > 0);
        }
    }
    
    /**
     * Test of merging json annotation files
     */
    @Test
    public void testConvertCode1Annotation2Code2() throws IOException {
        System.out.println("Testing convert codeset1 to codeset 2...");
        JSONRefParser instance = new JSONRefParser(props);
        instance.parse();
        instance.trees[0]=instance.loadCodeSet(0);
        instance.groupByDocs(instance.trees[0]);
        //new merged json file does't contain the minAge(3587807) from codeset1
        assertNull(instance.trees[0].getNode("3587807"));
    }
}
