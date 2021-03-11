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
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Detector/extractor for determining the presence of given attributes.<br>
 * This is used, for example, with BCTs, which are
 * of 'detect presence' type, i.e. one needs to predict if a given BCT applies for a
 * research study or not.
 * 
 * @author charlesj
 *
 */
public class GenericPresenceDetector extends LuceneQueryExtractor<ArmifiedAttributeValuePair>
    implements IndexBasedAVPExtractor {

    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeForEvaluation;
    private final String detectQueryString;
    private final double simThreshold;

    public GenericPresenceDetector(IndexingMethod indexingMethod, int numberOfTopPassages, Attribute attrib, String attribDetectQuery) {
        this(indexingMethod, numberOfTopPassages, attrib, attribDetectQuery, 0.2);  // default sim. threshold 0.2
    }

    public GenericPresenceDetector(IndexingMethod indexingMethod, int numberOfTopPassages, Attribute attrib, String attribDetectQuery, double similarityThreshold) {
        super(indexingMethod, numberOfTopPassages, attribDetectQuery);
        this.attribute = attrib;
        this.detectQueryString = attribDetectQuery;
        this.simThreshold = similarityThreshold;
        this.relevantAttributeForEvaluation = Sets.newHashSet(attrib);
    }

    @Override
    public String getQuery() { return this.detectQueryString; }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(PassageInIndex passage) {
        String value = "1";  // value assigned for 'presence'
        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> res = new ArrayList<>(1);
        if (passage.getScore() > this.simThreshold) {
            res.add(new CandidateInPassage<>(
                    passage,
                    new ArmifiedAttributeValuePair(attribute, value, passage.getDocname(), Arm.EMPTY, passage.getText()),
                    passage.getScore(),
                    1.0));
        }
        return res;
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
            @Override
            public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                return true; // meaning that any time we find these 2 values non-null, it's a TP
            }
        };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public String toString() {
        return attribute.getName().trim() + " Detector";
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeForEvaluation;
    }
}
