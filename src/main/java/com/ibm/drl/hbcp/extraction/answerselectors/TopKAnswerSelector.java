package com.ibm.drl.hbcp.extraction.answerselectors;

import com.ibm.drl.hbcp.extraction.candidates.Candidate;

import java.util.Collection;
import java.util.Comparator;
import java.util.stream.Collectors;

/**
 * Selects the top K best candidate answers (according to their score).
 *
 * @author marting
 */
public class TopKAnswerSelector<Answer, CandidateAnswer extends Candidate<Answer>> implements AnswerSelector<Answer, CandidateAnswer> {

    private final int k;

    public TopKAnswerSelector(int k) {
        this.k = k;
    }

    @Override
    public Collection<CandidateAnswer> select(Collection<CandidateAnswer> candidates) {
        return candidates.stream()
                .sorted(Comparator.reverseOrder())
                .limit(k)
                .collect(Collectors.toList());
    }
}
