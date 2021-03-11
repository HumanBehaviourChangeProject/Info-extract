package com.ibm.drl.hbcp.predictor.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.json.Json;
import javax.json.JsonArray;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.List;
import java.util.Map;

/**
 * An object that can be serialized to JSON.
 * Provides the ability to pretty-print the JSON value (with additional line breaks, for example).
 * @author marting
 */
public interface Jsonable {
    // JSON pretty printing
    Gson gson = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .create();
    JsonParser jp = new JsonParser();

    /** Converts the object to its JSON equivalent. */
    JsonValue toJson();

    /** Returns a pretty-print String version of this object's JSON value */
    default String toPrettyString() {
        return toPrettyString(toJson());
    }

    static String toPrettyString(JsonValue jsonValue) {
        String rawJson = jsonValue.toString();
        InputStream stream = new ByteArrayInputStream(rawJson.getBytes(StandardCharsets.UTF_8));
        try {
            try (BufferedReader br = new BufferedReader(new InputStreamReader(stream, StandardCharsets.UTF_8))) {
                // pretty print
                JsonElement je = jp.parse(br);
                String res = gson.toJson(je);
                return res;
            }
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    static <T extends Jsonable> String getJsonList(List<T> options) {
        return toPrettyString(getJsonArrayFromCollection(options));
    }

    static <T extends Jsonable> String getJsonStringToListMap(Map<String, List<T>> map) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (String key : map.keySet()) {
            builder.add(
                    Json.createObjectBuilder()
                            .add("key", key)
                            .add("value", getJsonArrayFromCollection(map.get(key)))
                            .build()
            );
        }
        return toPrettyString(builder.build());
    }

    static <T extends Jsonable> JsonArray getJsonArrayFromCollection(Collection<T> list) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (T element : list) {
            builder.add(element.toJson());
        }
        return builder.build();
    }

    static JsonArray getJsonArrayFromStrings(Collection<String> elements) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (String element : elements) {
            builder.add(element);
        }
        return builder.build();
    }

    static String getJsonStringArray(Collection<String> elements) {
        return toPrettyString(getJsonArrayFromStrings(elements));
    }
}
