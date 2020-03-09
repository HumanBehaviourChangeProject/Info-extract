/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import com.ibm.drl.hbcp.util.LuceneField;
import org.apache.commons.math3.exception.DimensionMismatchException;
import org.apache.commons.math3.ml.clustering.CentroidCluster;
import org.apache.commons.math3.ml.clustering.KMeansPlusPlusClusterer;
import org.apache.commons.math3.ml.distance.DistanceMeasure;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.StoredField;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

class CosineDistance implements DistanceMeasure {

    double norm(double[] vec) {
        // calculate and store
        double sum = 0;
        for (int i = 0; i < vec.length; i++) {
            sum += vec[i]*vec[i];
        }
        return Math.sqrt(sum);
    }
    
    @Override
    public double compute(double[] a, double[] b) throws DimensionMismatchException {
        double sum = 0;
        for (int i = 0; i < a.length; i++) {
            sum += a[i]*b[i];
        }
        double angle = Math.acos(sum/(norm(a)*norm(b))); 
        return angle;
    }    
}
/**
 *
 * @author Debasis
 */
public class WordVecsIndexer {
    IndexWriter writer;
    Properties prop;
    String indexPath;
    
    static final public String FIELD_WORD_NAME = "wordname";
    static final public String FIELD_WORD_VEC = "wordvec"; // cluster-id
    static final public String FIELD_CENTROID_VEC = "cvec";

    public WordVecsIndexer(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));                
        indexPath = prop.getProperty("wvecs.index");        
    }
    
    public Properties getProperties() { return prop; }

    public void writeIndex() throws Exception {        
        IndexWriterConfig iwcfg = new IndexWriterConfig(new WhitespaceAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        writer = new IndexWriter(FSDirectory.open(new File(indexPath).toPath()), iwcfg);        
        
        String fileToRead = prop.getProperty("wvecs.txt");
        indexFile(new File(fileToRead));
        writer.close();        
    }

    Document constructDoc(String id, String line) throws Exception {        
        Document doc = new Document();
        doc.add(new Field(FIELD_WORD_NAME, id, LuceneField.STORED_NOT_ANALYZED.getType()));
        doc.add(new Field(FIELD_WORD_VEC, line,
                LuceneField.STORED_NOT_ANALYZED.with(ft -> ft.setStoreTermVectors(false)).getType()));
        return doc;        
    }
    
    // Store the sum vecs in the index
    Document constructDoc(String id, String clusterId, WordVec cvec) throws Exception {        
        Document doc = new Document();
        doc.add(new Field(FIELD_WORD_NAME, id, LuceneField.STORED_NOT_ANALYZED.getType()));
        doc.add(new Field(FIELD_WORD_VEC, clusterId,
                LuceneField.STORED_NOT_ANALYZED.with(ft -> ft.setStoreTermVectors(false)).getType()));
        doc.add(new StoredField(FIELD_CENTROID_VEC, CompressionUtils.compress(cvec.toString())));
        return doc;        
    }

    void storeClusterInfo() throws Exception {
        int numClusters = Integer.parseInt(prop.getProperty("wvecs.numclusters", "500"));
        String clusterInfoBaseDir = prop.getProperty("wvecs.clusterids.basedir");
        String clusterInfoDirPath = clusterInfoBaseDir + "/" + numClusters;
        
        File clusterInfoFile = new File(clusterInfoDirPath);
        if (clusterInfoFile.isDirectory() && clusterInfoFile.exists()) {
            System.out.println("Cluster info already exists...");
            return;
        }

        // Create the directory...
        clusterInfoFile.mkdir();
        IndexWriterConfig iwcfg = new IndexWriterConfig(
                new WhitespaceAnalyzer());
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);

        IndexWriter clusterInfoFileWriter = new IndexWriter(FSDirectory.open(clusterInfoFile.toPath()), iwcfg);        
        clusterWordVecs(clusterInfoFileWriter, numClusters);
        clusterInfoFileWriter.close();        
    }
    
    void clusterWordVecs(IndexWriter clusterIndexWriter, int numClusters) throws Exception {
        // Index where word vectors are stored
        IndexReader reader = DirectoryReader.open(FSDirectory.open(new File(indexPath).toPath()));
        int numDocs = reader.numDocs();
        DistanceMeasure angleMeasure = new CosineDistance();
        KMeansPlusPlusClusterer<WordVec> clusterer = new KMeansPlusPlusClusterer<>(numClusters, 1000, angleMeasure);
        List<WordVec> wordList = new ArrayList<>(numDocs);
        
        // Read every com.ibm.drl.hbcp.core.wvec and load in memory
        for (int i = 0; i < numDocs; i++) {
            Document doc = reader.document(i);
            WordVec wvec = new WordVec(doc.get(FIELD_WORD_VEC));
            wordList.add(wvec);            
        }
        
        // Call K-means clustering
        System.out.println("Clustering the entire vocabulary...");
        List<CentroidCluster<WordVec>> clusters = clusterer.cluster(wordList);        
        
        // Save the cluster info
        System.out.println("Writing out cluster ids in Lucene index...");
        int clusterId = 0;
        for (CentroidCluster<WordVec> c : clusters) {
            WordVec cvec = (WordVec)c.getCenter();
            List<WordVec> pointsInThisClusuter = c.getPoints();
            for (WordVec thisPoint: pointsInThisClusuter) {
                Document clusterInfo = constructDoc(thisPoint.word, String.valueOf(clusterId), cvec);
                clusterIndexWriter.addDocument(clusterInfo);
            }
            clusterId++;
        }
        
        reader.close();
    }
    
    void indexFile(File file) throws Exception {
        FileReader fr = new FileReader(file);
        BufferedReader br = new BufferedReader(fr);
        String line;
        
        final int batchSize = 10000;
        int count = 0;
        
        // Each line is word vector
        while ((line = br.readLine()) != null) {
            
            int firstSpaceIndex = line.indexOf(" ");
            String id = line.substring(0, firstSpaceIndex);
            Document luceneDoc = constructDoc(id, line);
            
            if (count%batchSize == 0) {
                System.out.println("Added " + count + " words...");
            }
            
            writer.addDocument(luceneDoc);
            count++;
        }
        br.close();
        fr.close();
    }
    
    /* Use this to index the output of word2vec, i.e. instead
       of loading the word vectors from an in-memory hashmap
       keyed by a word, use Lucene search to retrieve the vector
       given a word. This makes it possible to run this on
       a limited memory environment. */
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java WordVecsIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            WordVecsIndexer wvIndexer = new WordVecsIndexer(args[0]);
            if (Boolean.parseBoolean(wvIndexer.getProperties().getProperty("wvecs.writeindex")))
                wvIndexer.writeIndex();
            wvIndexer.storeClusterInfo();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    
}
