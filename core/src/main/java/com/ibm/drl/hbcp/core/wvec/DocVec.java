/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;

import java.util.HashMap;

/**
 * Represents each document as a set of cluster points.
 * @author Debasis
 */
public class DocVec {
    ClusteredWordVecs wvecs;
    HashMap<String, WordVec> wvecMap;
    
    // This constructor is to be called during retrieval when we want
    // to construct a vector representation of the query terms
    public DocVec(ClusteredWordVecs wvecs, Query query) throws Exception {
        this.wvecs = wvecs;
        
        WeightedTerm[] qterms = QueryTermExtractor.getTerms(query);        
        wvecMap = new HashMap<>();
        
        for (WeightedTerm wterm: qterms) {
            WordVec qwv = wvecs.getVec(wterm.getTerm());
            if (qwv != null) {
                qwv.setClusterId(wvecs.getClusterId(qwv.getWord()));
                wvecMap.put(qwv.getWord(), qwv);                
            }
        }
    }
    
    public HashMap<String, WordVec> getWordVecMap() {
        return wvecMap;
    }
}
