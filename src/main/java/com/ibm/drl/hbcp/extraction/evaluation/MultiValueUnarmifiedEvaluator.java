package com.ibm.drl.hbcp.extraction.evaluation;

import com.ibm.drl.hbcp.extractor.RefComparison;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.HashSet;

public class MultiValueUnarmifiedEvaluator<Doc, E> implements PredicateUnarmifiedEvaluator<Doc, E> {

    private final PredicateUnarmifiedEvaluator<Doc, E> evaluator;

    public MultiValueUnarmifiedEvaluator(PredicateUnarmifiedEvaluator<Doc, E> evaluator) {
        this.evaluator = evaluator;
    }

    @Override
    public boolean isCorrect(@NotNull E predicted, @NotNull E expected) {
        return evaluator.isCorrect(predicted, expected);
    }

    @Override
    public RefComparison compareWithRef(Collection<E> predicted, Collection<E> expected) {
        if (predicted.isEmpty() && expected.isEmpty())
            return new RefComparison(0, 0, 0, 1); // how many true negatives when no values?
        if (predicted.isEmpty())
            // how many false negative when nothing has been predicted but there are several gold annotations? (1 is more lenient)
            return new RefComparison(0, 0, expected.size(), 0); // new RefComparison(0, 0, expected.size(), 0);
        if (expected.isEmpty())
            return new RefComparison(0, predicted.size(), 0, 0); // 1 fp is lenient, predicted.size() is harsh
        int tp = 0;
        int fp = 0;
        int fn = 0;
        // check if there is a gold match for each predicted value (+1 to tp if yes, +1 to fp if no)
        predicted = new HashSet<>(predicted);
        expected = new HashSet<>(expected);
        for (E predictedValue : predicted) {
            if (expected.stream().anyMatch(expectedValue -> isCorrect(predictedValue, expectedValue)))
                tp++;
            else fp++;
        }
        // check if every expected value was covered by the predictions (+1 fn for each absent value, tp were already counted in the previous loop)
        for (E expectedValue : expected) {
            if (predicted.stream().noneMatch(predictedValue -> isCorrect(predictedValue, expectedValue)))
                fn++;
        }
        return new RefComparison(tp, fp, fn, 0);
    }
}
