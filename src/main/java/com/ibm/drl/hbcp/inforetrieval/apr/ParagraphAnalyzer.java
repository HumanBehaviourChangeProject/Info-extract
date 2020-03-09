/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.FilteringTokenFilter;

import java.io.IOException;

/**
 *
 * @author dganguly
 */
public class ParagraphAnalyzer extends Analyzer {
    CharArraySet stoplist;

    ParagraphAnalyzer() {
        stoplist = StopFilter.makeStopSet(
                    PaperIndexer.buildStopwordList(
                        this.getClass().getClassLoader().getResource("stop.txt").getPath()
                    )
        );
    }

    @Override
    protected TokenStreamComponents createComponents(String string) {
        final Tokenizer tokenizer = new UAX29URLEmailTokenizer();

        TokenStream tokenStream = tokenizer;
        tokenStream = new LowerCaseFilter(tokenStream);
        tokenStream = new StopFilter(tokenStream, stoplist);
        tokenStream = new ValidWordFilter(tokenStream); // remove words with digits
        tokenStream = new PorterStemFilter(tokenStream);

        return new Analyzer.TokenStreamComponents(tokenizer, tokenStream);
    }
}

// Removes tokens with any digit
class ValidWordFilter extends FilteringTokenFilter {

    CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public ValidWordFilter(TokenStream in) {
        super(in);
    }
   
    @Override
    protected boolean accept() throws IOException {
        String token = termAttr.toString();
        int len = token.length();
        for (int i=0; i < len; i++) {
            char ch = token.charAt(i);
            if (Character.isDigit(ch))
                return false;
            if (ch == '.')
                return false;
        }
        return true;
    } 
}
