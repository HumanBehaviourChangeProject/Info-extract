/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extract types of biochemical verification assement terms: e.g., salivary cotinine, carbon monoxide, saliva sample, 
 * saliva cotinine, urine cotinine, plasma cotinine, CO, etc.
 * 
 * @author charlesj
 *
 */
public class BiochemicalVerification extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final String ATTRIB_NAME = "Biochemical verification";
    private static final String[] CONDITIONS = {"salivary cotinine",
            "carbon monoxide",
            "blood for carboxyhaemoglobin and thiocyanate estimations",
            "saliva sample",
            "saliva cotinine",
            "urine cotinine",
            "expired air sample for CO",
            "cotinine testing",
            "urinary continence",
            "plasma cotinine",
            "anabasine",
            "CO level"
            }; 
    private static final String QUERY_STRING = "(\"" + String.join("\" OR \"", CONDITIONS) + "\")";
    private static final List<Pattern> REGEXES = (List<Pattern>) Arrays.asList(CONDITIONS).stream().map(x -> Pattern.compile("(" + x + ")")).collect(Collectors.toList());
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    public BiochemicalVerification(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public BiochemicalVerification(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    protected CandidateInPassage<ArmifiedAttributeValuePair> newCandidate(String value, double score, Passage passage) {
        return new CandidateInPassage<>(
                passage,
                new ArmifiedAttributeValuePair(attribute, value, passage.getDocname(), Arm.EMPTY, passage.getText()),
                score,
                1.0);
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
            @Override
            public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
//                System.out.println("Expected: " + expected.getValue() + " ; " + "Predicted: " + predicted.getValue());
                return expected.getValue().toLowerCase().contains(predicted.getValue().toLowerCase());
            }
        };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new MultiValueUnarmifiedEvaluator<>(predicateUnarmifiedEvaluator),
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }
    
    /**
     * Extracts all the valid final string values from a single token, given all
     * the pattern matches in the window
     */
    protected List<String> getValidCandidates(String token, Set<String> patternMatches) {
        String postProcessedToken = postProcess(token);
        if (patternMatches.contains(postProcessedToken)) {
            return Lists.newArrayList(postProcessedToken);
        } else {
            return new ArrayList<>();
        }
    }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(PassageInIndex passage) {
        String preProcessedWindow = preProcess(passage.getText());
        Set<String> matches = getAllMatchingCandidates(preProcessedWindow);

        List<Pair<String, Double>> candidatesWithPosition = matches.stream().map(c -> Pair.of(c, passage.getScore())).collect(Collectors.toList());

        return candidatesWithPosition.stream()
                .map(candidateWithScore -> newCandidate(candidateWithScore.getLeft(), candidateWithScore.getRight(), passage))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return ATTRIB_NAME;
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            Properties props = Props.loadProperties("init.properties");
            BiochemicalVerification bioVerificationExtractor = new BiochemicalVerification(5);
            for (RefComparison evaluation : bioVerificationExtractor.evaluate(props)) {
                System.out.println(evaluation);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
