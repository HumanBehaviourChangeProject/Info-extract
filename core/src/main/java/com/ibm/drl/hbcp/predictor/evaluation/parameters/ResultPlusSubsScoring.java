package com.ibm.drl.hbcp.predictor.evaluation.parameters;

import java.util.function.BiFunction;

public interface ResultPlusSubsScoring extends BiFunction<Double, Double, Double> {

    public static double average(double resultNodeScore, double subResultScore) {
        return (resultNodeScore + subResultScore) / 2.0;
    }

    public static double multiply(double resultNodeScore, double subResultScore) {
        return resultNodeScore * subResultScore;
    }

    static ResultPlusSubsScoring name(BiFunction<Double, Double, Double> f, String n) {
        return new ResultPlusSubsScoring() {
            @Override
            public Double apply(Double d1, Double d2) {
                return f.apply(d1, d2);
            }

            @Override
            public String toString() {
                return n;
            }
        };
    }

}
