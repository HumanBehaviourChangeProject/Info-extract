/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.inforetrieval.normrsv.RSVNormalizer;
import com.ibm.drl.hbcp.parser.JSONRefParser;

/**
 *
 * @author dganguly
 */
public class InformationExtractorTest {
    static PaperIndexer indexer;
    
    public InformationExtractorTest() {
    }
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        System.out.println("Creating index before IE");
        indexer = new PaperIndexer(BaseDirInfo.getPath("test.properties"));
        indexer.processAll();
    }
    
    @AfterClass
    public static void tearDownClass() throws IOException, Exception {
        indexer.removeIndexDirs();
    }
    
    /**
     * Test of extractInformation method, of class InformationExtractor.
     */
    @Test
    public void testExtractInformation() throws ParseException, IOException {
        System.out.println("Running test for InformationExtractor.extractInformation()");
        try (InformationExtractor instance = new InformationExtractor(BaseDirInfo.getPath("test.properties"))) {
            instance.extractInformation();
            instance.removeIEIndexFiles();
        }
    }

    /**
     * Test of loadGroundTruth method, of class InformationExtractor.
     */
    @Test
    public void testLoadGroundTruth() throws IOException {
        System.out.println("loadGroundTruth");
        try (InformationExtractor instance = new InformationExtractor(BaseDirInfo.getPath("test.properties"))) {
            instance.loadGroundTruth();
        }
    }

    /**
     * Test of buildParaIndex method, of class InformationExtractor.
     */
    @Test
    public void testBuildParaIndex() throws Exception {
        System.out.println("buildParaIndex");
        int refDocId = 0;
        try (InformationExtractor instance = new InformationExtractor(BaseDirInfo.getPath("test.properties"))) {
            Directory inMemParaIndex = instance.buildParaIndex(refDocId, 10);
            assertNotNull(inMemParaIndex);

            try (IndexReader paraReader = DirectoryReader.open(inMemParaIndex)) {
                assertNotEquals(paraReader.numDocs(), 0);
            }
        }
    }

    /**
     * Test of extractInformationFromDoc method, of class InformationExtractor.
     */
    @Test
    public void testExtractMinAge() throws ParseException, IOException {
        System.out.println("testExtractMinAge");
        int docId = 0;    // document should be Ames 2014.pdf

        try (InformationExtractor instance = new InformationExtractor(BaseDirInfo.getPath("test.properties"))) {
            instance.armification = false;
            InformationUnit iu =
                    new PopulationMinAge(instance, ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION);
            
            InformationUnit result = instance.extractInformationFromDoc(docId, iu);
            assertTrue(result.weight>0); // if 0 => some problem in setting the position based weights
            assertEquals("18", result.mostLikelyAnswer.getKey());
        }
    }

    /**
     * Test of extractInformationFromDoc method, of class InformationExtractor.
     */
    @Test
    public void testExtractBCT() throws ParseException, IOException {
        System.out.println("testExtractBCT");
        int docId = 0;

        final String GOAL_SETTING_BCT = "3673271";

        try (InformationExtractor instance = new InformationExtractor(
                BaseDirInfo.getPath("test.properties"))) {
            InformationUnit iu = new AbstractDetectPresenceAttribute(
                    instance, ResearchDoc.FIELD_CONTENT, AttributeType.INTERVENTION, GOAL_SETTING_BCT);

            InformationUnit result = instance.extractInformationFromDoc(docId, iu);

            System.out.println("Similarity of best matching passage: " + result.weight);
            assertTrue(result.weight>0 && result.weight<=1); // if 0 => some problem in setting the position based weights
        }
    }
    
    /**
     * Test of extractInformationFromDoc method, of class InformationExtractor.
     */
    @Test
    public void testScoreNormalization() throws ParseException, IOException {
        System.out.println("testScoreNormalization");
        int docId = 0;

        final String GOAL_SETTING_BCT = "3673271";

        try (InformationExtractor instance = new InformationExtractor(BaseDirInfo.getPath("test.properties"))) {
            InformationUnit[] testIUs = new InformationUnit[2];
            testIUs[0] = new AbstractDetectPresenceAttribute(
                    instance, ResearchDoc.FIELD_CONTENT, AttributeType.INTERVENTION,
                    GOAL_SETTING_BCT);

            testIUs[1] = new PopulationMinAge(instance, ResearchDoc.FIELD_CONTENT,
                    AttributeType.POPULATION
            );

            for (InformationUnit iu: testIUs) {
                String docName = instance.reader.document(docId).get(ResearchDoc.FIELD_NAME);
                iu.reInit(docName);

                // +++TODO: Move out of the loop for increasing efficiency
                Directory inMemParaIndex = instance.buildParaIndex(docId, 10);
                IndexReader paraReader = DirectoryReader.open(inMemParaIndex);

                Query q = iu.constructQuery();

                IndexSearcher paraSearcher = new IndexSearcher(paraReader);
                Similarity sim = new LMJelinekMercerSimilarity(0.6f);
                paraSearcher.setSimilarity(sim);

                TopDocs topDocs = paraSearcher.search(q, iu.numWanted());

                // Normalize the retrieval model scores to ensure that we don't need to
                // compute cosine similarity inside the construct() function
                // of AbstractDetectPresenceAttribute.
                RSVNormalizer rsvn = new RSVNormalizer(paraSearcher, ResearchDoc.FIELD_CONTENT);
                for (ScoreDoc sd : topDocs.scoreDocs) {
                    Query docQ = rsvn.getWholeDocAsQuery(sd.doc);  // docId for internal use in temp in-mem index
                    assertNotNull(docQ);        // Query must not be null

                    TopDocs ntopdocs = paraSearcher.search(q, 1); // get the norm factor
                    assertTrue(ntopdocs.scoreDocs.length>0);

                    float z = ntopdocs.scoreDocs[0].score;
                    assertTrue(z >= sd.score);
                }
            }
        };
    }
}
