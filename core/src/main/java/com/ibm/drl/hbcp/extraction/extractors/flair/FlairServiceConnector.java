package com.ibm.drl.hbcp.extraction.extractors.flair;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.passages.SimplePassage;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.util.Environment;

import lombok.Data;

/**
 * Connects to a Python Flair REST API to get span-entities from sentences.
 * Also provides methods to convert those span-entities into extractor candidates.
 *
 * @author yhou, mgleize (refactoring)
 */
public class FlairServiceConnector {

    private final String host;
    private final int port;
    private final String protocol = "http";
    private final String endpoint = "/api/v1/extractEntitiesMultiSent";

    private static final int CROPPED_OUTPUT_LENGTH = 200;

    static Logger log = LoggerFactory.getLogger(FlairServiceConnector.class);

    // to parse the output of the REST API (in JSON)
    private final Gson gson = new Gson();

    public FlairServiceConnector(String host, int port) {
        this.host = host;
        this.port = port;
    }

    /** Gets a connector to a Flair service run on localhost and port 5000 */
    public static FlairServiceConnector createForLocalService() {
        return new FlairServiceConnector(Environment.getFlairURL(), Environment.getFlairPort());
    }

    /** Send sentences as input to the Flair REST API and get as output one JSON string per line */
    public List<String> requestFlairResultsForSentences(List<String> sentences){
        List<String> res = new ArrayList<>();
        HttpURLConnection conn = null;
        DataOutputStream os = null;
        try{
            URL url = new URL(protocol, host, port, endpoint);
//            String a1 = "Given that there are many Asian language groups in the United States";
//            String a2 = "Given that there are many Asian language groups in the United States and Ireland, we chose three language groups Chinese, Korean, and Vietnamese for practical reasons";
//            List<String> sents = new ArrayList();
//            sents.add(a1);
//            sents.add(a2);
//            String input = "{\"sentences\":" + "[" + "\"" + a1 + "\"" + "," + "\"" + a2 + "\"" + "]" + "}" ;
            String input = "{\"sentences\":" + "[" ;
            for(int i=0; i<sentences.size()-1; i++){
                input = input + "\"" + sentences.get(i) + "\"" + ",";
            }
            input = input + "\"" + sentences.get(sentences.size()-1) + "\""  + "]" + "}";

//            System.err.println(input);
            byte[] postData = input.getBytes(StandardCharsets.UTF_8);
            conn = (HttpURLConnection) url.openConnection();
            conn.setDoOutput(true);
            conn.setRequestMethod("POST");
            conn.setRequestProperty("Content-Type", "application/json");
            conn.setRequestProperty( "charset", "utf-8");
            conn.setRequestProperty("Content-Length", Integer.toString(input.length()));
            os = new DataOutputStream(conn.getOutputStream());
            os.write(postData);
            os.flush();

            if (conn.getResponseCode() != 200) {
                throw new RuntimeException("Failed : HTTP error code : "
                        + conn.getResponseCode());
            }

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            String output;

            log.info("Output received from Flair service: ");
            while ((output = br.readLine()) != null) {
                log.info("Cropped output ({} chars): {}", CROPPED_OUTPUT_LENGTH,
                        StringUtils.abbreviate(output, CROPPED_OUTPUT_LENGTH));
                res.add(output);
            }
            conn.disconnect();

        } catch (IOException e) {
        	log.error("Impossible to query the service.", e);
        } finally
        {
            if(conn != null){
                conn.disconnect();
            }
        }
        return res;
    }

    /** Gets the result candidates from a docName (to construct the AVP objects) and the reader to the JSON output */
    public List<List<CandidateInPassage<ArmifiedAttributeValuePair>>> getFlairUnarmifiedCandidates(List<String> docNames,
                                                                                                   Reader reader,
                                                                                                   List<Integer> sentencesPerDoc) {
        List<List<CandidateInPassage<ArmifiedAttributeValuePair>>> res = new ArrayList<>();
        int i = 0;
        for (Map<String, List<ValueWithContext>> extractionForDoc : extractPrediction(reader, sentencesPerDoc)) {
            String docName = docNames.get(i++);
            List<CandidateInPassage<ArmifiedAttributeValuePair>> candidatesForDoc = getFlairUnarmifiedCandidates(docName, extractionForDoc);
            res.add(candidatesForDoc);
        }
        return res;
    }

    /** Reads the output of the Flair REST service and index span annotations by their attribute */
    private List<Map<String, List<ValueWithContext>>> extractPrediction(String jsonfile, List<Integer> sentencesPerDoc) throws IOException, Exception {
        InputStream inputStream = new FileInputStream(new File(jsonfile));
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return extractPrediction(reader, sentencesPerDoc);
        }
    }

    /** Reads the output of the Flair REST service and index span annotations by their attribute */
    private List<Map<String, List<ValueWithContext>>> extractPrediction(Reader reader, List<Integer> sentencesPerDoc) {
        List<Map<String, List<ValueWithContext>>> res = new ArrayList<>();
        Type type = new TypeToken<List<SentenceEntity>>() { }.getType();
        List<SentenceEntity> result = gson.fromJson(reader, type);
        // check that the result contain as many sentences as the query
        checkFlairOutputSentenceCount(result.size(), sentencesPerDoc);
        // gather entities
        Iterator<SentenceEntity> iteratorOnSentences = result.iterator();
        for (int sentenceCountForDoc : sentencesPerDoc) {
            Map<String, List<ValueWithContext>> entitiesPerDoc = new HashMap<>();
            for (int i = 0; i < sentenceCountForDoc; i++) {
                if (iteratorOnSentences.hasNext()) { // should always be true
                    SentenceEntity predict = iteratorOnSentences.next();
                    if (!predict.entities.isEmpty()) {
                        for (SentenceEntity.Entity entity : predict.entities) {
                            String attributeName = getAttributeName(entity.type);
                            entitiesPerDoc.putIfAbsent(attributeName, new ArrayList<>());
                            entitiesPerDoc.get(attributeName).add(new ValueWithContext(entity.text, predict.text, Double.parseDouble(entity.confidence)));
                        }
                    }
                }
            }
            res.add(entitiesPerDoc);
        }
        return res;
    }

    private List<CandidateInPassage<ArmifiedAttributeValuePair>> getFlairUnarmifiedCandidates(String docName, Map<String, List<ValueWithContext>> flairOutput) {
        List<CandidateInPassage<ArmifiedAttributeValuePair>> res = new ArrayList<>();
        for (String att : flairOutput.keySet()) {
            for (ValueWithContext valueWithContext : flairOutput.get(att)) {
                ArmifiedAttributeValuePair avp = new ArmifiedAttributeValuePair(
                        Attributes.get().getFromName(att),
                        valueWithContext.getValue(),
                        docName,
                        Arm.MAIN,
                        valueWithContext.getContext()
                );
                res.add(new CandidateInPassage<>(
                        new SimplePassage(valueWithContext.getContext(), docName, valueWithContext.getScore()),
                        avp,
                        valueWithContext.getScore(),
                        valueWithContext.getScore()
                ));
            }
        }
        return res;
    }

    // TODO: make this private or move it elsewhere, the caller should decide on the sentences, not this object
    public List<String> generateTestingSentence(Document doc) {
        List<String> sentences = new ArrayList<>();
        try {
            for (String str : doc.getValue().split("\n")) {
                if (str.equalsIgnoreCase("acknowledgements") || str.equalsIgnoreCase("references")) {
                    break;
                }
                if (str.matches(".*?http:.*?")) {
                    continue;
                }
                if (str.split(" ").length < 6) {
                    continue;
                }
                sentences.add(escape(str));
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return sentences;
    }

    // TODO: see if this is needed or if there isn't a better way to implement this
    private static String escape(String raw) {
        String escaped = raw;
        escaped = escaped.replace("\\", "\\\\");
        escaped = escaped.replace("\"", "\\\"");
        escaped = escaped.replace("\b", "\\b");
        escaped = escaped.replace("\f", "\\f");
        escaped = escaped.replace("\n", "\\n");
        escaped = escaped.replace("\r", "\\r");
        escaped = escaped.replace("\t", "\\t");
        // TODO: escape other non-printing characters using uXXXX notation
        return escaped;
    }

    private String getAttributeName(String type) {
        return type.replaceAll("_", " ");
    }

    private void checkFlairOutputSentenceCount(int resultSize, List<Integer> sentenceCountsPerDocInQuery) {
        int queriedSentences = sentenceCountsPerDocInQuery.stream().reduce(0, Integer::sum);
        if (resultSize != queriedSentences) {
            log.error("Flair results accounted for {} sentences out of {} queried!", resultSize, queriedSentences);
            log.error("Queried sentence counts: {}", sentenceCountsPerDocInQuery);
            // only enforce this consistency check if there are multiple docs in the query
            if (sentenceCountsPerDocInQuery.size() > 1) {
                log.error("Batch call will now crash with no results return...");
                throw new AssertionError("Flair results returned fewer sentence entities than input sentences.");
            }
        }
    }

    @Data
    public static class ValueWithContext {
        private final String value;
        private final String context;
        private final double score;
    }
}
