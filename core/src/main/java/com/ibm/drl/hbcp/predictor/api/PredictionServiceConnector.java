package com.ibm.drl.hbcp.predictor.api;

import com.google.common.collect.Lists;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.util.Environment;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class PredictionServiceConnector {

    private final String host;
    private final int port;
    private static final String PROTOCOL = "http";
    private static final String ENDPOINT = "/hbcp/api/v1.0/predict/outcome/";

    private static final Logger log = LoggerFactory.getLogger(PredictionServiceConnector.class);

    // to parse the output of the REST API (in JSON)
    private final Gson gson = new Gson();

    public PredictionServiceConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Gets a connector to a Prediction service run on localhost and port 5000 */
    public static PredictionServiceConnector createForLocalService() {
        return new PredictionServiceConnector(Environment.getPredictionURL(), Environment.getPredictionPort());
    }

    public Optional<PredictionWithConfidence> requestPrediction(List<? extends AttributeValuePair> avps) {
        // format avps into prediction API input strings
        String queryString = avps.stream()
                .map(AttributeValueNode::new)
                .map(AttributeValueNode::toString)
                .collect(Collectors.joining("-"));
        HttpURLConnection con = null;
        try {
            // query the prediction API
            URL predictionUrl = new URL(PROTOCOL, host, port, ENDPOINT + queryString);
            con = (HttpURLConnection) predictionUrl.openConnection();
            con.setRequestMethod("GET");
            // get the response
            int status = con.getResponseCode();
            if (status < 300) {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getInputStream()))) {
                    PredictionWithConfidence result = gson.fromJson(in, new TypeToken<PredictionWithConfidence>() { }.getType());
                    return Optional.of(result);
                }
            } else {
                try (BufferedReader in = new BufferedReader(new InputStreamReader(con.getErrorStream()))) {
                    log.error("Error in prediction API response for query: {}", queryString);
                    String line;
                    while ((line = in.readLine()) != null) {
                        log.error(line);
                    }
                }
                return Optional.empty();
            }
        } catch (IOException e) {
            log.error("Error while querying the prediction API with: " + queryString, e);
            return Optional.empty();
        } finally {
            if (con != null) con.disconnect();
        }
    }

    @Value
    public static class PredictionWithConfidence {
        double value;
        double conf;
    }

    public static void main(String[] args) throws IOException {
        // test with the prediction docker container running locally on port 5000
        PredictionServiceConnector con = new PredictionServiceConnector("127.0.0.1", 5000);
        List<AttributeValuePair> query = Lists.newArrayList(
                new AttributeValuePair(Attributes.get().getFromId("5579088"), "25"),
                new AttributeValuePair(Attributes.get().getFromId("3675717"), "1"),
                new AttributeValuePair(Attributes.get().getFromId("5594105"), "5"),
                new AttributeValuePair(Attributes.get().getFromId("3673271"), "1"),
                new AttributeValuePair(Attributes.get().getFromId("4087191"), "6.5")
        );
        Optional<PredictionWithConfidence> res = con.requestPrediction(query);
        System.out.println(res);
    }
}
