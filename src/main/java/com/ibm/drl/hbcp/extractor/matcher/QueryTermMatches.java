/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor.matcher;

import java.util.HashMap;

/**
 * Computes a kernel-based similarities of the position matches.
 * The higher the positional differences between the matched query terms and the candidate answer terms,
 * the lower are the similarity values. Favours those
 * passages where this kernel based similarity is high.
 * 
 * @author dganguly
 */
public class QueryTermMatches {

    HashMap<String, QueryTermMatch> queryTermMatches;

    public QueryTermMatches() {
        queryTermMatches = new HashMap<>();
    }

    public void add(String term, int pos) {
        QueryTermMatch match = queryTermMatches.get(term);
        if (match == null) {
            match = new QueryTermMatch(term);
        }
        match.add(pos);
        queryTermMatches.put(term, match);
    }

    public void assignScoresToCandidateAnswers(CandidateAnswers answers) {
        for (CandidateAnswer a : answers.answers) {
            assignScoreToCandidateAnswer(a);
        }
    }

    public void assignScoreToCandidateAnswer(CandidateAnswer a) {
        a.avgKernelSim = 0;
        for (QueryTermMatch match : queryTermMatches.values()) {
            for (int pos : match.pos) {
                a.avgKernelSim += Math.exp(-1 * (pos - a.pos) * (pos - a.pos));
            }
        }
    }

    public CandidateAnswer selectBestAnswer(CandidateAnswers answers) {
        if (answers.answers.size() == 0) {
            return null;
        }
        assignScoresToCandidateAnswers(answers);
        return answers.getBestAnswer();
    }
}


