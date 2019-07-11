package com.ibm.drl.hbcp.predictor.api;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import java.util.List;

/**
 * A ranked list of results, to be returned in JSON format by the API.
 *
 * @param <T> A "result" object, that should contain a score and be able to be converted to JSON
 * @author marting
 * */
public class RankedResults<T extends Jsonable & Scored> implements Jsonable {
    protected final List<T> results;

    public RankedResults(List<T> results) {
        this.results = results;
    }

    @Override
    public JsonValue toJson() {
        JsonArrayBuilder json = Json.createArrayBuilder();
        int rank = 1;
        for (T result : results) {
            JsonValue value = Json.createObjectBuilder()
                    .add("item", result.toJson())
                    .add("rank", rank++)
                    .add("score", result.getScore())
                    .build();
            json.add(value);
        }
        return json.build();
    }
}
