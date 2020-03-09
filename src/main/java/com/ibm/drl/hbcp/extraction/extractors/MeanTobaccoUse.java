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
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extract the mean number of times tobacco used.  This usually refers to cigarettes smoked 
 * per day (or week).  The extractor extracts both the number, frequency, and the tobacco (e.g., cigarettes 
 * per day and 16.6).
 * 
 * @author charlesj
 *
 */
public class MeanTobaccoUse extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    public final static String QUERY_STRING = "(smoke OR cigarettes) AND (day OR week OR daily)";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("([Cc]igarettes?)[\\D]+(\\(?\\d+\\.\\d+\\)? +)+")
            );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;
    public static final String ATTRIB_NAME = "Mean number of times tobacco used";
    private final static Pattern CANDIDATE_FLOAT_IN_MATCH = Pattern.compile(" \\d+\\.\\d+ ");

    public MeanTobaccoUse(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public MeanTobaccoUse(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
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
    protected Set<String> getValidMatches(Matcher matcher) {
        return Sets.newHashSet(matcher.group()).stream()  // return full match
                .map(this::postProcess)
                .collect(Collectors.toSet());
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
//                System.out.print("Expected: " + expected.getValue() + " ; " + "Predicted: " + predicted.getValue() + " ; ");
                // prediction should contain tobacco type (cigaratte), interval (day/week), and number
                //  if all parts are in the expected value we return true
                for (String valuePart : predicted.getValue().split("\\s+")) {
                    if (!expected.getValue().toLowerCase().contains(valuePart)) {
//                        System.out.println("Wrong");
                        return false;
                    }
                }
//                System.out.println("Correct");
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

        // multiple candidates can be in one match (e.g., we match a table row with multiple values for cigarettes/day (for different arms))
        List<Pair<String, Double>> candidates = new ArrayList<>();
        for (String match : matches) {
            String tobaccoType = "cigarette";
            String interval = "day";
            if (match.contains("week")) {
                interval = "week";
            }
            Matcher m = CANDIDATE_FLOAT_IN_MATCH.matcher(match);
            while (m.find()) {
                candidates.add(Pair.of(tobaccoType + " " + interval + " " + m.group().trim(), passage.getScore()));  // change score depending on match
            }
        }
        
        return candidates.stream()
                .map(candidateWithScore -> newCandidate(candidateWithScore.getLeft(), candidateWithScore.getRight(), passage))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return ATTRIB_NAME;
    }

   public static void main(String[] args) throws IOException, ParseException {
       try {
           Properties props = Props.loadProperties("init.properties");
           MeanTobaccoUse meanTobaccoUseExtractor = new MeanTobaccoUse(5);
           for (RefComparison evaluation : meanTobaccoUseExtractor.evaluate(props)) {
               System.out.println(evaluation);
           }
       } catch (IOException | ParseException e) {
           e.printStackTrace();
       }
    }

}
