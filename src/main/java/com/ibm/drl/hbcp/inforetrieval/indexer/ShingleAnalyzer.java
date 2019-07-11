/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.Tokenizer;
import org.apache.lucene.analysis.ngram.NGramTokenFilter;
import org.apache.lucene.analysis.ngram.NGramTokenizer;
import org.apache.lucene.analysis.shingle.ShingleFilterFactory;
import org.apache.lucene.analysis.standard.StandardTokenizer;

/**
 * Provides the functionality to take into word level n-grams for matching
 * passages with queries. This can sometimes lead to better results.
 *
 * @author dganguly

*/public class ShingleAnalyzer extends Analyzer {

    Properties prop;

    /**
     * The properties file can specify the minmim and the maximum length
     * of the shingles with the help of the parameters 'minShingleSize' and
     * 'maxShingleSize' respectively.
     * @param prop  Properties object
     */
    public ShingleAnalyzer(Properties prop) {
        super();
        this.prop = prop;
    }

    /**
     * Overrides the 'createComponents' method of the Lucene class 'Analyzer'.
     * By default, the minimum shingle size is 2 and the maximum is 3, which means
     * that it generates 2-gram and 3-gram word shingles.
     *
     * @param string
     * @return
     */    
    @Override
    protected Analyzer.TokenStreamComponents createComponents(String string) {
        TokenStream result = null;

        Tokenizer source = new StandardTokenizer();
        Map<String, String> shingleFilterParams = new HashMap<>();

        int minShingleSize = Integer.parseInt(prop.getProperty("minShingleSize", "2"));
        int maxShingleSize = Integer.parseInt(prop.getProperty("maxShingleSize", "3"));

        if (minShingleSize == 1 || maxShingleSize < minShingleSize) {
            // we don't want n-gram indexing
            result = source;
        }
        else {
            shingleFilterParams.put("minShingleSize", String.valueOf(minShingleSize));
            shingleFilterParams.put("maxShingleSize", String.valueOf(maxShingleSize));
            shingleFilterParams.put("tokenSeparator", "#"); // looks good in luke
            shingleFilterParams.put("outputUnigrams", "true");
            shingleFilterParams.put("outputUnigramsIfNoShingles", "true");

            result = new ShingleFilterFactory(shingleFilterParams).create(source);
        }
        return new Analyzer.TokenStreamComponents(source, result);
    }
}