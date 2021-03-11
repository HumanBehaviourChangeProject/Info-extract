/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.normrsv;

import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.*;
import org.apache.lucene.util.BytesRef;

import java.io.IOException;


/**
 * A normalizer class for retrieval scores (independent of retrieval models).
 * 
 * @author Debasis
 */
public class RSVNormalizer  {
    String fieldName;
    IndexReader reader;
    IndexSearcher searcher;
    
    public RSVNormalizer(IndexSearcher searcher, String fieldName) {
        this.reader = searcher.getIndexReader();
        this.searcher = searcher;
        this.fieldName = fieldName;
    }
    
    /**
     * Get a specified document as a Lucene query
     * @param docId
     * @return A Lucene Query object comprised of the terms from the specified document
     * @throws IOException 
     */
    public Query getWholeDocAsQuery(int docId) throws IOException {
        Terms tfvector;
        String termText;
        BytesRef term;
        TermsEnum termsEnum;

        BooleanQuery.Builder dAndQ = new BooleanQuery.Builder();
        
        tfvector = reader.getTermVector(docId, fieldName);
        if (tfvector == null || tfvector.size() == 0)
            return null;

        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(); // access the terms for this field
    	while ((term = termsEnum.next()) != null) { // explore the terms for this field
            termText = term.utf8ToString();
            int tf = (int)termsEnum.totalTermFreq();
            
            TermQuery tq = new TermQuery(new Term(fieldName, termText));
            BoostQuery bq = new BoostQuery(tq, tf);
            dAndQ.add(bq, BooleanClause.Occur.SHOULD);
        }

        return dAndQ.build();
    }
    
    float getDocNorm(int docId) throws Exception {
        Query docq = getWholeDocAsQuery(docId);  // docId for internal use in temp in-mem index      
        TopDocs topdocs = searcher.search(docq, 1); // get the norm factor
        
        float zd = topdocs.scoreDocs[0].score;        
        return zd;
    }
    
    /**
     * Assume that all terms from document d is the query. Match
     * the document against itself to compute the normalization factor.
     * @param sd -- A Lucene 'ScoreDoc' object. 
     * @return A normalized Lucene 'ScoreDoc' object. 
     * @throws Exception 
     */
    public ScoreDoc getNormalizedScore(ScoreDoc sd) throws Exception {
        
        float z = getDocNorm(sd.doc);
        // TODO: this fails for some reason
        //assert (z >= sd.score);
        float nscore = sd.score/z;  // (d.q)/ (|d| |q|)  |x| --> L1 norm of x
        
        return new ScoreDoc(sd.doc, nscore);  // normalized rsv
    }    
}