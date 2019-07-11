/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor.matcher;

import java.util.Arrays;
import java.util.HashSet;

/**
 * A container class for candidate answers.
 * @author dganguly
 */
public class CandidateAnswers {

    HashSet<CandidateAnswer> answers;

    public CandidateAnswers() {
        answers = new HashSet<>();
    }

    public void addCandidateAnswer(CandidateAnswer answer) {
        answers.add(answer);
    }

    public CandidateAnswer getBestAnswer() {
        CandidateAnswer[] answers = new CandidateAnswer[this.answers.size()];
        answers = this.answers.toArray(answers);        
        Arrays.sort(answers);
        return answers[0];
    }
}

