package com.ibm.drl.hbcp.extraction.candidates;

import com.ibm.drl.hbcp.extraction.passages.Passage;

public class CandidateInPassage<T> extends Candidate<T> {
    private final Passage passage;
    private final double inPassageScore;
    private final double aggregationScore;

    public CandidateInPassage(Passage passage, T answer, double inPassageScore, double aggregationScore) {
        super(answer);
        this.passage = passage;
        this.inPassageScore = inPassageScore;
        this.aggregationScore = aggregationScore;
    }

    @Override // maybe some formula of the 2 scores here
    public double getScore() { return inPassageScore; }

    public Passage getPassage() { return passage; }
    public double getAggregationScore() { return aggregationScore; }

   
}