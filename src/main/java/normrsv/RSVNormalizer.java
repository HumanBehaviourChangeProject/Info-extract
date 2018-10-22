/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package normrsv;

import java.util.HashMap;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.index.Terms;
import org.apache.lucene.index.TermsEnum;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.util.BytesRef;


/**
 *
 * @author Debasis
 */
public class RSVNormalizer  {
    String fieldName;
    IndexReader reader;
    IndexSearcher searcher;
    
    public RSVNormalizer(IndexSearcher searcher, String fieldName) {
        this.reader = searcher.getIndexReader();
        this.searcher = searcher;
        this.fieldName = fieldName;
    }
    
    // Get the whole document as a query
    public Query getWholeDocAsQuery(int docId) throws Exception {
        Terms tfvector;
        String termText;
        BytesRef term;
        TermsEnum termsEnum;

        BooleanQuery dAndQ = new BooleanQuery();
        
        tfvector = reader.getTermVector(docId, fieldName);
        if (tfvector == null || tfvector.size() == 0)
            return null;

        // Construct the normalized tf vector
        termsEnum = tfvector.iterator(); // access the terms for this field
    	while ((term = termsEnum.next()) != null) { // explore the terms for this field
            termText = term.utf8ToString();
            int tf = (int)termsEnum.totalTermFreq();
            
            TermQuery tq = new TermQuery(new Term(fieldName, termText));
            tq.setBoost(tf);
            dAndQ.add(tq, BooleanClause.Occur.SHOULD);
        }

        return dAndQ;
    }
    
    float getDocNorm(int docId) throws Exception {
        Query docq = getWholeDocAsQuery(docId);  // docId for internal use in temp in-mem index      
        TopDocs topdocs = searcher.search(docq, 1); // get the norm factor
        
        float zd = topdocs.scoreDocs[0].score;        
        return zd;
    }
    
    // Assume that all terms from document d is the query. Match
    // the document against itself to compute the normalization factor
    public ScoreDoc getNormalizedScore(ScoreDoc sd) throws Exception {
        
        float z = getDocNorm(sd.doc);
        assert (z >= sd.score);
        float nscore = sd.score/z;  // (d.q)/ (|d| |q|)  |x| --> L1 norm of x
        
        return new ScoreDoc(sd.doc, nscore);  // normalized rsv
    }    
}