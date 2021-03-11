/*
 * Copyright 2020 francesca.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
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
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;
import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author francesca
 */
    

public class ProportionBelongingToIncomeCategory extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(ProportionByIncome.class);

    private static final String ATTRIB_NAME = "Proportion belonging to specified individual income category";
    public final static String QUERY_STRING = "income";

    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("(\\d+% income (.*\\d\\d.(\\d+)?))")
//            Pattern.compile("[Ii]ncome(.*\\d?\\d(\\.\\d+)?)")
            );

    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    /**
     * @param numberOfTopPassages
     * @throws ParseException
     */
    public ProportionBelongingToIncomeCategory(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    /**
     * @param indexingMethod
     * @param numberOfTopPassages
     * @throws ParseException
     */
    public ProportionBelongingToIncomeCategory(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        return Sets.newHashSet(matcher.group(1));
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
                        final String normExpected = expected.getValue().replaceAll("\\s+", " ");
                        final String normPredicted = predicted.getValue().replaceAll("\\s+", " ");
//                        System.out.println("Expected: " + normExpected + "; Predicted: " + normPredicted);
                        return normExpected.contains(normPredicted) || normPredicted.contains(normExpected);
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
            ProportionByIncome extractor = new ProportionByIncome(5);
            for (RefComparison evaluation : extractor.evaluate(props)) {
                System.out.println(evaluation);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
