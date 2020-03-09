/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.extractor.TopTermsExtractor;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.WeightedTerm;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dganguly
 */
public class BoWQueryFormulator implements QueryFormulator {

    String text;
    Retriever retriever;
    int nTopTerms;
    
    public BoWQueryFormulator(String text, Retriever retriever) {
        this.text = PaperIndexer.analyze(retriever.analyzer, text);
        this.retriever = retriever;
        this.nTopTerms = retriever.numTopTerms();
    }

    @Override
    public Query constructQuery() throws Exception {
        BooleanQuery.Builder bq = new BooleanQuery.Builder();
        
        List<String> textArray = new ArrayList<>(1);
        textArray.add(text);
        
        TopTermsExtractor termsExtractor = new TopTermsExtractor(textArray, retriever.getAnalyzer(), retriever.getReader(), ParagraphIndexer.APR_PARA_CONTENT_FIELD);
        List<WeightedTerm> weightedTermList = termsExtractor.computeTopTerms(nTopTerms, 0.4f);

        for (WeightedTerm wt : weightedTermList) {
            Query termQuery = new TermQuery(new Term(ParagraphIndexer.APR_PARA_CONTENT_FIELD, wt.getTerm()));
            BoostQuery boostQuery = new BoostQuery(termQuery, wt.getWeight());
            bq.add(new BooleanClause(boostQuery, BooleanClause.Occur.SHOULD));
        }
        
        return bq.build();
    }
    
}
