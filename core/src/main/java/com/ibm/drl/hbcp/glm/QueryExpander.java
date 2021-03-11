/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.glm;

import com.ibm.drl.hbcp.core.wvec.WordVec;
import com.ibm.drl.hbcp.core.wvec.WordVecs;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

class WordSimWeight {
    String key;
    float sim;
    int count;

    public WordSimWeight(String key) {
        this.key = key;
    }
}

/**
 * Expand an initial query by adding terms from the nearest neighbor set.
 * 
 * @author dganguly
 */
public class QueryExpander {
    WordVecs wvecs;
    List<String> queryTerms;
    int k;
    
    /**
     * Constructs an expander object for a given query.
     * 
     * @param k How many nearest neighbors to use for expansion
     * @param queryTerms List of query terms (strings)
     * @param wvecs Word vectors to use
     */
    public QueryExpander(int k, String[] queryTerms, WordVecs wvecs) {        
        this.queryTerms = new ArrayList<>();
        for (String qterm: queryTerms) {
            this.queryTerms.add(qterm);
        }
        
        this.wvecs = wvecs;
        this.k = k;
    }
    
    Query constructWightedQuery(HashMap<String, WordSimWeight> termWeights) {
        BooleanQuery.Builder q = new BooleanQuery.Builder();
        
        for (WordSimWeight wsimwt: termWeights.values()) {
            float avgsim = wsimwt.sim/(float)wsimwt.count;
            float avgdist = (float) Math.acos(avgsim);   // cos-inverse
            float wt = (float)Math.exp(-avgdist*avgdist);
            TermQuery tq = new TermQuery(new Term(ResearchDoc.FIELD_CONTENT, wsimwt.key));
            BoostQuery bq = new BoostQuery(tq, wt);
            q.add(bq, BooleanClause.Occur.SHOULD);
        }
        
        return q.build();
    }

    /**
     * Returns a Lucene Query comprised of the set of top-K most similar words
     * to the query terms.
     * @return A Lucene 'Query' object.
     */
    public Query getExpandedQuery() {
        HashMap<String, WordSimWeight> termWeights = new HashMap<>();
        
        // Copy the original query terms
        for (String qterm: queryTerms) {
            WordSimWeight wsimwt = new WordSimWeight(qterm);
            wsimwt.sim = 1;
            wsimwt.count = 1;
            termWeights.put(qterm, wsimwt);
        }
        
        for (String t: queryTerms) {
            List<Pair<WordVec, Double>> nnList = wvecs.getNearestNeighbors(t, k);
            if (nnList == null)
                continue;
            
            for (Pair<WordVec, Double> wordVecWithSim: nnList) {
                String key = wordVecWithSim.getKey().getWord();
                WordSimWeight wsimwt = termWeights.get(key);
                if (wsimwt == null) {
                    wsimwt = new WordSimWeight(key);
                    termWeights.put(key, wsimwt);
                }
                wsimwt.sim += wordVecWithSim.getValue();
                wsimwt.count++;
            } 
        }

        Query q = constructWightedQuery(termWeights);
        return q;
    }
}
