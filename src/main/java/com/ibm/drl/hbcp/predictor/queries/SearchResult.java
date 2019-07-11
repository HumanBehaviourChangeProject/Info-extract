package com.ibm.drl.hbcp.predictor.queries;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import org.jetbrains.annotations.NotNull;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.predictor.api.Scored;
import com.ibm.drl.hbcp.util.AttributeIdLookup;

import javax.json.Json;
import javax.json.JsonValue;

/**
 * A result returned by a query. Can be converted into JSON and returned by API endpoints.
 *
 * @see Query
 * @author marting
 */
public class SearchResult implements Comparable<SearchResult>, Scored, Jsonable {
    public final AttributeValueNode node;
    public final double score;

    public SearchResult(AttributeValueNode node, double score) {
        this.node = node; this.score = score;
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("attributeInfo", AttributeIdLookup.getInstance().getAttributeInfo(node.getAttribute().getId()).toJson())
                .add("value", node.getValue())
                .add("score", score)
                .build();
    }

    public String toString() { return node.toString(); }


    /** The natural ordering of results is the ordering of their score */
    @Override
    public int compareTo(@NotNull SearchResult o) {
        return Double.compare(score, o.score);
    }

    @Override
    public double getScore() {
        return score;
    }
}
