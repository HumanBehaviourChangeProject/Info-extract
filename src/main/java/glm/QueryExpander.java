/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package glm;

import indexer.ResearchDoc;
import java.util.*;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import wvec.WordVec;
import wvec.WordVecs;

/**
 * Expand an initial query by adding terms from the nearest neighbor set.
 * 
 * @author dganguly
 */

class WordSimWeight {
    String key;
    float sim;
    int count;

    public WordSimWeight(String key) {
        this.key = key;
    }
}

public class QueryExpander {
    WordVecs wvecs;
    List<String> queryTerms;
    int k;
    
    public QueryExpander(int k, String[] queryTerms, WordVecs wvecs) {        
        this.queryTerms = new ArrayList<>();
        for (String qterm: queryTerms) {
            this.queryTerms.add(qterm);
        }
        
        this.wvecs = wvecs;
        this.k = k;
    }
    
    Query constructWightedQuery(HashMap<String, WordSimWeight> termWeights) {
        BooleanQuery q = new BooleanQuery();
        
        for (WordSimWeight wsimwt: termWeights.values()) {
            float avgsim = wsimwt.sim/(float)wsimwt.count;
            float avgdist = (float) Math.acos(avgsim);   // cos-inverse
            float wt = (float)Math.exp(-avgdist*avgdist);
            TermQuery tq = new TermQuery(new Term(ResearchDoc.FIELD_CONTENT, wsimwt.key));
            tq.setBoost(wt);
            q.add(tq, BooleanClause.Occur.SHOULD);
        }
        
        return q;
    }
    
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
            List<WordVec> nnList = wvecs.getNearestNeighbors(t, k);
            if (nnList == null)
                continue;
            
            for (WordVec wv: nnList) {
                String key = wv.getWord();
                WordSimWeight wsimwt = termWeights.get(key);
                if (wsimwt == null) {
                    wsimwt = new WordSimWeight(key);
                    termWeights.put(key, wsimwt);
                }
                wsimwt.sim += wv.getQuerySim();
                wsimwt.count++;
            } 
        }

        Query q = constructWightedQuery(termWeights);
        return q;
    }
}
