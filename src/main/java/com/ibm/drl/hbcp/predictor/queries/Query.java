package com.ibm.drl.hbcp.predictor.queries;

import com.google.common.collect.Iterables;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;

/**
 * A query for attribute instance nodes.
 * To be executed on a NodeVecs object, a repository of vectors for attribute instances.
 *
 * @see NodeVecs
 * @author marting
 */
public interface Query {

    /**
     * Execute the query on the provided vectors.
     * @param vecs the repository containing the node vectors
     * @return Iterable search results (so that you can request as many as you want, and stop when you have enough)
     */
    Iterable<SearchResult> search(NodeVecs vecs);

    /**
     * Execute the query on the provided vectors and returns the top K results.
     * @param vecs the repository containing the node vectors
     * @param topK limits the results to K instances
     * @return list of results of size at most K
     */
    default List<SearchResult> searchTopK(NodeVecs vecs, int topK) {
        List<SearchResult> res = new ArrayList<>();
        for (SearchResult sr : search(vecs)) {
            if (topK >= 0 && res.size() >= topK) return res;
            res.add(sr);
        }
        return res;
    }

    /**
     * Creates a query returning filtered results according to a provided predicate
     * @param validityPredicate the condition for search results to be kept (true: keep, false: throw away)
     * @return new query equivalent to the original, but with a filter on results
     */
    default Query filteredWith(Predicate<SearchResult> validityPredicate) {
        final Query originalQuery = this;
        return (vecs) -> Iterables.filter(originalQuery.search(vecs), validityPredicate::test);
    }
}
