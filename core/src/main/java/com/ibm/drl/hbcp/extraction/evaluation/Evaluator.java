package com.ibm.drl.hbcp.extraction.evaluation;

import com.ibm.drl.hbcp.extraction.candidates.Candidate;
import com.ibm.drl.hbcp.extraction.extractors.EvaluatedExtractor;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An evaluator defines a strategy to return evaluation metrics (RefComparison) comparing the run of an
 * extractor on documents with ground truth annotations of the same documents.
 * @param <Document> type of document handled by the extractor
 * @param <Answer> type of answer returned by the extractor
 *
 * @author marting
 */
public interface Evaluator<Document, Answer> {

    /** For one document, compares the predicted values with the expected values (annotations) */
    RefComparison compareWithRef(Collection<Answer> predicted, Collection<Answer> expected);

    /** Evaluate the given extractor on documents (associated with their respective annotations) */
    default RefComparison evaluate(EvaluatedExtractor<Document, Answer, ?> extractor,
                                  List<Pair<Document, Collection<Answer>>> groundTruth) throws IOException {
        List<RefComparison> res = new ArrayList<>();
        for (Pair<Document, Collection<Answer>> docAndAnnotations : groundTruth) {
            Collection<Answer> relevantAnnotations = extractor.getRelevant(docAndAnnotations.getRight());
            RefComparison singleDocumentEval = evaluate(extractor, docAndAnnotations.getLeft(), relevantAnnotations);
            res.add(singleDocumentEval);
        }
        return aggregate(res);
    }

    /** Evaluates the given extractor on a single document associated with its annotations */
    default RefComparison evaluate(EvaluatedExtractor<Document, Answer, ?> extractor, Document document, Collection<Answer> expected) throws IOException {
        Collection<? extends Candidate<Answer>> predictedCandidates = extractor.extract(document);

        Collection<Answer> predicted = predictedCandidates.stream().map(Candidate::getAnswer).collect(Collectors.toList());
        return compareWithRef(predicted, expected);
    }

    /** Aggregates the evaluation results of single documents to get final results */
    default RefComparison aggregate(List<RefComparison> singleDocumentEvaluations) {
        RefComparison res = new RefComparison();
        for (RefComparison singleDocumentEval : singleDocumentEvaluations) {
            res.addConfusionMatrixCounts(singleDocumentEval);
        }
        res.compute();
        return res;
    }
}
