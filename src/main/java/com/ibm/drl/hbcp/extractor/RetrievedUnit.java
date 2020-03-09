/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswer;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswers;
import com.ibm.drl.hbcp.extractor.matcher.QueryTermMatches;

/**
 * Store this class' objects in the aggregate function.
 * This object holds a pointer (an index) into the array of retrieved IUs.
 * This allows the information unit object to change freely.
 * 
 * This is to fix a bug in the code where inside the loop - for all retrieved
 * passages, the information unit objects were getting changed.
 * 
 * @author dganguly
 */
public class RetrievedUnit implements Comparable<RetrievedUnit> {
    float weight;
    int rank;
    InformationUnit iu;
    
    CandidateAnswers keys;
    QueryTermMatches queryTermMatches;    
    CandidateAnswer mostLikelyAnswer;

    public RetrievedUnit(InformationUnit iu, float weight, int rank) {
        this.iu = iu;
        this.weight = weight;
        this.rank = rank;
        
        // use the current iu object to save the retrieved status
        //this.keys = iu.keys;
        this.queryTermMatches = iu.queryTermMatches;
        this.mostLikelyAnswer = iu.mostLikelyAnswer;
    }
    
    InformationUnit writeBackBestValues() {
        //iu.keys = this.keys;
        iu.queryTermMatches = this.queryTermMatches;
        iu.mostLikelyAnswer = this.mostLikelyAnswer;
        iu.weight = this.weight;
        
        return iu;
    }

    @Override
    public int compareTo(RetrievedUnit o) {
        return -1* Float.compare(weight, o.weight);
    }
}
