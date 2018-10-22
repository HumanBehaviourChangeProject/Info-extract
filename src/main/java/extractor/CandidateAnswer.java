/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

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

    public CandidateAnswer(String key, int pos) {
        this.key = new String(key);
        this.pos = pos;
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
}


