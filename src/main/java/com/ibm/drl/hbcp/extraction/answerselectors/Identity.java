package com.ibm.drl.hbcp.extraction.answerselectors;

import com.ibm.drl.hbcp.extraction.candidates.Candidate;

import java.util.Collection;

/**
 * Answer selector that does nothing and returns exactly the same set of candidate answers
 *
 * @author marting
 */
public class Identity<Answer, CandidateAnswer extends Candidate<Answer>> implements AnswerSelector<Answer, CandidateAnswer> {
    @Override
    public Collection<CandidateAnswer> select(Collection<CandidateAnswer> candidates) {
        return candidates;
    }
}
