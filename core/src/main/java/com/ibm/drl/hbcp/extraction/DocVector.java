/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extraction;

import com.ibm.drl.hbcp.inforetrieval.indexer.NGramAnalyzer;
import com.ibm.drl.hbcp.inforetrieval.indexer.ShingleAnalyzer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

class TermFreq {
    String term;
    int freq;    
}

/**
 * Defines a sparse vector representation of a document in terms of a list
 * of (term, wt) pairs.
 * Useful for various utilities such as finding lengths, cosine similarities etc.
 *
 * @author dganguly
 */
public class DocVector {
    String text;
    HashMap<String, TermFreq> tfMap;

    private void init(String[] retrievedTerms) {
        tfMap = new HashMap<>();
        for (String term : retrievedTerms) {
            TermFreq tf = tfMap.get(term);
            if (tf == null) {
                tf = new TermFreq();
                tf.term = term;
                tf.freq = 0;
            }
            tf.freq++;
            tfMap.put(term, tf);
        }
    }

    public DocVector(String text, int ngramSize) {
        this.text = text;
        String[] retrievedTerms = analyze(new NGramAnalyzer(ngramSize), text);            
        init(retrievedTerms);
    }
    
    public String getText() { return text; }
    
    public DocVector(Properties prop, String text) {
        this.text = text;
        String[] retrievedTerms = analyze(new ShingleAnalyzer(prop), text);
        init(retrievedTerms);
    }

    /**
     * Tokenizes a piece of 'text' (parameter) into a list of tokens
     * after removing stopwords and stemming (as specified by the parameter
     * analyzer).
     * @param analyzer Analyzer object as passed to this function, e.g. EnglishAnalyzer
     * @param text A piece of text
     * @return An array of tokens (String objects)
     */
    public String[] analyze(Analyzer analyzer, String text) {
        
        List<String> buff = new ArrayList<>();
        try {
            TokenStream stream = analyzer.tokenStream("dummy", new StringReader(text));
            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                String term = termAtt.toString();
                buff.add(term);
            }
            stream.end();
            stream.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        
        String[] buffArray = new String[buff.size()];
        return buff.toArray(buffArray);
    }
    
    float docLen() {
        float len = 0;
        for (TermFreq tf : tfMap.values()) {
            len += (tf.freq*tf.freq);
        }
        return (float)Math.sqrt(len);
    }
    
    /**
     * Computes similarity of this document object (bag of words)
     * with a list of query terms
     * @param queryTerms
     * @return 
     */
    public float cosineSimWithQuery(String[] queryTerms) {
        float sim = 0;
        float dLen = docLen();
        float qLen = (float)Math.sqrt(queryTerms.length);
        
        for (String q: queryTerms) {
            TermFreq tf = tfMap.get(q);
            if (tf == null)
                continue;
            sim += tf.freq;
        }
        return sim/(dLen*qLen);
    }
    
    /**
     * Computes similarity with another 'Document' object.
     * @param that
     * @return 
     */
    public float cosineSim(DocVector that) {
        float sim = 0;
        float dLen = docLen();
        float qLen = that.docLen();
        
        for (TermFreq tf: tfMap.values()) {
            TermFreq that_tf = that.tfMap.get(tf.term);
            if (that_tf == null)
                continue;
            sim += tf.freq * that_tf.freq;
        }
        return dLen==0 || qLen==0? 0 : sim/(dLen*qLen);
    }
    
    /**
     * Computes the (<a href="https://en.wikipedia.org/wiki/METEOR">METEOR</a>) score
     * between this document text and 'that' (parameter) Document object.
     * Used in the evaluation flow.
     * @param that
     * @return 
     */
    public float computeMETEOR(DocVector that) {
        float prec = computeBLEU(that);
        float recall = computeROUGE(that);
        float denom = prec + recall;
        return denom==0? 0 : 2*prec*recall/(prec+recall);
    }
    
    // this is the predicted and that is the reference
    public float computeBLEU(DocVector that) {
        int tp = 0, fp = 0;
        for (TermFreq tf: tfMap.values()) {
            TermFreq that_tf = that.tfMap.get(tf.term);
            if (that_tf == null) {
                // this is a false-positive error
                fp++;
            }
            else {
                tp++;
            }
        }
        float denom = tp+fp;
        return denom==0? 0 : tp/denom;
    }
    
    float computeROUGE(DocVector that) {
        int tp = 0, fn = 0;
        for (TermFreq tf: that.tfMap.values()) {
            TermFreq that_tf = this.tfMap.get(tf.term);
            if (that_tf == null) {
                // this is a false-negative error
                fn++;
            }
            else {
                tp++;
            }
        }
        float denom = tp+fn;
        return denom==0? 0 : tp/denom;
    }    
    
    /**
     * Returns a string representation of the set of terms and weights.
     * @return 
     */
    public String toString() {
        StringBuffer buff = new StringBuffer("[");
        
        for (TermFreq tf : this.tfMap.values()) {
            buff.append(tf.term).append(":").append(tf.freq).append(" ");
        }
        buff.append("]");
        
        return buff.toString();
    }    
}

