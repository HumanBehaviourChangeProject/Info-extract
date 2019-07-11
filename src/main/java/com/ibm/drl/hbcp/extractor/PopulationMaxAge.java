/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswer;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswers;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import java.util.Arrays;
import org.apache.lucene.search.Query;
import com.ibm.drl.hbcp.parser.CodeSetTree;

/**
 * This class is used to extract the maximum age of a population.
 * The constructor sets the query object.
 * 
 * @author dganguly
 */
public class PopulationMaxAge extends InformationUnit {

    public static final String ATTRIB_ID = "4507434";
    public static final String ATTRIB_NAME = "Maximum Age";
    static final int[] MAX_AGE_VALID_RANGE = {9, 100};
    String currentRetrievedText;
    Set<String> maxAgeCandidatesInCurrentRetrievedText;
    List<Pattern> maxAgePattern;
    String max;

    public PopulationMaxAge(InformationExtractor extractor, String contentFieldName, AttributeType type) {
        super(extractor, contentFieldName, type);
        //query = "(participant OR smoker OR people OR person) AND (age OR year OR old)";
        query = "(participant OR smoker OR people OR person OR patient OR client OR respondent) AND (age OR year OR old)";
        keepUnAnalyzedToken = false;
        currentRetrievedText = "";
        maxAgePattern = new ArrayList();
        maxAgePattern.add(Pattern.compile("from (\\d+) (to) (\\d+)"));
        maxAgePattern.add(Pattern.compile("between (\\d+) (to) (\\d+)"));
        maxAgePattern.add(Pattern.compile("between (\\d+) (and) (\\d+)"));
        maxAgePattern.add(Pattern.compile("(\\d+)(\\-|–)(\\d+)"));
    }

    @Override
    String preProcess(String window) {
        currentRetrievedText = window;
        maxAgeCandidatesInCurrentRetrievedText = new HashSet();
        for (Pattern p : maxAgePattern) {
            Matcher m = p.matcher(currentRetrievedText);
            while (m.find()) {
                int ageCandidate = Integer.valueOf(m.group(3));
                if (ageCandidate > MAX_AGE_VALID_RANGE[0] && ageCandidate < MAX_AGE_VALID_RANGE[1]) {
                    maxAgeCandidatesInCurrentRetrievedText.add(m.group(3));
                }
            }
        }
        return window;
    }

    /* Note: The purpose of this function is to make sure that the correct answer
    is set correctly (25) when the token is the case of "18 25"
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
                    token = token.split(" ")[1];
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
            word = word.split(" ")[1];
        }
        boolean valid = maxAgeCandidatesInCurrentRetrievedText.contains(word);
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
        max = this.getBestAnswer().getKey();
        //for the token as the form of "18 50" after PaperIndexer.analyze
        if (max.matches("\\d+ \\d+")) {
            max = max.split(" ")[1];
        }
    }

    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc, boolean armification) {
        String ageMax = null;
        String ageMax_context = null;
        if (gt.getNode(ATTRIB_ID) == null) {
            ageMax = null;
            ageMax_context = null;
        } else {
            if (armification) {
                ageMax = gt.getNode(ATTRIB_ID).getAnnotatedTextAllArmsContatenated(docName);
                ageMax_context = gt.getNode(ATTRIB_ID).getAnnotatedContextAllArmsContatenated(docName);
            } else {
                ageMax = gt.getNode(ATTRIB_ID).getAnnotatedText(docName);
                ageMax_context = gt.getNode(ATTRIB_ID).getAnnotatedContext(docName);
            }

        }

        logger.info("annotation:" + ageMax);
        if (predicted == null) {
            if (ageMax != null) {
                rc.fn++; // annotated but we predict null
            }
            return;  // no point of going further.. sine there's nothing we predicted
        } else if (ageMax == null) {
            rc.fp++; // we predicted null when there was a value
            return;
        }

        PopulationMaxAge predictedObj = (PopulationMaxAge) predicted;
        trace(ATTRIB_ID, docName, predictedObj.max, ageMax);

//        boolean correctMax = predictedObj.max.equals(ageMax);
        boolean correctMax = ageMax_context.contains(predictedObj.max);
        logger.info("AgeMax (predicted, reference): " + docName + ": " + predictedObj.max + ", " + ageMax);

        if (correctMax) {
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
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    @Override
    public boolean matches(String attribId) {
        return attribId.equals(ATTRIB_ID);
    }

}
