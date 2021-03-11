package com.ibm.drl.hbcp.extraction.answerselectors;

import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.passages.Passage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * For each candidate, aggregate scores over all the passages represented in the answer collection
 * and containing the candidate.
 * The final result is a list of answers (all unique) attached to a representative passage and a final score.
 * @param <T> type of answer
 *
 * @author marting
 */
public class AcrossPassagesAggregator<T> implements AnswerSelector<T, CandidateInPassage<T>> {

    @Override
    public Collection<CandidateInPassage<T>> select(Collection<CandidateInPassage<T>> candidates) {
        // group the answers by their passage (context)
        Map<Passage, List<CandidateInPassage<T>>> candidatesPerPassage = candidates.stream()
                .collect(Collectors.groupingBy(CandidateInPassage::getPassage));
        // keep only certain answers for each passage (examples: all, only the best, ...)
        Map<Passage, List<CandidateInPassage<T>>> retainedCandidatesPerPassage =
                getCandidatesToKeepPerPassage(candidatesPerPassage);
        // accumulate the per-passage scores for each candidate, and also determine the best representative passage
        // to give as context
        Map<T, Double> aggregatedScorePerCandidate = new HashMap<>();
        Map<T, Double> bestIndividualScorePerPassage = new HashMap<>();
        Map<T, CandidateInPassage<T>> bestPassagePerCandidate = new HashMap<>();
        for (List<CandidateInPassage<T>> candidatesForPassage : retainedCandidatesPerPassage.values()) {
            for (CandidateInPassage<T> candidate : candidatesForPassage) {
                T answer = candidate.getAnswer();
                aggregatedScorePerCandidate.putIfAbsent(answer, 0.0);
                bestIndividualScorePerPassage.putIfAbsent(answer, Double.NEGATIVE_INFINITY);
                double individualScore = getCombinedScore(candidate.getScore(), candidate.getPassage().getScore());
                if (individualScore > bestIndividualScorePerPassage.get(answer)) {
                    // update the best passage
                    bestIndividualScorePerPassage.put(answer, individualScore);
                    bestPassagePerCandidate.put(answer, candidate);
                }
                // accumulate the score
                aggregatedScorePerCandidate.put(answer, aggregatedScorePerCandidate.get(answer) + individualScore);
            }
        }
        // at this point we have the accumulated score, and we also stored the best passage
        // build the final collection
        List<CandidateInPassage<T>> res = new ArrayList<>();
        for (T answer : aggregatedScorePerCandidate.keySet()) {
            CandidateInPassage<T> bestCandidate = new CandidateInPassage<>(
                    bestPassagePerCandidate.get(answer).getPassage(),
                    bestPassagePerCandidate.get(answer).getAnswer(),
                    aggregatedScorePerCandidate.get(answer),
                    aggregatedScorePerCandidate.get(answer));
            res.add(bestCandidate);
        }
        // sort by decreasing score (TODO: should we do that here???, not really important)
        res.sort(Comparator.reverseOrder());
        return res;
    }

    protected Map<Passage, List<CandidateInPassage<T>>> getCandidatesToKeepPerPassage(Map<Passage, List<CandidateInPassage<T>>> candidatesPerPassage) {
        return candidatesPerPassage;
    }

    protected double getCombinedScore(double querySimilarity, double passageScore) {
        return querySimilarity * passageScore;
    }
}
