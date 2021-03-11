package com.ibm.drl.hbcp.predictor.queries;

import javax.json.Json;
import javax.json.JsonValue;

import org.jetbrains.annotations.NotNull;

import com.ibm.drl.hbcp.predictor.api.AttributeInfo;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.predictor.api.Scored;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;

import lombok.EqualsAndHashCode;
import lombok.Getter;

/**
 * A result returned by a query. Can be converted into JSON and returned by API endpoints.
 *
 * @see Query
 * @author marting
 */
@EqualsAndHashCode
public class SearchResult implements Comparable<SearchResult>, Scored, Jsonable {
    @Getter
    public final AttributeValueNode node;
    @Getter
    public final double score;
    @Getter
    private String followUp; // can be null

    public SearchResult(AttributeValueNode node, double score, String followUp) {
        this.node = node;
        this.score = score;
        this.followUp = followUp;
    }

    public SearchResult(AttributeValueNode node, double score) {
        this(node, score, "6 months");
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("attributeInfo", AttributeInfo.fromAttribute(node.getAttribute()).toJson())
                .add("value", node.getValue())
                .add("score", score)
                .build();
    }

    @Override
    public String toString() { return node.toString() + "(" + String.format("%.2f", score) + ")"; }

    /** The natural ordering of results is the ordering of their score */
    @Override
    public int compareTo(@NotNull SearchResult o) {
        return Double.compare(score, o.score);
    }
}
