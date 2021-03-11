package com.ibm.drl.hbcp.predictor.similarity;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.api.ExtractorController;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.api.ArmSimilarityResult;
import com.ibm.drl.hbcp.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

public class QueryArmSimilarityTester {

    private final JSONRefParser jsonRefParser;
    private final QueryArmSimilarity queryArmSimilarity;

    private static final Logger log = LoggerFactory.getLogger(ExtractorController.class);

    public QueryArmSimilarityTester() throws IOException {
        Properties props = Props.loadProperties();
        jsonRefParser = new JSONRefParser(props);
        queryArmSimilarity = new QueryArmSimilarity(jsonRefParser.getAttributeValuePairs(), props);
    }

    public List<ArmSimilarityResult> query(double meanAge, List<String> bctNames, int topK) throws IOException {
        // population constraint
        AttributeValuePair meanAgeAvp = new AttributeValuePair(Attributes.get().getFromName("Mean age"), String.valueOf(meanAge));
        // BCTs
        List<AttributeValuePair> bctAvps = bctNames.stream()
                .map(name -> Attributes.get().getFromName(name))
                .map(att -> new AttributeValuePair(att, "1"))
                .collect(Collectors.toList());
        // build the query
        List<AttributeValuePair> query = new ArrayList<>(bctAvps);
        query.add(meanAgeAvp);
        // get the similarities
        return queryArmSimilarity.querySimilarityByArm(query, jsonRefParser).stream()
                .limit(topK)
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException {
        QueryArmSimilarityTester tester = new QueryArmSimilarityTester();
        // query
        double meanAge = 30;
        List<String> bctNames = Lists.newArrayList(
            "1.2 Problem solving"
        );
        // display top 10
        for (ArmSimilarityResult result : tester.query(meanAge, bctNames, 5)) {
            log.info("==========================");
            log.info("Score: {}", result.getScore());
            log.info("Mean Age: {}", result.getAttributeSimilarities().get(Attributes.get().getFromName("Mean age")));
            log.info("Outcome value: {}", result.getOutcomeValue());
            log.info("Common interventions: ");
            for (Attribute bct : result.getCommonInterventions()) {
                log.info("\t" + bct.getName());
            }
        }
    }
}
