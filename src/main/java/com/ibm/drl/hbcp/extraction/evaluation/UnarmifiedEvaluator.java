package com.ibm.drl.hbcp.extraction.evaluation;

import com.ibm.drl.hbcp.extractor.RefComparison;

import java.util.Collection;

/**
 * This evaluator assumes that a single answer (at most) is predicted AND expected from a document.
 *
 * @param <Document> type of document handled by the extractor
 * @param <E> type of answers returned by the extractor
 *
 * @author marting
 */
public interface UnarmifiedEvaluator<Document, E> extends Evaluator<Document, E> {

    RefComparison compareWithRef(E candidateAnswer, E expected);

    @Override
    default RefComparison compareWithRef(Collection<E> predicted, Collection<E> expected) {
        // get the first candidate (there should only be one at most anyway)
        E candidateAnswer = predicted.stream().findFirst().orElse(null);
        // get the first annotation (there should only be one at most anyway)
        E expectedAnswer = expected.stream().findFirst().orElse(null);
        return compareWithRef(candidateAnswer, expectedAnswer);
    }
}
