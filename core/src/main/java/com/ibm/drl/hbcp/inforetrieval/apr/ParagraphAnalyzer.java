/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.FilteringTokenFilter;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.core.LowerCaseFilter;
import org.apache.lucene.analysis.en.PorterStemFilter;
import org.apache.lucene.analysis.standard.UAX29URLEmailTokenizer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.IOException;

/**
 *
 * @author dganguly
 */
public class ParagraphAnalyzer extends Analyzer {
    //CharArraySet stoplist;
    boolean stem;

    public ParagraphAnalyzer() {
        this(false);
    }

    public ParagraphAnalyzer(boolean stem) {
        this.stem = stem;
        /*
        stoplist = StopFilter.makeStopSet(
                    PaperIndexer.buildStopwordList(
                        this.getClass().getClassLoader().getResource("stop.txt").getPath()
                    )
        );
        */
    }

    @Override
    protected TokenStreamComponents createComponents(String string) {
        final Tokenizer tokenizer = new UAX29URLEmailTokenizer();

        TokenStream tokenStream = tokenizer;
        tokenStream = new LowerCaseFilter(tokenStream);
        //tokenStream = new StopFilter(tokenStream, stoplist);
        tokenStream = new ValidWordFilter(tokenStream); // remove words with digits
        if (stem)
            tokenStream = new PorterStemFilter(tokenStream);

        return new Analyzer.TokenStreamComponents(tokenizer, tokenStream);
    }

    public static void main(String[] args) {
        String test = "this is 1 test string";
        ParagraphAnalyzer a = new ParagraphAnalyzer();
        String t = PaperIndexer.analyze(a, test);
        System.out.println(t);
    }
}

// Removes tokens with any digit
class ValidWordFilter extends FilteringTokenFilter {

    CharTermAttribute termAttr = addAttribute(CharTermAttribute.class);

    public ValidWordFilter(TokenStream in) {
        super(in);
    }

    @Override
    protected boolean accept() throws IOException { // accept only words which doesn't have digits
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