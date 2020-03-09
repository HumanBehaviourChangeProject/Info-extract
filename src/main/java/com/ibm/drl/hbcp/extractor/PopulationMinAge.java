/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswer;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswers;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.CodeSetTree;
import com.ibm.drl.hbcp.util.LuceneField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * This class is used to extract the "mininum age" from a population.
 * @author dganguly
 */
public class PopulationMinAge extends InformationUnit {

    public static final String ATTRIB_ID = "4507433";
    public static final String ATTRIB_NAME = "Minimum Age";
    static final int[] MIN_AGE_VALID_RANGE = {9, 100};
    String currentRetrievedText;
    Set<String> minAgeCandidatesInCurrentRetrievedText;
    List<Pattern> minAgePattern;
    String min;

    public PopulationMinAge(InformationExtractor extractor, String contentFieldName, AttributeType type) {
        super(extractor, contentFieldName, type);
        //query = "(participant OR smoker OR people OR person) AND (age OR year OR old)";
        query = "(participant OR smoker OR people OR person OR patient OR client OR respondent) AND (age OR year OR old)";
        keepUnAnalyzedToken = false;
        currentRetrievedText = "";
        minAgePattern = new ArrayList();
        minAgePattern.add(Pattern.compile("from (\\d+) (to) (\\d+)"));
        minAgePattern.add(Pattern.compile("between (\\d+) (to) (\\d+)"));
        minAgePattern.add(Pattern.compile("between (\\d+) (and) (\\d+)"));
        minAgePattern.add(Pattern.compile("(\\d+)(\\-|–)(\\d+)"));
    }

    @Override
    String preProcess(String window) {
        currentRetrievedText = window;
        minAgeCandidatesInCurrentRetrievedText = new HashSet();
        for (Pattern p : minAgePattern) {
            Matcher m = p.matcher(currentRetrievedText);
            while (m.find()) {
                int ageCandidate = Integer.valueOf(m.group(1));
                if (ageCandidate > MIN_AGE_VALID_RANGE[0] && ageCandidate < MIN_AGE_VALID_RANGE[1]) {
                    minAgeCandidatesInCurrentRetrievedText.add(m.group(1));
                }
            }
        }
        return window;
    }

    /* Note: The purpose of this function is to make sure that the correct answer
    is set correctly (18) when the token is the case of "18 25"
     */
    @Override
    public void construct(String window, Query q, float sim) {
        this.queryTerms = extractQueryTerms(q);

        String pp_window = preProcess(window);

        CandidateAnswers keys = new CandidateAnswers();

        String[] tokens = pp_window.split("\\s+");
        int numTokens = tokens.length;
        int j = 0;

        for (int i = 0; i < numTokens; i++) {
            String token = tokens[i];
            if (!keepUnAnalyzedToken) {
                token = PaperIndexer.analyze(analyzer, token);
            }

            if (token == null) {
                logger.error("Could not analyze token " + token);
            }

            if (isValidCandidate(token)) {
                if (token.matches("\\d+ \\d+")) {
                    token = token.split(" ")[0];
                }
                keys.addCandidateAnswer(new CandidateAnswer(token, i, window));
            }

            int index = Arrays.binarySearch(this.queryTerms, token);
            if (index > -1) {
                queryTermMatches.add(token, i); // i is the position of the query term in 'window'.
            }
        }

        mostLikelyAnswer = queryTermMatches.selectBestAnswer(keys);
        if (mostLikelyAnswer != null) {
            weight = mostLikelyAnswer.avgKernelSim() * sim;
            logger.info(window + "->" + mostLikelyAnswer.getKey() + "- weight: " + weight);
        }
    }

    @Override
    boolean isValidCandidate(String word) {

        //for the token "18-50", currently after PaperIndexer.analyze, it becomes "18 50", so here we keep 50 and check 
        //whether it appeas in the maxAge pattern
        if (word.matches("\\d+ \\d+")) {
            word = word.split(" ")[0];
        }
        boolean valid = minAgeCandidatesInCurrentRetrievedText.contains(word);
        return valid;
    }

    boolean isInteger(String word) {
        int len = word.length();
        for (int i = 0; i < len; i++) {
            if (!Character.isDigit(word.charAt(i))) {
                return false;
            }
        }
        return true;
    }

    @Override
    public void setProperties() {
        min = this.getBestAnswer().getKey();
        //for the token as the form of "18 50" after PaperIndexer.analyze
        if (min.matches("\\d+ \\d+")) {
            min = min.split(" ")[0];
        }
    }

    /**
     * 
     * @param gt
     * @param predicted
     * @param rc
     * @param armification 
     */
    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc, boolean armification) {
        String ageMin = null;
        String ageMin_context = null;
        if (gt.getNode(attribId) == null) {
            ageMin = null;
            ageMin_context = null;
        } else {
            if (armification) {
                ageMin = gt.getNode(ATTRIB_ID).getAnnotatedTextAllArmsContatenated(docName);
                ageMin_context = gt.getNode(ATTRIB_ID).getAnnotatedContextAllArmsContatenated(docName);
            } else {
                ageMin = gt.getNode(ATTRIB_ID).getAnnotatedText(docName);
                ageMin_context = gt.getNode(ATTRIB_ID).getAnnotatedContext(docName);
            }

        }
        logger.info("annotation:" + ageMin);
        if (predicted == null) {
            if (ageMin != null) {
                rc.fn++; // annotated but we predict null
            }
            return;  // no point of going further.. sine there's nothing we predicted
        } else if (ageMin == null) {
            rc.fp++; // we predicted null when there was a value
            return;
        }

        PopulationMinAge predictedObj = (PopulationMinAge) predicted;
        trace(ATTRIB_ID, docName, predictedObj.min, ageMin);

//        boolean correctMin = predictedObj.min.equals(ageMin);
        boolean correctMin = ageMin_context.contains(predictedObj.min);
        logger.info("AgeMin (predicted, reference): " + docName + ": " + predictedObj.min + ", " + ageMin);

        if (correctMin) {
            rc.tp++;
        } else {
            rc.fp++;
            rc.fn++;
        }
    }

    @Override
    public String getName() {
        return ATTRIB_NAME;
    }

    @Override
    public void appendFields(Document doc) {
        doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
                String.valueOf(ATTRIB_ID),
                LuceneField.STORED_NOT_ANALYZED.getType()));
    }

    @Override
    public boolean matches(String attribId) {
        return attribId.equals(ATTRIB_ID);
    }

}
