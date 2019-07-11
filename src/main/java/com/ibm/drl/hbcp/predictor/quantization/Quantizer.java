package com.ibm.drl.hbcp.predictor.quantization;

/**
 * Turns a continuous number into a quantum (a value part of a smaller, often finite set).
 * @author marting
 * @param <T> The type of numbers the quantum handles (Double or Integer basically)
 */
public interface Quantizer<T extends Number & Comparable<T>> {

    /** Returns a quantized/discretized version of a number */
    Quantum<T> quantize(T number);
}
