/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import lombok.Data;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.quartz.PersistJobDataAfterExecution;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Contains utility classes for splitting a document into fixed length word
 * windows.
 * @author Debasis
 */
public class SlidingWindowParagraphBuilder implements ParagraphBuilder {
    
    int paraWindowSize;
    private final IndexingMethod indexingMethod;
    Analyzer analyzer;

    public SlidingWindowParagraphBuilder(int paraWindowSize, Analyzer analyzer) {
        this.paraWindowSize = paraWindowSize;
        indexingMethod = new SlidingWindowMethod(paraWindowSize);
        this.analyzer = analyzer;                
    }

    @Data
    public static class SlidingWindowMethod implements IndexingMethod {
        private final int windowSize;

        @Override
        public String toString() {
            return "window=" + getWindowSize();
        }
    }
        
    /**
     * Constructs a paragraph for a given document identifier and the content
     * @param docId
     * @param content
     * @return
     * @throws IOException 
     */
    @Override
    public List<Paragraph> constructParagraphs(int docId, String content) throws IOException {
        List<Paragraph> parList = new ArrayList<>();
        
        List<String> tokens = new ArrayList<>();
        Analyzer analyzer = new WhitespaceAnalyzer();
        TokenStream stream = analyzer.tokenStream("dummy", new StringReader(content));
        CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
        stream.reset();

        int count = 0;
        int id = 0;
        while (stream.incrementToken()) {
            String term = termAtt.toString();
            tokens.add(term);
            count++;
            if (count == paraWindowSize) {
                // create a paragraph
                Paragraph p = new Paragraph(docId + "_" + String.valueOf(id++), tokens, indexingMethod);
                tokens.clear();
                count = 0;
                splitAndAddParagraph(parList, p);
            }
        }
        if (count > 0) {
            Paragraph p = new Paragraph(docId + "_" + String.valueOf(id++), tokens, indexingMethod);
            splitAndAddParagraph(parList, p);
        }

        stream.end();
        stream.close();
        
        return parList;
    }
    
    /**
     * Splits a paragraph if an EOS token is found in it.
     * @param parList Fills up this list object (output parameter)
     * @param p Given passage
     */
    void splitAndAddParagraph(List<Paragraph> parList, Paragraph p) {
        int start = 0;
        int indexOfEOS;
        String pattern = PaperIndexer.EOS;
        int plen = pattern.length();
        int count = 0;
        Paragraph split_p = null;
        
        while ((indexOfEOS = p.content.indexOf(pattern, start)) > -1) {
            if (indexOfEOS > 0) {
                split_p = new Paragraph(p.id + "_" + String.valueOf(count), p.content.substring(start, indexOfEOS));
                start = indexOfEOS + plen;
                parList.add(split_p);
            }
            count++;
        }
        if (count == 0)
            parList.add(p);
        else
            parList.add(new Paragraph(p.id + "_" + String.valueOf(count), p.content.substring(start)));
    }
}
