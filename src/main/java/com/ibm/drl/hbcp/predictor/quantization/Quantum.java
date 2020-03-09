package com.ibm.drl.hbcp.predictor.quantization;

import org.jetbrains.annotations.NotNull;

/**
 * A quantum of a continuous quantity, more simply a range of continuous values.
 * @author marting
 * @param <T> The type of numbers the quantum handles (Double or Integer basically)
 */
public class Quantum<T extends Number & Comparable<T>> implements Comparable<Quantum<T>> {

    public final T start;
    public final T end;

    public Quantum(T start, T end) {
        this.start = start;
        this.end = end;
    }

    /** Compares the starts of the quantum, if the quantums are correctly built this is enough for simple sorting. */
    @Override
    public int compareTo(@NotNull Quantum<T> o) {
        return start.compareTo(o.start);
    }

    @SuppressWarnings("unchecked")
    @Override
    public boolean equals(Object obj) {
        if (obj == null) return false;
        if (!this.getClass().equals(obj.getClass())) return false;
        Quantum<T> other = (Quantum<T>)obj;
        return start.equals(other.start);
    }

    @Override
    public int hashCode() {
        return start.hashCode();
    }

    @Override
    public String toString() {
        if (start.equals(end))
            return start.toString();
        else return start + "-" + end;
    }
}
