package com.ibm.drl.hbcp.extraction.evaluation;

import com.ibm.drl.hbcp.extractor.RefComparison;
import org.jetbrains.annotations.NotNull;

/**
 * An unarmified evaluator with a reasonable computation of the confusion matrix (TP, TN, FP, FN) especially
 * in the presence of null values (predicted or expected). The user should only implement a predicate that
 * defines the equivalence/correctness of the predicted answer with respect to the expected answer.
 *
 * @author marting
 * */
public interface PredicateUnarmifiedEvaluator<Doc, E> extends UnarmifiedEvaluator<Doc, E> {

    boolean isCorrect(@NotNull E predicted, @NotNull E expected);

    @Override
    default RefComparison compareWithRef(E predicted, E expected) {
        if (predicted == null && expected == null)
            //return new RefComparison();
            return new RefComparison(0, 0, 0, 1); // TODO: here the multiple current compareWithRef don't agree
        if (predicted == null)
            return new RefComparison(0, 0, 1, 0);
        if (expected == null)
            return new RefComparison(0, 1, 0, 0);
        // here we know both predicted and expected exist
        if (isCorrect(predicted, expected))
            return new RefComparison(1, 0, 0, 0);
        else
            return new RefComparison(0, 1, 1, 0);
    }
}
