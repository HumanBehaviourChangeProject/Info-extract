package com.ibm.drl.hbcp.extraction.extractors;

import com.ibm.drl.hbcp.extraction.candidates.Candidate;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extractor.RefComparison;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * An extractor that can be evaluated against an annotated collection of document.
 * Each evaluator constitutes a different evaluation strategy and there is a resulting RefComparison for each of them.
 * @param <Document> type of document handled by the extractor
 * @param <Answer> type of answer returned by the extractor
 * @param <CandidateAnswer> type of candidate answer object (wrapper around answer)
 */
public interface EvaluatedExtractor<Document, Answer, CandidateAnswer extends Candidate<Answer>> extends Extractor<Document, Answer, CandidateAnswer> {

    /** Returns all the evaluators to run. Evaluators should be quick to construct */
    List<? extends Evaluator<Document, Answer>> getEvaluators();

    /** Returns an evaluation result (RefComparison) for each evaluator run over the whole list of annotated documents */
    default List<RefComparison> evaluate(List<Pair<Document, Collection<Answer>>> groundTruth) throws IOException {
        List<RefComparison> res = new ArrayList<>();
        for (Evaluator<Document, Answer> evaluator : getEvaluators()) {
            RefComparison evaluation = evaluator.evaluate(this, groundTruth);
            res.add(evaluation);
        }
        return res;
    }

    /** Returns only the relevant annotated values for this extractor (it is interesting for example to filter
     * by type of attribute) */
    default Collection<Answer> getRelevant(Collection<Answer> annotations) {
            return annotations;
    }
}
