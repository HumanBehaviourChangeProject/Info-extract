package com.ibm.drl.hbcp.predictor.quantization;

/**
 * A basic quantizer that returns an interval reduced to the number itself.
 * @param <T>
 * @author marting
 */
public class IdentityQuantizer<T extends Number & Comparable<T>> implements Quantizer<T> {

    @Override
    public Quantum<T> quantize(T number) {
        return new Quantum<>(number, number);
    }
}
