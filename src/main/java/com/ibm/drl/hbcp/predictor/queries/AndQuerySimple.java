package com.ibm.drl.hbcp.predictor.queries;

import com.google.common.collect.ImmutableList;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;

import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * A simpler implementation of AND-query, combining the results of different subqueries and returning a result supposed to
 * represent an "AND" constraint of them.
 *
 * @author marting
 */
public class AndQuerySimple extends AndQuery implements Query {

    public AndQuerySimple(List<? extends Query> queries, Predicate<AttributeValueNode> filter, int topK) {
        super(queries, filter, topK);
    }

    public AndQuerySimple(List<? extends Query> queries) {
        this(queries, x -> true, DEFAULT_TOP_K);
    }

    public AndQuerySimple(AndQuery andQuery) {
        this(andQuery.queries, andQuery.filter, andQuery.topK);
    }

    @Override
    public Iterable<SearchResult> search(NodeVecs vecs) {
        return getResults(vecs);
    }

    private List<SearchResult> getResults(NodeVecs vecs) {
        // get all subquery results first
        List<List<SearchResult>> subqueryResults = queries.stream()
                .map(q -> q.searchTopK(vecs, topK))
                .collect(Collectors.toList());
        // get all results, through all the combinations
        List<SearchResult> res = new ArrayList<>();
        for (List<SearchResult> subResultCombo : getAllCombinations(subqueryResults)) {
            List<SearchResult> topKResults = getResults(vecs, subResultCombo);
            res.addAll(topKResults);
        }
        Map<AttributeValueNode, SearchResult> bestRes = res.stream().collect(Collectors.toMap(
                SearchResult::getNode,
                sr -> sr,
                (sr1, sr2) -> sr1.getScore() > sr2.getScore() ? sr1 : sr2
        ));
        List<SearchResult> resSorted = new ArrayList<>(bestRes.values());
        resSorted.sort(Comparator.comparing(SearchResult::getScore).reversed());
        return resSorted.stream().limit(topK).collect(Collectors.toList());
    }

    private Iterable<List<SearchResult>> getAllCombinations(List<List<SearchResult>> subqueryResults) {
        return () -> new Iterator<List<SearchResult>>() {
            int[] nextIndices = new int[subqueryResults.size()];

            @Override
            public boolean hasNext() {
                return IntStream.range(0, nextIndices.length).allMatch(i -> nextIndices[i] < subqueryResults.get(i).size());
            }

            @Override
            public List<SearchResult> next() {
                if (!hasNext())
                    throw new NoSuchElementException("Combination list no longer has elements.");
                // get the result
                List<SearchResult> res = IntStream.range(0, nextIndices.length)
                        .mapToObj(i -> subqueryResults.get(i).get(nextIndices[i]))
                        .collect(Collectors.toList());
                // update the next indices
                OptionalInt changingIndice = IntStream.range(0, nextIndices.length)
                        .filter(i -> nextIndices[i] + 1 < subqueryResults.get(i).size())
                        .findFirst();
                if (changingIndice.isPresent()) {
                    int j = changingIndice.getAsInt();
                    for (int i = 0; i < j; i++) {
                        nextIndices[i] = 0;
                    }
                    nextIndices[j]++;
                } else {
                    // this is the end of the iterator
                    IntStream.range(0, nextIndices.length).forEach(i -> nextIndices[i]++);
                }
                return res;
            }
        };
    }
}