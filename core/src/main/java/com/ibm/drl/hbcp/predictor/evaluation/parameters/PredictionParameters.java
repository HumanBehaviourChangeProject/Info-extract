package com.ibm.drl.hbcp.predictor.evaluation.parameters;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import lombok.Data;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;


@Data
public class PredictionParameters {
    private final List<Integer> windowSize;
    private final List<Double> p;

    private final List<Integer> topK;
    private final List<Function<List<SearchResult>, Double>> subResultScoring;
    private final List<BiFunction<Double, Double, Double>> resultPlusSubsScoring;
    private final List<Boolean> flattenQuery;


    public static final PredictionParameters ALL = new PredictionParameters(
            Lists.newArrayList(1, 5),
            Lists.newArrayList(0.4, 0.5, 0.6),
            Lists.newArrayList(1, 5),
            Lists.newArrayList(
                    SubResultScoring.name(SubResultScoring::average, "average"),
                    SubResultScoring.name(SubResultScoring::multiply, "multiply")
            ),
            Lists.newArrayList(
                    ResultPlusSubsScoring.name(ResultPlusSubsScoring::average, "average"),
                    ResultPlusSubsScoring.name(ResultPlusSubsScoring::multiply, "multiply")
            ),
            Lists.newArrayList(false, true)
    );

    public List<PredictionParameters> getAllCombinations() {
        List<PredictionParameters> res = new ArrayList<>();
        for (Integer wsize : windowSize) {
            for (Double p : p) {
                for (Integer topK : topK) {
                    for (Function<List<SearchResult>, Double> subScoring : subResultScoring) {
                        for (BiFunction<Double, Double, Double> resultPlusSubsScoring : resultPlusSubsScoring) {
                            for (Boolean flattenQuery : flattenQuery) {
                                PredictionParameters param = new PredictionParameters(
                                        s(wsize),
                                        s(p),
                                        s(topK),
                                        s(subScoring),
                                        s(resultPlusSubsScoring),
                                        s(flattenQuery)
                                );
                                res.add(param);
                            }
                        }
                    }
                }
            }
        }
        return res;
    }

    private <E> List<E> s(E singleton) {
        return Collections.singletonList(singleton);
    }
}
