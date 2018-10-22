/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package normrsv;

import extractor.InformationUnit;
import indexer.PaperIndexer;
import indexer.ResearchDoc;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dganguly
 */

class ScoreDocComparator implements Comparator<ScoreDoc> {

    @Override
    public int compare(ScoreDoc thisObj, ScoreDoc thatObj) {
        return Float.compare(thatObj.score, thisObj.score);  // descending
    }
}

public class NormalizedRSVRetriever {
    IndexSearcher searcher;
    String fieldName;
    RSVNormalizer rsvn;
    Logger logger;
    
    public NormalizedRSVRetriever(IndexSearcher searcher, String fieldName) {
        this.searcher = searcher;
        this.fieldName = fieldName;
        rsvn = new RSVNormalizer(searcher, fieldName);
        
        logger = (Logger)LoggerFactory.getLogger(NormalizedRSVRetriever.class);
    }
    
    String topDocsToString(TopDocs topDocs) {
        StringBuffer buff = new StringBuffer();
        for (ScoreDoc sd: topDocs.scoreDocs) {
            buff.append("(").append(sd.doc).append(",").append(sd.score).append(") ");
        }
        return buff.toString();
    }
    
    public TopDocs rerank(TopDocs topDocs) throws Exception {        
        ArrayList<ScoreDoc> rerankedSDList = new ArrayList<>();
        
        for (ScoreDoc sd : topDocs.scoreDocs) {
            try {
                ScoreDoc nsd = rsvn.getNormalizedScore(sd);
                rerankedSDList.add(nsd);
            }
            catch (Exception ex) {
                ex.printStackTrace();
            }
        }
        
        Collections.sort(rerankedSDList, new ScoreDocComparator());
        
        ScoreDoc[] newsd = new ScoreDoc[rerankedSDList.size()];
        newsd = rerankedSDList.toArray(newsd);
        
        TopDocs rerankedTopDocs = new TopDocs(newsd.length, newsd, newsd[0].score);
        
        if (false) {
            logger.info("Before norm: " + topDocsToString(topDocs));
            logger.info("After norm: " + topDocsToString(rerankedTopDocs));

            // check if the order has changed...
            for (int i=0; i<topDocs.scoreDocs.length; i++) {
                if (topDocs.scoreDocs[i].doc != rerankedTopDocs.scoreDocs[i].doc) {
                    logger.error("Change in order at index " + i);
                }

            }
        }
        
        return rerankedTopDocs;
    }
    
}
