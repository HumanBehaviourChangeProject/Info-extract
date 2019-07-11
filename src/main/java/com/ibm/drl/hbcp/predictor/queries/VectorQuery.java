package com.ibm.drl.hbcp.predictor.queries;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.core.wvec.WordVec;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Asks for the closest vector(s) in term of cosine sim (nearest neighbors)
 * @author marting
 */
public class VectorQuery implements Query {

    public final WordVec vector;
    public final int topK;

    public VectorQuery(WordVec vector, int topK) {
        this.vector = vector;
        this.topK = topK;
    }

    @Override
    public List<SearchResult> search(NodeVecs vecs) {
        List<WordVec> nns = vecs.getNearestNeighbors(vector, topK);
        return nns.stream()
                .map(wv -> new SearchResult(
                        AttributeValueNode.parse(wv.getWord()),
                        wv.getQuerySim())) //TODO: do the correct sorting here
                .collect(Collectors.toList());
    }

    public int getTopK() {
        return topK;
    }
}
