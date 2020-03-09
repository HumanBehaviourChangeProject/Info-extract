package com.ibm.drl.hbcp.predictor.evaluation.parameters;

import com.ibm.drl.hbcp.predictor.queries.SearchResult;

import java.util.List;
import java.util.function.Function;

public interface SubResultScoring extends Function<List<SearchResult>, Double> {

    public static double average(List<SearchResult> subResults) {
        // cross your fingers that it's never empty
        return subResults.stream().map(SearchResult::getScore).reduce(0.0, Double::sum) / subResults.size();
    }

    public static double multiply(List<SearchResult> subResults) {
        return subResults.stream().map(SearchResult::getScore).reduce(1.0, (x, y) -> x*y);
    }

    static SubResultScoring name(Function<List<SearchResult>, Double> f, String n) {
        return new SubResultScoring() {
            @Override
            public Double apply(List<SearchResult> searchResults) {
                return f.apply(searchResults);
            }

            @Override
            public String toString() {
                return n;
            }
        };
    }
}
