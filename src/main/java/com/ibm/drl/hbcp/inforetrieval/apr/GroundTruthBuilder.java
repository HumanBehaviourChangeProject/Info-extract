/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.

 * @author dganguly
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

import java.io.*;
import java.util.*;

/**
 * Refactored to use the class Attribute in package ref
 * 
 * @author dganguly procheta
 */

public class GroundTruthBuilder {

    List<ParagraphVec> pvecList;
    Properties prop;
    int numQueries; // num queries
    // use as queries for ranking evaluation
    int numBins;
    HashSet<ParagraphVec> queries;
    
    private static final int SEED = 123456;

    public GroundTruthBuilder(String propFileName) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(new File(propFileName)));
        
        numQueries = Integer.parseInt(prop.getProperty("apr.numqueries", "50"));
        numBins = Integer.parseInt(prop.getProperty("com.ibm.drl.hbcp.inforetrieval.apr.qrels.bins", "5"));
        pvecList = new ArrayList<>();
    }
    
    /* Call buildPerDocRefs for ALL annotated documents and then do the query selection.
       Otherwise the attributes for annotated docs won't be populated and sim computation will fail. 
    */
    public void build() throws Exception {
        // Load the list of per para attributes
        File indexDir = new File(prop.getProperty("apr.index"));
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        int numDocs = reader.numDocs();
        
        for (int i = 0; i < numDocs; i++) {  // iterate over passages
            ParagraphVec pvec = buildPerDocRefs(reader, i);
            if (pvec != null)
                pvecList.add(pvec);
        }
        
        buildQueries(reader);
        
        reader.close();
    }

    public HashSet<ParagraphVec> getQueries() { return queries; }
    
    void buildQueries(IndexReader reader) throws Exception {
        
        Collections.shuffle(pvecList, new Random(SEED));
        
        queries = new HashSet<>();
        
        // Take the first numQueries
        assert(numQueries < pvecList.size());
        int i = 0;
        
        for (ParagraphVec pvec: pvecList) {
            queries.add(pvec);  
            if (i++ == numQueries)
                break;
        }
    }
    
    ParagraphVec buildPerDocRefs(IndexReader reader, int i) throws Exception {
        Document doc = reader.document(i);
        
        boolean notAnnotated = Integer.parseInt(doc.get(ParagraphIndexer.APR_IS_ANNOTATED_PARAGRAPH_FIELD)) == 0;            
        if (notAnnotated)
            return null;

        String paraIdName = doc.get(ParagraphIndexer.APR_ID_FIELD);
        
        // use global document id (of Lucene) to identify this document (passage) in the index
        // paraIdName is of the form <docname>_<paragraph id within a doc> --- useful for debugging
        ParagraphVec perDocRefs = new ParagraphVec(i, paraIdName);
        
        String[] attrIdTokens = doc.get(ParagraphIndexer.APR_ATTRIB_ID_FIELD).split(ParagraphIndexer.ATTRIB_SEPARATOR_PATTERN);
        String[] attrTitleTokens = doc.get(ParagraphIndexer.APR_DOCTITLE_FIELD).split(ParagraphIndexer.ATTRIB_SEPARATOR_PATTERN);
        String[] attrValTokens = doc.get(ParagraphIndexer.APR_ATTRIB_VALUE_FIELD).split(ParagraphIndexer.ATTRIB_SEPARATOR_PATTERN);
        String[] attrContextTokens = doc.get(ParagraphIndexer.APR_ATTRIB_CONTEXT_FIELD).split(ParagraphIndexer.ATTRIB_SEPARATOR_PATTERN);
        assert(attrIdTokens.length == attrValTokens.length);
        
        for (int j=0; j < attrIdTokens.length; j++) {
            // TODO need attribute type
            Attribute att = new Attribute(attrIdTokens[j], null, "");
            // TODO the arm is empty right now, too
            ArmifiedAttributeValuePair a = new ArmifiedAttributeValuePair(att, attrValTokens[j], attrTitleTokens[j], "", attrContextTokens[j]);
            perDocRefs.addAttrib(a);
        }
        
        return perDocRefs;
    }
    
    /* compare with all */
    void getTop(ParagraphVec pvec) throws Exception {
        int n = pvecList.size();
        float sim;
        
        for (int i=0; i < n; i++) {
            ParagraphVec x = pvecList.get(i);
            sim = i==pvec.id? 1: pvec.cosineSim(x);
            x.setQuerySim(sim);
        }
        
        Collections.sort(pvecList);
    } 
    
    void reInitRelevanceList() { // restore default order
        for (ParagraphVec pvec: pvecList) {
            pvec.simWithQuery = 0;
        }
    }
    
    String writeRelevanceList(ParagraphVec qvec) throws Exception {

        StringBuffer buff = new StringBuffer();
        getTop(qvec);
        
        // leave out the query itself which is the top of the list
        int i = 0;
        
        for (ParagraphVec x: pvecList) {
            if (i > 0 && x.simWithQuery > 0)
                buff
                    .append(qvec.para_id)
                    .append("\t")
                    .append("Q0")
                    .append("\t")
                    .append(x.para_id)
                    .append("\t")
                    .append(x.simWithQuery)
                    .append("\n");
            
            i++;
        }
        
        reInitRelevanceList();  // reset the query sim values
        return buff.toString();
    }
    
    public void saveQrels() throws Exception {
        FileWriter fw = new FileWriter(new File(prop.getProperty("apr.qrels")));
        BufferedWriter bw = new BufferedWriter(fw);
        
        for (ParagraphVec pvec: queries) {  // compute and save qrels for each query
            String relList = writeRelevanceList(pvec);
            bw.write(relList);
        }
        
        bw.close();
        fw.close();
    }

    public static void main(String[] args) throws IOException, Exception {
        GroundTruthBuilder gtBuilder = new GroundTruthBuilder("init.properties");
        gtBuilder.build();
        gtBuilder.saveQrels();
    }
}
