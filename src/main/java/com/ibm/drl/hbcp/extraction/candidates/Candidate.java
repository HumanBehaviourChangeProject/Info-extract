package com.ibm.drl.hbcp.extraction.candidates;

import com.ibm.drl.hbcp.extraction.Scored;

import java.util.List;
import java.util.stream.Collectors;

/**
 * A candidate answer returned by processors in the extraction package (like extractors).
 * Essentially a scored object.
 *
 * @param <T> type of object returned as candidate
 * @author marting
 */
public abstract class Candidate<T> implements Scored {

    protected final T answer;

    public Candidate(T answer) {
        this.answer = answer;
    }

    public T getAnswer() {
        return answer;
    }

    @Override
    public String toString() {
        return answer + "(" + getScore() + ")";
    }

    public static <T, C extends Candidate<T>> List<T> unwrap(List<C> candidates) {
        return candidates.stream()
                .map(C::getAnswer)
                .collect(Collectors.toList());
    }
}
