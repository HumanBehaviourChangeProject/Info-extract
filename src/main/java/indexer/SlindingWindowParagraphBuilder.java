/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

/**
 * Contains utility classes for splitting a document into fixed length word
 * windows.
 * 
 * @author Debasis
 */

public class SlindingWindowParagraphBuilder {
    
    int paraWindowSize;
    Analyzer analyzer;

    public SlindingWindowParagraphBuilder(int paraWindowSize, Analyzer analyzer) {
        this.paraWindowSize = paraWindowSize;
        this.analyzer = analyzer;                
    }
        
    // the window size in number of words
    public List<Paragraph> constructParagraphs(int docId, String content) throws Exception {
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
                Paragraph p = new Paragraph(docId + "_" + String.valueOf(id++), tokens);
                tokens.clear();
                count = 0;
                splitAndAddParagraph(parList, p);
            }
        }
        if (count > 0) {
            Paragraph p = new Paragraph(docId + "_" + String.valueOf(id++), tokens);            
            splitAndAddParagraph(parList, p);
        }

        stream.end();
        stream.close();
        
        return parList;
    }

    // Split a paragraph if an EOS token is found in it.
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
