package com.ibm.drl.hbcp.parser.pdf.textract;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;

public interface TextractBoundingBoxed extends Comparable<TextractBoundingBoxed> {

    double getLeft();

    double getTop();

    double HALF_PAGE_LEFT_THRESHOLD = 0.5;

    /**
     * Sort elements on the same page by their reading order
     * TODO: allow an approximate comparison of "left"s and "top"s, they seem overly precise in the JSON
     */
    @Override
    default int compareTo(@NotNull TextractBoundingBoxed textractBoundingBoxed) {
        // check if they're on the same half of the page
        if (areOnSameHalf(textractBoundingBoxed)) {
            // "top" is what matters, then "left"
            return Comparator.comparing(TextractBoundingBoxed::getTop)
                    .thenComparing(TextractBoundingBoxed::getLeft)
                    .compare(this, textractBoundingBoxed);
        } else {
            // "left" is what matters
            return Comparator.comparing(TextractBoundingBoxed::getLeft).compare(this, textractBoundingBoxed);
        }
    }

    default boolean areOnSameHalf(TextractBoundingBoxed textractBoundingBoxed) {
        return (getLeft() < HALF_PAGE_LEFT_THRESHOLD) == (textractBoundingBoxed.getLeft() < HALF_PAGE_LEFT_THRESHOLD);
    }
}
