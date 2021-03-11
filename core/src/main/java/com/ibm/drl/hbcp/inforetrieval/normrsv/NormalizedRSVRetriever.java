/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.normrsv;

import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TotalHits;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

/**
 *
 * @author dganguly
 */

class ScoreDocComparator implements Comparator<ScoreDoc> {

    @Override
    public int compare(ScoreDoc thisObj, ScoreDoc thatObj) {
        return Float.compare(thatObj.score, thisObj.score);  // descending
    }
}

/**
 * Utility class to normalize the retrieval scores of a list of documents
 * irrespective of the retrieval similarity model used.
 * @author dganguly
 */
public class NormalizedRSVRetriever {
    IndexSearcher searcher;
    String fieldName;
    RSVNormalizer rsvn;
    Logger logger;
    
    /**
     * 
     * @param searcher Lucene 'IndexSearcher' object.
     * @param fieldName The fieldName which stores the terms.
     */
    public NormalizedRSVRetriever(IndexSearcher searcher, String fieldName) {
        this.searcher = searcher;
        this.fieldName = fieldName;
        rsvn = new RSVNormalizer(searcher, fieldName);
        
        logger = (Logger)LoggerFactory.getLogger(NormalizedRSVRetriever.class);
    }
    
    String topDocsToString(TopDocs topDocs) {
        StringBuffer buff = new StringBuffer();
        for (ScoreDoc sd: topDocs.scoreDocs) {
            buff.append("(").append(sd.doc).append(",").append(sd.score).append(") ");
        }
        return buff.toString();
    }
    
    /**
     * Reranks a list of retrieved documents post normalization.
     * @param topDocs The top docs to rerank
     * @return 
     */
    public TopDocs rerank(TopDocs topDocs) {
        ArrayList<ScoreDoc> rerankedSDList = new ArrayList<>();
        
        for (ScoreDoc sd : topDocs.scoreDocs) {
            try {
                ScoreDoc nsd = rsvn.getNormalizedScore(sd);
                rerankedSDList.add(nsd);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        Collections.sort(rerankedSDList, new ScoreDocComparator());
        
        ScoreDoc[] newsd = new ScoreDoc[rerankedSDList.size()];
        newsd = rerankedSDList.toArray(newsd);
        
        TopDocs rerankedTopDocs = new TopDocs(new TotalHits(newsd.length, TotalHits.Relation.EQUAL_TO), newsd);
        return rerankedTopDocs;
    }
    
}
