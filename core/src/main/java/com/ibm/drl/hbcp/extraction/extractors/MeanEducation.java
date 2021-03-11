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

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;
import edu.emory.mathcs.backport.java.util.Arrays;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract the mean number of years in education completed.
 * 
 * @author charlesj
 *
 */
public class MeanEducation extends RegexQueryExtractor<ArmifiedAttributeValuePair>
    implements IndexBasedAVPExtractor {

    private static final String ATTRIB_NAME = "Mean number of years in education completed";
    public final static String QUERY_STRING = "+(education OR schooling) formal years";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("([Ee]ducation|schooling)\\D+(\\(?\\d?\\d\\.\\d+\\)?\\s+)+")
    );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    public MeanEducation(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public MeanEducation(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        // group 2 should contain the percentages and may be repeated
        String fullMatch = matcher.group(2);
        List<String> meanStr;
        if (fullMatch.contains("(")) {
            meanStr = new ArrayList<>();
            // assuming mean is first and SD is in parentheses, e.g., education (average, SD) 11.3 (2.7) 11.5 (2.1)
            String[] toks = fullMatch.split("\\s+");
            for (String tok : toks) {
                if (Character.isDigit(tok.charAt(0))) {  // could also check that not parenthesis
                   meanStr.add(tok);
                }
            }
        } else {
            meanStr = Arrays.asList(fullMatch.split("\\s+"));  // group can match multiple values separated by whitespace
            // check that are numbers and remove std dev
            meanStr = cleanMeans(meanStr);
        }
        return new HashSet<>(meanStr);
    }

    private List<String> cleanMeans(List<String> meanStrs) {
        List<String> keptStrs = new ArrayList<>();
        Float firstMean = null;  // assume that first number is a mean (and not std dev.)
        for (String str : meanStrs) {
            try {
                final float parseFloat = Float.parseFloat(str);
                if (firstMean == null) {
                    firstMean = parseFloat;
                }
                if (parseFloat > firstMean/2) { // kind of a hack, but consider means smaller than half
                                                 // of first mean to be standard deviations
                    keptStrs.add(str);
                }
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return keptStrs;
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
                return expected.getValue().contains(predicted.getValue()) || predicted.getValue().contains(expected.getValue());  // TODO treat as a number
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
    
    @Override
    public String toString() {
        return ATTRIB_NAME;
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        MeanEducation extractor = new MeanEducation(5);
        for (RefComparison evaluation : extractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}
