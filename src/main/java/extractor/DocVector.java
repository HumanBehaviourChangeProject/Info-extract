/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import indexer.ShingleAnalyzer;
import java.io.*;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;

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
    HashMap<String, TermFreq> tfMap;

    public DocVector(Properties prop, String text) {
        tfMap = new HashMap<>();
        String[] retrievedTerms = analyze(new ShingleAnalyzer(prop), text);
        
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
    
    // Sorted array of query and document terms
    float cosineSimWithQuery(String[] queryTerms) {
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
    
    float cosineSim(DocVector that) {
        float sim = 0;
        float dLen = docLen();
        float qLen = that.docLen();
        
        for (TermFreq tf: tfMap.values()) {
            TermFreq that_tf = that.tfMap.get(tf.term);
            if (that_tf == null)
                continue;
            sim += tf.freq * that_tf.freq;
        }
        return sim/(dLen*qLen);
    }
    
    float computeMETEOR(DocVector that) {
        float prec = computeBLEU(that);
        float recall = computeROUGE(that);
        float denom = prec + recall;
        return denom==0? 0 : 2*prec*recall/(prec+recall);
    }
    
    // this is the predicted and that is the reference
    float computeBLEU(DocVector that) {
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
}

