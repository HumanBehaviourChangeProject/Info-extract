package com.ibm.drl.hbcp.extraction.answerselectors;

import com.ibm.drl.hbcp.extraction.candidates.Candidate;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Comparator;
import java.util.List;

/**
 * Selects the best candidate answer (according to its score).
 *
 * @author marting
 */
public class BestAnswerSelector<Answer, CandidateAnswer extends Candidate<Answer>> implements AnswerSelector<Answer, CandidateAnswer> {

    @Override
    public Collection<CandidateAnswer> select(Collection<CandidateAnswer> candidates) {
        List<CandidateAnswer> res = new ArrayList<>();
        candidates.stream().max(Comparator.naturalOrder()).ifPresent(res::add);
        return res;
    }
}
