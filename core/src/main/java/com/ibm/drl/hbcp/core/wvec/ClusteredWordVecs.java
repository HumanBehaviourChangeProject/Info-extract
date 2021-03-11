/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import java.io.File;
import java.io.FileReader;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.TopScoreDocCollector;
import org.apache.lucene.store.FSDirectory;

/**
 * A collection of WordVec instances for each unique term in
 * the collection.
 * @author Debasis
 */
public class ClusteredWordVecs {

    Properties prop;
    IndexReader reader;
    IndexSearcher searcher;
    IndexReader clusterInfoReader;
    CentroidInfo centroidInfo;
    int numClusters;
    
    HashMap<String, Integer> clusterMap = new HashMap<>();
    
    public ClusteredWordVecs(Properties prop) throws Exception {
        this.prop = prop;
        String loadFrom = prop.getProperty("wvecs.index");
        File indexDir = new File(loadFrom);
        
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        searcher = new IndexSearcher(reader);
        
        int numClusters = Integer.parseInt(prop.getProperty("retrieve.vocabcluster.numclusters", "0"));
        if (numClusters > 0) {
            String clusterInfoIndexPath = prop.getProperty("wvecs.clusterids.basedir") + "/" + numClusters;
            clusterInfoReader = DirectoryReader.open(FSDirectory.open(new File(clusterInfoIndexPath).toPath()));
        }
        
        System.out.println("Loading cluster ids in memory...");
        int numDocs = clusterInfoReader.numDocs();
        for (int i=0; i<numDocs; i++) {
            Document d = clusterInfoReader.document(i);
            String wordName = d.get(WordVecsIndexer.FIELD_WORD_NAME);
            int clusterId = Integer.parseInt(d.get(WordVecsIndexer.FIELD_WORD_VEC));
            clusterMap.put(wordName, clusterId);
        }
        
        clusterInfoReader.close();
        
        centroidInfo = new CentroidInfo(this);
        centroidInfo.buildCentroids();
    }

    public int getNumClusters() { return numClusters; }
    
    public CentroidInfo getCentroidInfo() { return centroidInfo; }

    /** Gets the stored sum vec corresponding to this cluster id */
    public WordVec getCentroidVec(int clusterId) throws Exception {
        TermQuery tq = new TermQuery(new Term(WordVecsIndexer.FIELD_WORD_VEC, String.valueOf(clusterId)));
        TopDocs topDocs = searcher.search(tq, 1);
        Document d = reader.document(topDocs.scoreDocs[0].doc);
        String centroidvec = CompressionUtils.decompress(d.getBinaryValue(WordVecsIndexer.FIELD_CENTROID_VEC).bytes);
        WordVec cvec = new WordVec(centroidvec);
        return cvec;
    }
    
    public void close() throws Exception {
        reader.close();
    }

    Query getLuceneQueryObject(String word) {
        BooleanQuery.Builder q = new BooleanQuery.Builder();
        TermQuery tq = new TermQuery(new Term(WordVecsIndexer.FIELD_WORD_NAME, word));
        q.add(tq, BooleanClause.Occur.MUST);
        return q.build();
    }

    public WordVec getVec(String word) throws Exception {
        TopScoreDocCollector collector;
        TopDocs topDocs;
        // TODO: marting: I set "totalHitsThreshold" to 0 here (port to Lucene 8), but I'm not sure that's correct
        collector = TopScoreDocCollector.create(1, 0);
        searcher.search(getLuceneQueryObject(word), collector);
        topDocs = collector.topDocs();
        
        if (topDocs.scoreDocs == null || topDocs.scoreDocs.length == 0) {
            //System.err.println("vec for word: " + word + " not found");
            return null;
        }
        
        int wordId = topDocs.scoreDocs[0].doc;
        Document matchedWordVec = reader.document(wordId);
        String line = matchedWordVec.get(WordVecsIndexer.FIELD_WORD_VEC);
        WordVec wv = new WordVec(line);
        return wv;
    }

    public float getSim(String u, String v) throws Exception {
        WordVec uVec = getVec(u);
        WordVec vVec = getVec(v);
        return uVec.cosineSim(vVec);
    }
    
    public float getSim(WordVec u, WordVec v) throws Exception {
        return u.cosineSim(v);
    }

    public float getAngularDist(WordVec u, WordVec v) throws Exception {
        return (float)Math.acos(u.cosineSim(v));
    }
    
    public WordVec getCentroid(List<WordVec> wvecs) {
        int numVecs = wvecs.size(), j, dimension = wvecs.get(0).getDimension();
        WordVec centroid = new WordVec(dimension);
        for (WordVec wv : wvecs) {
            for (j = 0; j < dimension; j++) {
                centroid.vec[j] += wv.vec[j];
            }
        }
        for (j = 0; j < dimension; j++) {
            centroid.vec[j] /= (double)numVecs;
        }
        
        return centroid;
    }
    
    public int getClusterId(String word) throws Exception {
        if (!clusterMap.containsKey(word))
            return -1;
        return clusterMap.get(word);
    }    
    
    public Properties getProperties() { return prop; }
    
    public static void main(String[] args) {        
        try {
            if (args.length < 1) {
                args = new String[1];
                args[0] = "init.properties";
            }
            
            Properties prop = new Properties();
            prop.load(ClusteredWordVecs.class.getClassLoader().getResourceAsStream(args[0]));
        	// prop.load(new FileReader(args[0]));
            ClusteredWordVecs wvecs = new ClusteredWordVecs(prop);
            System.out.println(wvecs.getVec("govern"));
            wvecs.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
