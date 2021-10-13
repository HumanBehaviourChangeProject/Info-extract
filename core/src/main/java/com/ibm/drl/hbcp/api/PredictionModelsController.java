package com.ibm.drl.hbcp.api;

import com.google.common.io.ByteStreams;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.ParametricGraphKerasDataPreparation;
import com.ibm.drl.hbcp.util.Environment;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.util.UriComponentsBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Enumeration;

/**
 * The REST controller for the Prediction Training API.
 *
 * Exposes endpoints allowing to train prediction models based on other annotation sets, list existing models,
 * manage them in various ways.
 *
 * @author marting
 */
@RestController("PredictorController")
public class PredictionModelsController {

    // default Node2Vec parameters
    public static final int DEFAULT_DIMENSION = 100;
    public static final int DEFAULT_N2V_WINDOW = 10;
    public static final double DEFAULT_N2V_P = 0.1;
    public static final double DEFAULT_N2V_Q = 0.9;

    // hbcp-prediction-experiments Flask API call parameters
    public static final String PROTOCOL = "http";
    public static final String HOST = Environment.getPredictionURL();
    public static final int PORT = Environment.getPredictionPort();
    public static final String ENDPOINT_BASE = "/hbcp/api/v1.0/predict/models/";
    public static final String TRAIN_ENDPOINT = ENDPOINT_BASE + "train/";

    private static final Logger logger = LoggerFactory.getLogger(PredictionModelsController.class);

    @ApiOperation(value = "Trains a new prediction model based on user annotations.",
            notes = "Trains a new prediction model (regressed outcome value + confidence score) from annotations provided in EPPI JSON format.")
    @RequestMapping(value = "/api/predict/models/train",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces="application/json;charset=utf-8")
    public String train(
            @ApiParam(value = "Name of the trained model")
            @RequestParam(value="modelName") String modelName,
            @ApiParam(value = "Description of the trained model")
            @RequestParam(value="modelDescription", required = false, defaultValue = "") String modelDescription,
            @ApiParam(value = "Number of training epochs")
            @RequestParam(value="epochs", required = false, defaultValue = "10") int epochs,
            @ApiParam(value = "A json file set of attribute-value pairs representing Behavior Change intervention scenarios serving as input")
            @RequestParam(value="annotationFile") MultipartFile annotationFile) throws MalformedURLException, URISyntaxException {
        // reads the annotations
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = null;
        try (InputStream jsonFile = annotationFile.getInputStream()) {
            JSONRefParser parser = new JSONRefParser(jsonFile, false);
            annotations = parser.getAttributeValuePairs();
        } catch (IOException e) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST, "Malformed annotation JSON file", e);
        }
        // prepares the data required to train the model
        ParametricGraphKerasDataPreparation.ResourcePaths trainAndVecFiles = null;
        try {
            trainAndVecFiles = ParametricGraphKerasDataPreparation.predictionResources4ApiStart(annotations, DEFAULT_DIMENSION,
                    DEFAULT_N2V_WINDOW, DEFAULT_N2V_P, DEFAULT_N2V_Q);
        } catch (IOException | InterruptedException e) {
            throw new ResponseStatusException(
                    HttpStatus.INTERNAL_SERVER_ERROR, "Failed merging of embedding files", e);
        }
        // calls the python API
        return callProxyTrainRequest(modelName, modelDescription, epochs,
                trainAndVecFiles.getTrainFile(), trainAndVecFiles.getMergedVecFile());
    }

    @ApiOperation(value = "Lists all the available prediction models.",
            notes = "Returns a JSON array of the prediction models (names and descriptions) available on this server.")
    @GetMapping(value = "/api/predict/models/list")
    public void list(HttpServletRequest request, HttpServletResponse response) throws IOException {
        URL url = getURL(ENDPOINT_BASE + "list/");
        forward(request, response, url.toString());
    }

    @ApiOperation(value = "Removes the specified model.",
            notes = "Removes the model from the server and returns its name/description if successful")
    @GetMapping(value = "/api/predict/models/remove", produces = "application/json;charset=utf-8")
    public String remove(@ApiParam(value = "Name of the model to remove")
                       @RequestParam(value="modelName") String modelName) throws IOException {
        HttpHeaders headers = new HttpHeaders();
        //headers.set(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE);
        UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(getURL(ENDPOINT_BASE + "remove/").toString())
                .queryParam("model_name", modelName);
        HttpEntity<?> entity = new HttpEntity<>(headers);
        HttpEntity<String> response = new RestTemplate().exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                String.class);
        return response.getBody();
    }

    private String callProxyTrainRequest(String modelName, String modelDescription, int epochs, File trainFile, File mergedVecFile) throws MalformedURLException, URISyntaxException {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.MULTIPART_FORM_DATA);

        MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
        body.add("model_name", modelName);
        body.add("model_description", modelDescription);
        body.add("epochs", String.valueOf(epochs));
        body.add("train_file", new FileSystemResource(trainFile));
        body.add("merged_vec_file", new FileSystemResource(mergedVecFile));

        HttpEntity<MultiValueMap<String, Object>> requestEntity = new HttpEntity<>(body, headers);
        URL trainPredictionApiUrl = getURL(TRAIN_ENDPOINT);
        RestTemplate restTemplate = new RestTemplate();
        ResponseEntity<String> response = restTemplate.postForEntity(trainPredictionApiUrl.toURI(), requestEntity, String.class);
        logger.info("Response code: " + response.getStatusCode());
        return response.getBody();
    }

    private URL getURL(String endpoint) throws MalformedURLException {
        return new URL(PROTOCOL, HOST, PORT, endpoint);
    }

    private void forward(HttpServletRequest request, HttpServletResponse response, String urlString) throws IOException {
        URL url = new URL(urlString);
        String method = request.getMethod();
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        con.setRequestMethod(method);
        for (Enumeration<String> names = request.getHeaderNames(); names.hasMoreElements();) {
            String name = names.nextElement();
            for (Enumeration<String> values = request.getHeaders(name); values.hasMoreElements();) {
                String value = values.nextElement();
                con.setRequestProperty(name, value);
            }
        }
        // write body if POST method
        if ("POST".equals(method)) {
            con.setDoOutput(true);
            ByteStreams.copy(request.getInputStream(), con.getOutputStream());
        }
        // get response
        response.setStatus(con.getResponseCode());
        ByteStreams.copy(con.getInputStream(), response.getOutputStream());
    }
}
