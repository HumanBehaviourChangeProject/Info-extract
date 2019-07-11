/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

/**
 * A vector representing a centroid of multiple vectors.
 * @author Debasis
 */

public class CentroidVec implements Comparable<CentroidVec> {
    WordVec sumvec;
    int nvecs;
    float simWithQuery;
    
    public CentroidVec(WordVec wv, int clusterId) throws Exception {
        sumvec = new WordVec(wv.getDimension()); // zero vec
        sumvec.clusterId = clusterId;
    }
    
    public void add(WordVec wv) {
        sumvec = WordVec.sum(sumvec, wv);
        nvecs++;
    }

    @Override
    public int compareTo(CentroidVec that) {
        return -1*Float.compare(simWithQuery, that.simWithQuery); // descending
    }
}
