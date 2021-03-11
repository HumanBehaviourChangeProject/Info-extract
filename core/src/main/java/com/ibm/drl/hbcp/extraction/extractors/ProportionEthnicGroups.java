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
import java.util.Collection;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
 * The extractor for 'Proportion identifying as belonging to a specific ethnic group' should identify
 * ethnic groups in the text and then percentages defining the proportion of the study comprising that
 * ethnic group. 
 * 
 * @author charlesj
 *
 */
public class ProportionEthnicGroups extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProportionEthnicGroups.class);
    
    private static final String[] ETHNICITY_UNIGRAMS = {
            "White",
            "Hispanic",
            "Black",
            "Latino",
            "Asian",
            "Hawaiian",
            "Alaskan",
            "Caucasian",
            "Indian",
            "Pakistani",
            "Bangladeshi",
            "Malay",
            "Chinese",
            "race",
            "ethnicity"
    };
    private static final String[] ETHNICITY_NGRAMS = {
            "African American",
            "European American",
            "American Indian",
            "Pacific Islander",
    };    
    
    private static final String ATTRIB_NAME = "Proportion identifying as belonging to a specific ethnic group";
    public final static String QUERY_STRING = String.join(" OR ", ETHNICITY_UNIGRAMS) + " OR \"" + String.join("\" OR \"", ETHNICITY_NGRAMS) + "\"";

    private final static List<Pattern> REGEXES = Lists.newArrayList(
            // looking for extended version of something like: (White|(African American)|Caucasian).*\d+(\.\d+)?%?
            // this will be greedy but the annotation is already pretty noisy for this
            Pattern.compile("(" + String.join("|", ETHNICITY_UNIGRAMS) + "|(" + String.join(")|(", ETHNICITY_NGRAMS) + ")).*\\d+(\\.\\d+)?%?")
            );
    private final static Pattern NUM_PATTERN = Pattern.compile("\\d\\d?(\\.\\d\\d?)?%?");

    
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;


    /**
     * @param numberOfTopPassages
     * @throws ParseException 
     */
    public ProportionEthnicGroups(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    /**
     * @param indexingMethod
     * @param numberOfTopPassages
     * @throws ParseException 
     */
    public ProportionEthnicGroups(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        return Sets.newHashSet(matcher.group(0));
    }

    @Override
    protected CandidateInPassage<ArmifiedAttributeValuePair> newCandidate(String value, double score,
            Passage passage) {
        return new CandidateInPassage<>(
                passage,
                new ArmifiedAttributeValuePair(attribute, value, passage.getDocname(), Arm.EMPTY, passage.getText()),
                score,
                1.0);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
            @Override
            public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
//                System.out.println("Expected: " + expected.getValue() + "; Predicted: " + predicted.getValue());
                Matcher matcher = NUM_PATTERN.matcher(expected.getValue());
                List<String> expectedNumbers = new ArrayList<>();
                while (matcher.find()) {
                    expectedNumbers.add(matcher.group());
                }
                if (expectedNumbers.isEmpty()) {
                    LOGGER.info("Gold annotation does not contain numbers: " + expected.getValue());
                    return false;
                }
                for (String number : expectedNumbers) {
                    if (!predicted.getValue().contains(number)) {
//                        System.out.println("INCORRECT");
                        return false;
                    }
                }
//                System.out.println("CORRECT");
                return true;
            }
        };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new MultiValueUnarmifiedEvaluator<>(predicateUnarmifiedEvaluator),
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(PassageInIndex passage) {
        String preProcessedWindow = preProcess(passage.getText());
        Set<String> matches = getAllMatchingCandidates(preProcessedWindow);

        return matches.stream()
                .map(match -> newCandidate(match, passage.getScore(), passage))
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
            ProportionEthnicGroups extractor = new ProportionEthnicGroups(5);
            for (RefComparison evaluation : extractor.evaluate(props)) {
                System.out.println(evaluation);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
