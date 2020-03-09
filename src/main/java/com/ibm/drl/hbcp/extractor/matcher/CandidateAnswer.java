/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor.matcher;

/**
 * Defines a unit for a candidate answer, e.g. the value "18" for minimum age.
 * In general, can correspond to a sequence of tokens.
 *
 * @author dganguly
*/
public class CandidateAnswer implements Comparable<CandidateAnswer> {

    String key;
    int pos;
    float avgKernelSim;
    String context;
    
    public CandidateAnswer(String key, int pos, String context) {
        this.key = new String(key);
        this.pos = pos;
        this.context = context;
    }

    @Override
    public boolean equals(Object that) {
        CandidateAnswer thatAnswer = (CandidateAnswer)that;
        return key.equals(thatAnswer.key);
    }

    @Override
    public int hashCode() {
        return key.hashCode();
    }    
    
    @Override
    public int compareTo(CandidateAnswer that) {
        return -1 * Float.compare(avgKernelSim, that.avgKernelSim);
    }

    public String toString() {
        return "(" + key + ", " + avgKernelSim + ")";
    }
    
    public String getKey() { return key; }
    public void setKey(String key) { this.key = key; }
    public String getContext() { return context; }
    public float avgKernelSim() { return avgKernelSim; }
}


