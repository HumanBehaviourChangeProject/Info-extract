package com.ibm.drl.hbcp.extraction.answerselectors;

import com.ibm.drl.hbcp.extraction.candidates.Candidate;

import java.util.Collection;

/**
 * Performs an endofunction on a collection of candidate answers, i.e. it returns another collection of
 * answers when given a collection of answers. The objective of an AnswerSelector will often be to vastly decrease the size
 * of the answer set, for example a typical AnswerSelector could be the one that returns a singleton
 * comprised of the answer with the best score.
 * @param <Answer> type of answer
 * @param <CandidateAnswer> type of candidates holding answers (these objects will typically hold information like passage/context, scores, etc...)
 *
 * @author marting
 */
public interface AnswerSelector<Answer, CandidateAnswer extends Candidate<Answer>> {

    /** Selects the answer candidate(s) to return */
    Collection<CandidateAnswer> select(Collection<CandidateAnswer> candidates);

    /** Selector performing one selector, and then another one on the first's results */
    default AnswerSelector<Answer, CandidateAnswer> then(AnswerSelector<Answer, CandidateAnswer> secondSelector) {
        AnswerSelector<Answer, CandidateAnswer> firstSelector = this;
        return new AnswerSelector<Answer, CandidateAnswer>() {
            @Override
            public Collection<CandidateAnswer> select(Collection<CandidateAnswer> candidates) {
                return secondSelector.select(firstSelector.select(candidates));
            }
        };
    }
}
