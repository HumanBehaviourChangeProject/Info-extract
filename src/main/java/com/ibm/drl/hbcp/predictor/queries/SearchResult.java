package com.ibm.drl.hbcp.predictor.queries;

import com.ibm.drl.hbcp.predictor.api.AttributeInfo;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.predictor.api.Scored;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import lombok.Data;
import org.jetbrains.annotations.NotNull;

import javax.json.Json;
import javax.json.JsonValue;

/**
 * A result returned by a query. Can be converted into JSON and returned by API endpoints.
 *
 * @see Query
 * @author marting
 */
@Data
public class SearchResult implements Comparable<SearchResult>, Scored, Jsonable {
    public final AttributeValueNode node;
    public final double score;

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("attributeInfo", new AttributeInfo(node.getAttribute()).toJson())
                .add("value", node.getValue())
                .add("score", score)
                .build();
    }

    public String toString() { return node.toString() + "(" + String.format("%.2f", score) + ")"; }

    /** The natural ordering of results is the ordering of their score */
    @Override
    public int compareTo(@NotNull SearchResult o) {
        return Double.compare(score, o.score);
    }
}
