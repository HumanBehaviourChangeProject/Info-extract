package com.ibm.drl.hbcp.extraction;

import org.jetbrains.annotations.NotNull;

/**
 * An object with a score.
 *
 * @author marting
 */
public interface Scored extends Comparable<Scored> {

    double getScore();

    @Override
    default int compareTo(@NotNull Scored o) {
        return Double.compare(getScore(), o.getScore());
    }
}
