package com.ibm.drl.hbcp.extraction.evaluation;

import org.jetbrains.annotations.NotNull;

/**
 * A simple evaluator testing the pure Java equality of the predicted and expected answers.
 *
 * @author marting
 */
public class EqualityEvaluator<Doc, E> implements PredicateUnarmifiedEvaluator<Doc, E> {
    @Override
    public boolean isCorrect(@NotNull E predicted, @NotNull E expected) {
        return predicted.equals(expected);
    }
}
