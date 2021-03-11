package com.ibm.drl.hbcp.extraction.answerselectors;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.passages.Passage;

import java.util.*;
import java.util.stream.Collectors;

/**
 * For each passage, only retain the best candidate (wrt a combined score of the query similarity and the passage
 * retrieval score), then aggregate scores over all the passages represented in the answer collection.
 * The final result is a list of answers (all unique) attached to a representative passage and a final score.
 * @param <T> type of answer
 *
 * @author marting
 */
public class BestCandidateInPassageAggregator<T> extends AcrossPassagesAggregator<T> {

    @Override
    protected Map<Passage, List<CandidateInPassage<T>>> getCandidatesToKeepPerPassage(Map<Passage, List<CandidateInPassage<T>>> candidatesPerPassage) {
        return candidatesPerPassage.entrySet().stream()
                .map(passageListEntry -> new AbstractMap.SimpleEntry<>(passageListEntry.getKey(),
                        // ignore that warning, there is at least one element for each key in this map
                        Lists.newArrayList(passageListEntry.getValue().stream().max(Comparator.naturalOrder()).get())))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    protected double getCombinedScore(double querySimilarity, double passageScore) {
        return querySimilarity * passageScore;
    }
}
