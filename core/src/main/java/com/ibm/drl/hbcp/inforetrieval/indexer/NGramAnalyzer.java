/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.ngram.NGramTokenizer;

/**
 * Class for splitting up a string into character n-gram tokens (not to be
 * confused with the 'ShingleAnalyzer' in the same package).
 * @author dganguly
 */
public class NGramAnalyzer extends Analyzer {
    int ngramSize;
    
    public NGramAnalyzer(int ngramSize) {
        this.ngramSize = ngramSize;
    }
    
    @Override
    protected TokenStreamComponents createComponents(String string) {
        final Tokenizer source = new NGramTokenizer(ngramSize, ngramSize);        
        TokenStream result = new LowerCaseFilter(source); // new NGramTokenFilter(source, ngramSize, ngramSize);
        return new TokenStreamComponents(source, result);
    }
    
}
