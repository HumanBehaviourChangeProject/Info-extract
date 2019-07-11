/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import java.io.File;
import java.util.*;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 * Builds up the list of sum vectors in memory
 * 
 * @author Debasis
 */
public class CentroidInfo {
    ClusteredWordVecs wvecs;
    Properties prop;
    IndexReader clusterInfoReader;
    HashMap<Integer, CentroidVec> partialSumVecMap;  // map partial sum vecs for each cluster

    public CentroidInfo(ClusteredWordVecs wvecs) throws Exception {
        this.prop = wvecs.getProperties();
        this.wvecs = wvecs;
        
        int numVocabClusters = Integer.parseInt(prop.getProperty("retrieve.vocabcluster.numclusters", "0"));
        if (numVocabClusters > 0) {
            String clusterInfoIndexPath = prop.getProperty("wvecs.clusterids.basedir") + "/" + numVocabClusters;
            clusterInfoReader = DirectoryReader.open(FSDirectory.open(new File(clusterInfoIndexPath).toPath()));
        }
    }

    public void buildCentroids() throws Exception {
        System.out.println("Building cluster vecs in memory...");
        int numDocs = clusterInfoReader.numDocs();
        for (int i=0; i<numDocs; i++) {
            Document d = clusterInfoReader.document(i);
            String wordName = d.get(WordVecsIndexer.FIELD_WORD_NAME);
            int clusterId = Integer.parseInt(d.get(WordVecsIndexer.FIELD_WORD_VEC));
            
            CentroidVec partialSumVec = partialSumVecMap.get(clusterId);
            WordVec wv = wvecs.getVec(wordName);
            if (partialSumVec == null) {
                partialSumVec = new CentroidVec(wv, clusterId);
            }
            partialSumVec.add(wv);
            partialSumVecMap.put(clusterId, partialSumVec);
        }        
        
        // Build the list of centroids
        for (CentroidVec psv : partialSumVecMap.values()) {
            psv.sumvec.scalarMutiply(1/(float)numDocs);
        }
        
        clusterInfoReader.close();
    }
    
    /** Returns the nearest centroids */
    public List<WordVec> getNearestClusterCentres(WordVec queryTermVec) {
        List<CentroidVec> psvList = new ArrayList<>(partialSumVecMap.size());
        int k = Integer.parseInt(prop.getProperty("lfbm.knn.clusters", "5"));
        
        for (CentroidVec cvec : partialSumVecMap.values()) {
            cvec.simWithQuery = queryTermVec.cosineSim(cvec.sumvec);
            psvList.add(cvec);
        }
        Collections.sort(psvList);        
        psvList = psvList.subList(0, k);
        
        List<WordVec> nncvecs = new ArrayList<>();
        for (CentroidVec psv: psvList) {
            psv.sumvec.querySim = psv.simWithQuery;
            nncvecs.add(psv.sumvec);
        }
        return nncvecs;
    }
}
