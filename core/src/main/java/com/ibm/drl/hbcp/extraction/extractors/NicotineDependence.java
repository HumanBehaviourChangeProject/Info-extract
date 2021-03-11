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
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extract value for nicotine dependence, typically value from Fagerstrom test of nicotine dependence
 * 
 * @author charlesj
 *
 */
public class NicotineDependence extends RegexQueryExtractor<ArmifiedAttributeValuePair>
    implements IndexBasedAVPExtractor {

    private static final String ATTRIB_NAME = "Nicotine dependence";
    public final static String QUERY_STRING = "(Fagerstrom OR Fagerström OR Fagerstr) test (nicotine OR cigarette) dependence OR ftnd OR ftcd";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("(Fagerstr.m|nicotine dependence|cigarette dependence|FTND|FTCD).+(\\(?\\d?\\d\\.\\d+\\)?\\s+)+", Pattern.CASE_INSENSITIVE)
    );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    public NicotineDependence(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public NicotineDependence(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        // match whole group and then try to get numbers (because several varieties of Fagerstrom mentions)
        String fullMatch = matcher.group(0);
        List<String> meanStr = new ArrayList<>();
        // this should return all the floating point numbers that are not in parentheses
        // note: this does not handle row with alternating mean and SD listed without parenthese
        String[] toks = fullMatch.split("\\s+");
        for (String tok : toks) {
            try {
                float ftnd = Float.parseFloat(tok);
//                if (ftnd >= 0.0 && ftnd <= 10.0)  // FTND should be between 0-10
                meanStr.add(tok);
            } catch (NumberFormatException e) {
                // ignore
            }
        }
        return new HashSet<>(meanStr);
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
        NicotineDependence extractor = new NicotineDependence(5);
        for (RefComparison evaluation : extractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}
