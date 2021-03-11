/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Pattern;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import lombok.Data;

/**
 * This class is used to split a document into sentences instead of fixed
 * word-length paragraphs. Employs a rule-based sentence splitter.
 *
 * @author Debasis and Francesca
 */

public class SentenceBasedParagraphBuilder implements ParagraphBuilder {
    
    int paraNumberOfSentences;
    private final IndexingMethod indexingMethod;
    Analyzer analyzer;
   
    private static final String[] ABBREVIATIONS = {
        "Dr." , "Prof." , "Mr." , "Mrs." , "Ms." , "Jr." , "Ph.D.", "vs.","e.g.","i.e.",
    };
    
    private static boolean isAbbreviation(String a)  {
        return Arrays.asList(ABBREVIATIONS).contains(a);
    }

    public SentenceBasedParagraphBuilder(int paraNumberOfSentences, Analyzer analyzer) {
        this.paraNumberOfSentences = paraNumberOfSentences;
        indexingMethod = new SentenceBasedMethod();
        this.analyzer = analyzer;                
    }

    @Data
    public static class SentenceBasedMethod implements IndexingMethod {

        @Override
        public String toString() {
            return "sentence";
        }
    }

    /**
     * Constructs a list of sentences from a given document.
     * @param docId Id of a document from an index.
     * @param content The text content of the document stored in the Lucene index.
     * @return A list of 'Paragraph' objects constructed from this document text.
     */
    @Override
    public List<Paragraph> constructParagraphs(int docId, String content) throws IOException {
    	
        List<Paragraph> parList = new ArrayList<>();
        List<String> tokens = new ArrayList<>();
        
    	try (
			Analyzer analyzer = new WhitespaceAnalyzer();
			TokenStream stream = analyzer.tokenStream("dummy", new StringReader(content));
		) {

            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();

            int numSentences = 0;
            int id = 0;
            
            while (stream.incrementToken()) {
               
                String term = termAtt.toString();
                tokens.add(term);
                
                // check if there is end of sentence:
                boolean isEndOfSentence= Pattern.compile("[.!?]$").matcher(term).find();
                char lastLetter=term.charAt(0);
                if (term.length()>2) 
                	lastLetter= term.charAt(term.length()-2);
               
                if (isEndOfSentence && isLowerCase(lastLetter) && !isAbbreviation(term) )
                	numSentences++; 
                
                if (numSentences==paraNumberOfSentences){
                    // create a paragraph
                	numSentences++;
                    Paragraph p = new Paragraph(docId + "_" + String.valueOf(id++), tokens, indexingMethod);
                  //  System.out.println(tokens);
                    tokens.clear();
                    numSentences = 0;
                    splitAndAddParagraph(parList, p);
                }
            }
            
            
            //FB check this case:
            if (numSentences > 0) {
                Paragraph p = new Paragraph(docId + "_" + String.valueOf(id++), tokens, indexingMethod);
                splitAndAddParagraph(parList, p);
            }

            stream.end();

            // PT Note: Stream is now closed by Java (try-with-resources), there was a leak in analyzer
            // stream.close();
            
            return parList;
    	}
    	
    }
    
    /**
     * Splits a paragraph if an EOS token is found in it.
     * @param parList
     * @param p 
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
    
  
    
    private static boolean isLowerCase(char c)  {
    	//if is not a letter, I want to split (eg: the cat is on the (table).)
        if (Character.isLetter(c))
                //if it is letter AND it is upper  case I do not split (M. J. Adams)
         return c == Character.toLowerCase(c) && c != Character.toUpperCase(c);
        return true;
    }
    
}
