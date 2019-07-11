/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.Fields;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.MultiFields;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.lucene.util.BytesRef;
import org.apache.lucene.search.highlight.WeightedTerm;

/**
 * Extraction function is a LM-based term scoring function
 * @author dganguly
 */
public class TopTermsExtractor {
    Analyzer analyzer;
    List<String> docs;
    IndexReader inMemReader;
    IndexReader externalReader;  // for external idf stats
    String externalReaderContentFieldName;
    
    static final String FIELD_CONTENT = "content";
    
    /**
     * Constructs an in-memory index for computing term statistics from
     * a given set of documents (strings), with a specified analyzer
     * and an external index for computing global (document frequency) statistics.
     * 
     * @param docs
     * @param analyzer
     * @param externalReader
     * @param externalReaderContentFieldName
     * @throws Exception 
     */
    public TopTermsExtractor(List<String> docs, Analyzer analyzer,
            IndexReader externalReader, String externalReaderContentFieldName) throws Exception {
        this.analyzer = analyzer;
        this.docs = docs;
        
        this.externalReaderContentFieldName = externalReaderContentFieldName;
        inMemReader = constructIndex();
    }
    
    IndexReader constructIndex() throws Exception {
        Directory ramdir = new RAMDirectory();                
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);
        
        // Write the documents in index... We simply want to use Lucene's term
        // statistics function to get a list of frequencies for each term.
        for (String s : docs) {
            Document doc = new Document();
            doc.add(new Field(FIELD_CONTENT,
                    s,
                    Field.Store.YES, Field.Index.ANALYZED));
            writer.addDocument(doc);
        }
        
        writer.commit();
        writer.close();

        Directory inMemIndex = writer.getDirectory();
        return DirectoryReader.open(inMemIndex);
    }
    
    /**
     * Computes the language modeling (LM) score of a set of
     * terms in this index and returns a list comprising the top k ones after
     * sorting these in descending order.
     * 
     * @param k The number of top terms required.
     * @param lambda The Jelinek-Mercer smoothing parameter.
     * @return A list of weighted terms
     * @throws Exception 
     */
    public List<WeightedTerm> computeTopTerms(int k, float lambda) throws Exception {
        // Create in-mem index
        double wt;
        float alpha = lambda/(1-lambda);
        
        Fields fields = MultiFields.getFields(inMemReader);
        Terms terms = fields.terms(FIELD_CONTENT);
        TermsEnum termsEnum = terms.iterator();
        BytesRef term;

        List<WeightedTerm> wtermsList = new ArrayList<>();
        
        while ((term = termsEnum.next()) != null) { // explore the terms for this field
            String termText = term.utf8ToString();
            long freq = inMemReader.totalTermFreq(new Term(FIELD_CONTENT, termText));
            double relativeFreq = freq/(double)docs.size();
            
            double idf = 1;
            
            if (externalReader != null) {
                long externalCf = externalReader.totalTermFreq(new Term(externalReaderContentFieldName, termText));
                long externalSumCf = externalReader.getSumTotalTermFreq(externalReaderContentFieldName);
                idf = externalSumCf/(double)externalCf;
            }
            
            wt = Math.log(1 + alpha * relativeFreq * idf);
            WeightedTerm wterm = new WeightedTerm((float)wt, termText);
            
            //WeightedTerm wterm = new WeightedTerm(termText, (float)wt);
            wtermsList.add(wterm);
        }
        
        wtermsList.sort(Comparator.comparing(WeightedTerm::getWeight));
        return wtermsList.subList(0, Math.min(k, wtermsList.size()));
    }
    
}
