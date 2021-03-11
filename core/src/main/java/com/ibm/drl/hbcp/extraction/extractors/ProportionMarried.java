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
 * Extract the proportion of study population in a legal marriage or union.
 * 
 * @author charlesj
 *
 */
public class ProportionMarried extends RegexQueryExtractor<ArmifiedAttributeValuePair>
    implements IndexBasedAVPExtractor {

    private static final String ATTRIB_NAME = "Proportion in a legal marriage or union";
    public final static String QUERY_STRING = "(married OR \"marital status\")";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("([Mm]arried)\\D+((\\d+\\s*\\(\\d?\\d\\.\\d%?\\)\\s+)+)"),  // capture percentages in parentheses (usually with actual count, i.e., "Married 11 (7.5) 8 (6.0)")
            // pick one of the following two, if we want only floats or except also integers
            Pattern.compile("([Mm]arried)\\D*\\s+((\\d?\\d\\.\\d%?\\s+)+)"), // capture table rows with percentages: first group has married; second group should match repeated percentages/floats
            Pattern.compile("([Mm]arried)\\D*\\s+((\\d?\\d(\\.\\d)?%?\\s+)+)"), // capture table rows with percentages: first group has married; second group should match repeated numbers
            Pattern.compile("([Mm]arried)\\s+(\\(\\d?\\d\\.\\d%?\\))"),  // capture percentages in parentheses directly following married
            Pattern.compile("()(\\d?\\d(\\.\\d)?%)\\s+(were|are)\\s+([Mm]arried)")  // capture percentages in text before married, i.e., "67.5% were married"; extra parens make number group 2

    );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    public ProportionMarried(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public ProportionMarried(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
//        Set<String> retSet = new HashSet<>();
        // group 2 should contain the percentages and may be repeated
        String fullMatch = matcher.group(2);
        List<String> percentStrs;
        if (fullMatch.contains("(")) {
            percentStrs = new ArrayList<>();
            // assuming percentages are in parentheses, e.g., 11 (7.5) 8 (6.0)
            String[] toks = fullMatch.split("\\s+");
            for (String tok : toks) {
                if (tok.startsWith("(") && tok.endsWith(")")) {
                   percentStrs.add(tok.substring(1, tok.length()-1)); 
                }
            }
        } else {
            percentStrs = Arrays.asList(fullMatch.split("\\s+"));  // group can match multiple values separated by whitespace
            // can be number followed by percentage, in this case keep only floats
            if (percentStrs.size() > 1) {
                try {
                    Integer.parseInt(percentStrs.get(0));
                    try {
                        Integer.parseInt(percentStrs.get(1));
                        // both integers, keep all numbers
                        percentStrs = keepAllNumbers(percentStrs);
                    } catch (NumberFormatException e1) {
                        try {
                            Float.parseFloat(percentStrs.get(1));
                            // second number is float, keep only floats (assuming total number is followed by percent)
                            percentStrs = keepOnlyFloats(percentStrs);
                        } catch (NumberFormatException e2) {
                            // second number not float, keep all numbers? hopefully this doesn't happen much
                            percentStrs = keepAllNumbers(percentStrs);
                        }
                    }
                } catch (NumberFormatException e3) {
                    // take any number or only floats at this point?
                    percentStrs = keepAllNumbers(percentStrs);
                }
            } else {
                percentStrs = keepAllNumbers(percentStrs);
            }
        }
        return new HashSet<>(percentStrs);
    }

    private List<String> keepAllNumbers(List<String> percentStrs) {
        List<String> keptStrs = new ArrayList<>();
        for (String str : percentStrs) {
            try {
                final float parseFloat = Float.parseFloat(str.replace('%', ' '));
                // expect to be a percent
                if (parseFloat >= 0 && parseFloat <= 100)
                    keptStrs.add(str);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return keptStrs;
    }

    private List<String> keepOnlyFloats(List<String> percentStrs) {
        List<String> keptStrs = new ArrayList<>();
        for (String str : percentStrs) {
            try {
                Integer.parseInt(str.replace('%', ' '));
                // if parses as int, skip
            } catch (NumberFormatException e) {
                // check if parses as float
                try {
                    final float parseFloat = Float.parseFloat(str.replace('%', ' '));
                    if (parseFloat >= 0 && parseFloat <= 100)
                        keptStrs.add(str);  // add float that are not integers
                } catch (NumberFormatException e1) {
                    // not a number, ignore
                }
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
                return expected.getContext().contains(predicted.getValue());
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
        ProportionMarried extractor = new ProportionMarried(5);
        for (RefComparison evaluation : extractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}

