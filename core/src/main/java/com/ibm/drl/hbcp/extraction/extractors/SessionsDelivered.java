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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The extractor for 'Sessions delivered' should identify percentage of completed interventions, for example,
 * '% of phone call sessions were successfully delivered'.
 * 
 * @author charlesj
 *
 */
public class SessionsDelivered extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final Logger LOGGER = LoggerFactory.getLogger(SessionsDelivered.class);

    private static final String ATTRIB_NAME = "Sessions delivered";
    public final static String QUERY_STRING = "attend session complete call";

    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("(\\d?\\d\\.?\\d*%)"),  // look for any number with percent sign
            Pattern.compile("(\\d?\\d\\.?\\d)*.{1,20}(attend|complete|call)")  // ugly regex, but this is a pretty noisy entity
            );

    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    /**
     * @param numberOfTopPassages
     * @throws ParseException
     */
    public SessionsDelivered(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    /**
     * @param indexingMethod
     * @param numberOfTopPassages
     * @throws ParseException
     */
    public SessionsDelivered(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
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
    public String toString() {
        return ATTRIB_NAME;
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        SessionsDelivered extractor = new SessionsDelivered(5);
        for (RefComparison evaluation : extractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }

}
