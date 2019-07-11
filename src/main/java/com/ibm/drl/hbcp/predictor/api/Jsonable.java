package com.ibm.drl.hbcp.predictor.api;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;

import javax.json.JsonValue;
import java.io.*;
import java.nio.charset.StandardCharsets;

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
}
