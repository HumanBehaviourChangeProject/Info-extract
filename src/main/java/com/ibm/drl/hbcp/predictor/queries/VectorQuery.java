package com.ibm.drl.hbcp.predictor.queries;

import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.core.wvec.WordVec;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import lombok.Getter;

import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

/**
 * Asks for the closest vector(s) in term of cosine sim (nearest neighbors)
 * @author marting
 */
public class VectorQuery implements Query {

    private final WordVec vector;
    @Getter
    private final int topK;
    private final Predicate<AttributeValueNode> filter;

    public VectorQuery(WordVec vector, Predicate<AttributeValueNode> filter, int topK) {
        this.vector = vector;
        this.filter = filter;
        this.topK = topK;
    }

    @Override
    public List<SearchResult> search(NodeVecs vecs) {
        // return ALL vectors (this method already sorts)
        List<WordVec> nns = vecs.getNearestNeighbors(vector, Integer.MAX_VALUE);
        // filter here
        return nns.stream()
                .map(wv -> new SearchResult(
                        AttributeValueNode.parse(wv.getWord()),
                        wv.getQuerySim()))
                .filter(sr -> filter.test(sr.getNode()))
                .sorted(Comparator.reverseOrder())
                .limit(topK)
                .collect(Collectors.toList());
    }
}
