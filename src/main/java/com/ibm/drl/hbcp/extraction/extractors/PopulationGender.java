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
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.Attributes;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Gender extractor that determines the population gender attribute (i.e., "all male", "all female", 
 * "mixed genders", "other gender") from a passage.
 * It matches a list of gender keywords to words in the passage and selects a gender value based on
 * those matches (e.g., if male and female keywords are matched, it should return "mixed gender").
 * 
 * @author charlesj
 *
 */
public class PopulationGender extends TokenMatchingQueryExtractor<ArmifiedAttributeValuePair>
    implements IndexBasedAVPExtractor {

    public enum Gender {
        MALE ("All Male"),  // "4507430", //"4266602", //"3587701", // "5579092", // male only
        FEMALE ("All Female"),  //  "4507426", //"4266589", // 3587702", //"5579093", // female only
        MIXED ("Mixed gender", "Mixed genders"),    // "4507427",//"4269096" // "3587703", //"5579095", // mixed gender
        OTHER ("All other gender", "Other gender"); //"5579094"

        private final String[] longNames;
        Gender(String... longNames) {
            this.longNames = longNames;
        }
    }
    
    private static final String[] MALE_KEYWORDS = { "male", "man", "men" };
    private static final String[] FEMALE_KEYWORDS = { "female", "woman", "women", "pregnant" };
    private static final String[] OTHER_KEYWORDS = {};
    private static final String QUERY_STRING = "male OR man OR men OR female OR woman OR women OR pregnant OR gender";
    
    private final List<String> maleKeywordsNormalized;
    private final List<String> femaleKeywordsNormalized;
    private final List<String> otherKeywordsNormalized;
    private final Set<String> genderKeywords;
    private boolean numberPresent;
    private final Analyzer analyzer;
    private final Set<Attribute> relevantAttributesForEvaluation;
    
    protected PopulationGender(IndexingMethod indexingMethod, int numberOfTopPassages)
            throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING);
        relevantAttributesForEvaluation = Arrays.stream(Gender.values())
                .map(gender -> Attributes.get().getFromName(gender.longNames))
                .collect(Collectors.toSet());
        // use analyzer to normalize keywords for later candidate validation
        analyzer = IndexManager.DEFAULT_ANALYZER;
        maleKeywordsNormalized = new ArrayList<>(MALE_KEYWORDS.length);
        for (String w : MALE_KEYWORDS) {
            maleKeywordsNormalized.add(PaperIndexer.analyze(analyzer, w));
        }
        femaleKeywordsNormalized = new ArrayList<>(FEMALE_KEYWORDS.length);
        for (String w : FEMALE_KEYWORDS) {
            femaleKeywordsNormalized.add(PaperIndexer.analyze(analyzer, w));
        }
        otherKeywordsNormalized = new ArrayList<>(OTHER_KEYWORDS.length);
        for (String w : OTHER_KEYWORDS) {
            otherKeywordsNormalized.add(PaperIndexer.analyze(analyzer, w));
        }
        genderKeywords = new HashSet<>(maleKeywordsNormalized);
        genderKeywords.addAll(femaleKeywordsNormalized);
        genderKeywords.addAll(otherKeywordsNormalized);
    }

    @Override
    protected Set<String> getAllMatchingCandidates(String window) {
        // not a candidate but also check if numbers are present in this window, an indication of mixed gender
        numberPresent = numbersPresent(window);
        
        // only keep candidates matching keywords
        // so take intersection of window's words and keywords
        String[] tokens = window.split("\\s+");
        Set<String> res = new HashSet<>(Arrays.asList(tokens));
        res.retainAll(genderKeywords);
        return res;
    }

    private boolean numbersPresent(String text) {
        String[] tokens = text.split("\\s+");
        for (String token: tokens) {
            try {
                Float.parseFloat(token);
                return true;  // most likely some percentage... the complement set exists hence M-F
            }
            catch (NumberFormatException ex) {
                continue;
            }
        }
        return false;
    }

    @Override
    protected CandidateInPassage<ArmifiedAttributeValuePair> newCandidate(String value, double score, Passage passage) {
        // determine candidate gender from value
        // TODO default MIXED gender or null?
        Gender gender = Gender.MIXED;
        for (Gender gen : Gender.values()) {
            if (gen.name().equals(value)) {
                gender = gen;
                break;
            }
        }
        Attribute attribute = Attributes.get().getFromName(gender.longNames);
        return new CandidateInPassage<ArmifiedAttributeValuePair>(
                passage,
                new ArmifiedAttributeValuePair(attribute, value, passage.getDocname(), Arm.EMPTY, passage.getText()),
                score,
                1.0);
    }

    @Override
    protected List<Pair<String, Double>> getScoredCandidates(List<Pair<String, Integer>> candidatesWithPosition,
            List<Pair<String, Integer>> queryTermsWithPosition) {
        // scores candidates like parent TokenMatchingQueryExtractor but standardize values to match enum
        List<Pair<String, Double>> scoredCandidates = candidatesWithPosition.stream()
                .map(candidateWithPosition -> {
                    double res = 0.0;
                    for (Pair<String, Integer> queryTermWithPosition : queryTermsWithPosition) {
                        res += similarity(candidateWithPosition, queryTermWithPosition);
                    }
                    return Pair.of(standardizeCandidateValue(candidateWithPosition.getLeft()), res);
                })
                .collect(Collectors.toList());
        // do initial aggregation over this passage if male or female are matched
        // keep old candidates and add mixed gender
        // TODO revisit scoring
        // mixed is most likely and if there is mention of male and female we add a mixed candidate with new best score
        //   10% higher than previous max
        boolean seenMale = false;
        boolean seenFemale = false;
        double maxScore = 0.0;
        for (Pair<String, Double> candidate : scoredCandidates) {
            if (candidate.getKey().equals(Gender.MALE.toString())) {
                seenMale = true;
            } else if (candidate.getKey().equals(Gender.FEMALE.toString())) {
                seenFemale = true;
            }
            if (candidate.getValue().doubleValue() > maxScore) {
                maxScore = candidate.getValue().doubleValue();
            }
        }
        if (numberPresent || (seenMale && seenFemale)) {
            scoredCandidates.add(Pair.of(Gender.MIXED.toString(), maxScore + maxScore * 0.1));
        }
        return scoredCandidates;
    }
    
    private String standardizeCandidateValue(String genderValue) {
        if (maleKeywordsNormalized.contains(genderValue)) {
            return Gender.MALE.toString();
        } else if (femaleKeywordsNormalized.contains(genderValue)) {
            return Gender.FEMALE.toString();
        } else if (otherKeywordsNormalized.contains(genderValue)) {
            return Gender.OTHER.toString();
        }
        return null;
    }

    @Override
    protected String preProcess(String window) {
        return PaperIndexer.analyze(analyzer, window);
    }
    
    @Override
    protected boolean isValidCandidate(String word) {
        return genderKeywords.contains(word);
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
            @Override
            public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                return predicted.getAttribute().getId().equals(expected.getAttribute().getId());  // correct if attribute Ids match
            }
        };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributesForEvaluation;
    }

    @Override
    public String toString() {
        return "Gender";
    }
}
