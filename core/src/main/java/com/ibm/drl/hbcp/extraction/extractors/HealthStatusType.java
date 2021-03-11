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
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

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
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extract the aggregate health status type.  This typically includes conditions of the people in the study
 * e.g., substance use disorders, major depressive disorder, erectile dysfunction, pregnant
 *  
 * @author charlesj
 *
 */
public class HealthStatusType extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final String ATTRIB_NAME = "Aggregate health status type";
    private static final String[] CONDITIONS = {"substance use disorder",
            "SUD",
            "binge drinking",
            "nonacute psychotic disorder",
            "depressive disorder",
            "MDD",
            "CES-D",
            "diabetic\n",
            "myocardial infarction",
            "erectile dysfunction",
            "chronic obstructive pulmonary disease",
            "pregnant",
            "HIV+",
            "BMI",
            "BDI",
            "alcohol dependent",
            "cancer",
            "coronary heart disease"}; 
    private static final String QUERY_STRING = "(\"" + String.join("\" OR \"", CONDITIONS) + "\")";
    private static final List<Pattern> REGEXES = (List<Pattern>) Arrays.asList(CONDITIONS).stream().map(x -> Pattern.compile("(" + x + ")")).collect(Collectors.toList());
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    public HealthStatusType(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public HealthStatusType(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        String match = matcher.group(1);
        if (match != null) {
            return Sets.newHashSet(match);
        } else 
            return new HashSet<>();
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
            HealthStatusType healthStatusExtractor = new HealthStatusType(5);
            for (RefComparison evaluation : healthStatusExtractor.evaluate(props)) {
                System.out.println(evaluation);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
