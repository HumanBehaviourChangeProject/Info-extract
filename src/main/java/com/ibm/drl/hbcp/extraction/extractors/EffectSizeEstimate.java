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
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
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
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extract the effect size estimate. This is the (numeric) value associated with something like 'odds ratio' 
 * or 'risk ratio' (captured with another attribute extractor i.e., odds ratio attribute uses {@link GenericPresenceDetector}).
 * This attribute often should be accompanied by a p-value ({@link EffectSizePValue}).
 * 
 * @author charlesj
 *
 */
public class EffectSizeEstimate extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    public final static String QUERY_STRING = "\"odds ratio\" OR \"follow up\" OR month OR week OR p OR rr OR group OR ci";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("[OR]R\\s?[\\S\\W]?\\s?(\\d\\.\\d+)"),
            Pattern.compile(".(\\d\\.\\d+).")  // use pretty relaxed pattern and filter matches later (e.g., valid numbers followed by %)
            );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;
    public static final String ATTRIB_NAME = "Effect size estimate";

    public EffectSizeEstimate(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public EffectSizeEstimate(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        // patterns check general form of effects size, e.g., odds ratio something like 1.83
        // here we filter out any of that form that are in parentheses or are percentages
        String fullMatch = matcher.group(0);
        if (fullMatch.contains("OR") || fullMatch.contains("RR") ||
                (fullMatch.startsWith(" ") && fullMatch.endsWith(" "))) { 
            return Sets.newHashSet(matcher.group(1));
        } else {
            return new HashSet<>();
        }
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
                // TODO check equality of float values?
//                System.out.println("Expected: " + expected.getValue() + " ; " + "Predicted: " + predicted.getValue() +
//                        (expected.getValue().toLowerCase().contains(predicted.getValue().toLowerCase()) ? " ; Correct" : " ; Wrong"));
//                System.out.println("Context: " + expected.getContext()+ " ; " + "\nPredicted: " + predicted.getContext());
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
    public String toString() {
        return ATTRIB_NAME;
    }

   public static void main(String[] args) throws IOException, ParseException {
       try {
           Properties props = Props.loadProperties("init.properties");
           EffectSizeEstimate effectSizeExtractor = new EffectSizeEstimate(5);
           for (RefComparison evaluation : effectSizeExtractor.evaluate(props)) {
               System.out.println(evaluation);
           }
       } catch (IOException | ParseException e) {
           e.printStackTrace();
       }
    }

}
