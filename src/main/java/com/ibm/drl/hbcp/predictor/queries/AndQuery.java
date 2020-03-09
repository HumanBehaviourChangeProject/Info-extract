package com.ibm.drl.hbcp.predictor.queries;

import com.google.common.collect.ImmutableList;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.core.wvec.WordVec;
import com.ibm.drl.hbcp.predictor.evaluation.parameters.ResultPlusSubsScoring;
import com.ibm.drl.hbcp.predictor.evaluation.parameters.SubResultScoring;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import org.apache.commons.lang3.StringUtils;

import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * An AND-query, combining the results of different subqueries and returning a result supposed to
 * represent an "AND" constraint of them.
 *
 * @author marting
 */
public class AndQuery implements Query {

    public final List<? extends Query> queries;
    public final int topK;
    // a filter on the results of the query, will drive the search
    final Predicate<AttributeValueNode> filter;

    public static final int DEFAULT_TOP_K = 50;
    protected static final int BEAM_WIDTH_FACTOR = 10;

    // HIDEOUS. TODO: if it turns out to be useful again, reimplement that properly
    public static Function<List<SearchResult>, Double> subResultScoring = SubResultScoring::multiply;
    public static BiFunction<Double, Double, Double> resultPlusSubsScoring = ResultPlusSubsScoring::multiply;

    public AndQuery(List<? extends Query> queries, Predicate<AttributeValueNode> filter, int topK) {
        this.queries = ImmutableList.copyOf(queries);
        this.filter = filter;
        this.topK = topK;
    }

    public AndQuery(List<? extends Query> queries, int topK) {
        this(queries, x -> true, topK);
    }

    public AndQuery(List<? extends Query> queries) {
        this(queries, x -> true, DEFAULT_TOP_K);
    }

    public AndQuery withFilter(Predicate<AttributeValueNode> filter) {
        return new AndQuery(queries, filter, topK);
    }

    public static AndQuery forOutcomeValue(List<? extends Query> queries) {
        return new AndQuery(queries, avp -> avp.getAttribute().getName().contains("Outcome value"), DEFAULT_TOP_K);
    }

    public static AndQuery flatten(AndQuery andQueryOfAndQueries) {
        List<AndQuery> subqueries = andQueryOfAndQueries.queries.stream().map(q -> (AndQuery)q).collect(Collectors.toList());
        List<Query> leaves = subqueries.stream().flatMap(aq -> aq.queries.stream()).collect(Collectors.toList());
        return new AndQuery(leaves);
    }

    public AndQuery forOutcomeValue() {
        return forOutcomeValue(queries);
    }

    @Override
    public String toString() {
        return "And(" + StringUtils.join(queries.stream().map(Object::toString).collect(Collectors.toList()), ", ") + ")";
    }

    @Override
    public Iterable<SearchResult> search(NodeVecs vecs) {
        // create a nice iterator that streams the best results of the AND-query
        return () -> getResultIterator(vecs);
    }

    /**
     * Streams the best results of the AND query (so you get as many as you need)
     * but with a restricted depth-first search (to reduce complexity). (exponential --to-- linear)
     * Getting the global optimum isn't insured, score order discrepancies may be observed
     * if you require enough results.
     * @param vecs
     * @return
     */
    protected Iterator<SearchResult> getResultIterator(NodeVecs vecs) {
        // get all subquery results first
        List<Iterable<SearchResult>> subqueryResults = queries.stream()
                .map(q -> q.search(vecs))
                .collect(Collectors.toList());
        // get the iterator producing the best subresult combinations
        Iterator<List<SearchResult>> subresultCombos = getSubqueryResultCombinationIterator(subqueryResults);
        return new Iterator<SearchResult>() {
            private List<SearchResult> bestResultBuffer = new ArrayList<>();
            private int nextResultIndex = 0;

            @Override
            public boolean hasNext() {
                return nextResultIndex < bestResultBuffer.size() || subresultCombos.hasNext();
            }

            @Override
            public SearchResult next() {
                // if we still have some results from the last batch left, return that
                if (nextResultIndex < bestResultBuffer.size()) {
                    return bestResultBuffer.get(nextResultIndex++);
                } else { // compute the next batch of results using the N next best combos of subresults
                    boolean hadNext = subresultCombos.hasNext();
                    bestResultBuffer = getBestResultsFromRestrictedSubresultCombos(vecs, subresultCombos, getBestFirstComboBatchCount(queries.size()));
                    nextResultIndex = 0;
                    if (bestResultBuffer.isEmpty()) {
                        System.out.println(hadNext);
                        throw new NoSuchElementException(); // this shouldn't happen if you called hasNext() correctly
                    }
                    return bestResultBuffer.get(nextResultIndex++);
                }
            }
        };
    }

    protected List<SearchResult> getBestResultsFromRestrictedSubresultCombos(NodeVecs vecs, Iterator<List<SearchResult>> subresultCombos, int comboLimit) {
        // first get the next best subresult combos (simple best-first search approach)
        List<List<SearchResult>> bestCombos = getNextBestSubresultCombos(subresultCombos, comboLimit);
        Map<AttributeValueNode, SearchResult> bestResultPerNode = new HashMap<>();
        // for each combo, compute the results of the AND-query
        for (List<SearchResult> subresults : bestCombos) {
            List<SearchResult> results = getResults(vecs, subresults);
            // keep only the best result per node (so if we see the node twice, keep only the best score)
            for (SearchResult result : results) {
                bestResultPerNode.computeIfPresent(result.node, ((id, searchResult) -> result.score > searchResult.score ? result : searchResult));
                bestResultPerNode.putIfAbsent(result.node, result);
            }
        }
        // sort the results, by descending score
        List<SearchResult> res = new ArrayList<>(bestResultPerNode.values());
        res.sort(Comparator.reverseOrder());
        return res;
    }

    /** compute the centroid of the subresults, and return the NNs in the graph */
    protected List<SearchResult> getResults(NodeVecs vecs, List<SearchResult> subresults) {
        WordVec centroid = getCentroid(subresults.stream().map(sr -> sr.node).collect(Collectors.toList()), vecs);

        if (centroid == null)
            return new ArrayList<>();

        // search nearest neighbors
        List<SearchResult> res = new VectorQuery(centroid, filter, topK).search(vecs);
        // adjust their scores if needed (using the scores of the subresults)
        return res.stream()
                .map(sr -> new SearchResult(sr.node, getAdjustedResultScore(sr.score, subresults)))
                .collect(Collectors.toList());
    }

    /** Average of original score + avg(subresult scores) TODO: find something better */
    protected double getAdjustedResultScore(double originalResultScore, List<SearchResult> subresults) {
        // subresults should never be empty here
        double avgSubresults = subResultScoring.apply(subresults);
        double res = resultPlusSubsScoring.apply(originalResultScore, avgSubresults);
        // debug here
        return res;
    }

    protected List<List<SearchResult>> getNextBestSubresultCombos(Iterator<List<SearchResult>> subresultCombos, int comboLimit) {
        List<List<SearchResult>> bestCombos = new ArrayList<>();
        while (subresultCombos.hasNext() && bestCombos.size() < comboLimit)
            bestCombos.add(subresultCombos.next());
        return bestCombos;
    }

    protected Iterator<List<SearchResult>> getSubqueryResultCombinationIterator(List<Iterable<SearchResult>> subqueryResults) {
        // start to iterate on subquery results
        List<Iterator<SearchResult>> iterators = subqueryResults.stream()
                .map(Iterable::iterator)
                .collect(Collectors.toList());
        return new Iterator<List<SearchResult>>() {

            private List<SearchResult> previousCombo = null;
            private Map<Iterator<SearchResult>, SearchResult> peekedElements = new HashMap<>();

            @Override
            public boolean hasNext() {
                if (!peekedElements.isEmpty()) {
                    for (Iterator<SearchResult> iterator : iterators) {
                        if (!peekedElements.containsKey(iterator) && !iterator.hasNext())
                            return false;
                    }
                    return true;
                } else return !iterators.isEmpty() && iterators.stream().allMatch(Iterator::hasNext);
            }

            @Override
            public List<SearchResult> next() {
                if (!hasNext()) throw new NoSuchElementException();
                // refill peeked elements as needed
                for (Iterator<SearchResult> iterator : iterators) {
                    if (!peekedElements.containsKey(iterator) && iterator.hasNext())
                        peekedElements.put(iterator, iterator.next());
                }
                if (previousCombo == null) {
                    // this is the top 1 combo (peeked elements will be empty again after that)
                    previousCombo = new ArrayList<>();
                    for (Iterator<SearchResult> iterator : iterators) {
                        previousCombo.add(peekedElements.remove(iterator));
                    }
                    return previousCombo;
                } else {
                    // pick the best peeked subquery result (the one that degrades the previous combo the least)
                    Map.Entry<Integer, SearchResult> bestNext = null;
                    double bestNextScore = Double.NEGATIVE_INFINITY;
                    for (int i = 0; i < previousCombo.size(); i++) {
                        SearchResult replacement = peekedElements.get(iterators.get(i));
                        double score = getResultComboScoreWithReplacement(previousCombo, replacement, i);
                        if (score >= bestNextScore) {
                            bestNextScore = score;
                            bestNext = new AbstractMap.SimpleEntry<>(i, replacement);
                        }
                    }
                    // apply the replacement (and pop it from the peeked)
                    previousCombo = getListWithReplacement(previousCombo, bestNext.getValue(), bestNext.getKey());
                    peekedElements.remove(iterators.get(bestNext.getKey()));
                    return previousCombo;
                }
            }
        };
    }

    protected double getResultComboScoreWithReplacement(List<SearchResult> results, SearchResult replacement, int index) {
        List<SearchResult> listWithReplacement = getListWithReplacement(results, replacement, index);
        return subResultScoring.apply(listWithReplacement);
    }

    // unused at the moment, might be useful for debugging the above in case of NPE
    protected double getResultComboScore(List<SearchResult> results) {
        double res = 0.0;
        for (SearchResult result : results) res += result.score;
        res /= results.size();
        return res;
    }

    protected <T> List<T> getListWithReplacement(List<T> results, T replacement, int index) {
        List<T> res = new ArrayList<>();
        for (int i = 0; i < results.size(); i++) {
            if (i == index)
                res.add(replacement);
            else res.add(results.get(i));
        }
        return res;
    }

    protected int getBestFirstComboBatchCount(int queryCount) {
        // compute the minimum number of results to produce 'topK' different combinations
        // this is the n-th root of the top K, (ceil'ed to be sure to have at least topK)
        int minResultsPerQuery = Math.max(1, (int)Math.ceil(Math.pow(topK, 1.0 / queryCount)));
        // then we multiply that by a factor, to obtain a pool of combinations large enough to compute topK good results
        return minResultsPerQuery * BEAM_WIDTH_FACTOR;
    }

    private WordVec getCentroid(List<AttributeValueNode> nodes, NodeVecs vecs) {
        if (nodes.isEmpty()) return null;
        WordVec res = new WordVec(getVectorDimension(nodes.get(0), vecs));
        // TODO: handle cases where not found in 'vecs' (if even possible, but likely not)
        for (AttributeValueNode node : nodes) {
            res = WordVec.sum(res, getNodeInstanceVector(vecs, node.toString()));
        }
        // TODO: change to float
        res.scalarMutiply(1.0f / nodes.size());
        return res;
    }

    private int getVectorDimension(AttributeValueNode node, NodeVecs vecs) {
        return getNodeInstanceVector(vecs, node.toString()).getDimension();
    }

    private WordVec getNodeInstanceVector(NodeVecs vecs, String word) {
        WordVec res = vecs.getNodeInstanceVector(word);
        if (res == null) {
            System.err.println(word + " wasn't found in the vectors.");
        }
        return res;
    }
}
