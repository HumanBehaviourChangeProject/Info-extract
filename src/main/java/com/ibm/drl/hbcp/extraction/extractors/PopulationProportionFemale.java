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

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * Extract the proportion of study population that achieved university or college education. 
 * 
 * @author fbonin
 *
 */
public class PopulationProportionFemale extends RegexQueryExtractor<ArmifiedAttributeValuePair>
    implements IndexBasedAVPExtractor {

    private static final String ATTRIB_NAME = "Proportion identifying as female gender";
    public final static String QUERY_STRING = "(gender OR female OR percentage)";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("([Ff]emale)\\D*\\s+((\\d?\\d\\.\\d%?\\s+)+)"), // capture table rows with percentages: first group has Female; second group should match repeated percentages/floats
            Pattern.compile("([Ff]emale|[Ww]omen)\\D+((\\d+\\s*\\(\\d?\\d\\.\\d%?\\)\\s+)+)"),  // capture percentages in parentheses (usually with actual count, i.e., "Female 11 (7.5) 8 (6.0)")
            Pattern.compile("\\D+((\\d+\\s*\\(\\d?\\d\\.\\d%?\\)\\s+)+)([Ff]emale|[Ww]omen)")
            
    );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    public PopulationProportionFemale(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }
 
    public PopulationProportionFemale(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
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
        }
        // TODO should be a percent.  Check range 0-100?
        return new HashSet<>(percentStrs);
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
        return "Female percentage";
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        PopulationProportionFemale extractor = new PopulationProportionFemale(5);
        for (RefComparison evaluation : extractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}

