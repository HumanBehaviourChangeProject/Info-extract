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

package com.ibm.drl.hbcp.extraction.evaluation;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import org.apache.commons.lang3.tuple.Pair;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.extraction.candidates.Candidate;
import com.ibm.drl.hbcp.extraction.extractors.ArmAssociator;
import com.ibm.drl.hbcp.extraction.extractors.ArmsExtractor;
import com.ibm.drl.hbcp.extraction.extractors.Associator;
import com.ibm.drl.hbcp.extraction.extractors.EvaluatedExtractor;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extractor.RefComparison;

/**
 * Armified evaluator interface does the attribute extraction (like {@link Evaluator}) and
 * then extracts arms and does arm association for evaluation of armified AVPs.
 * 
 * @author charlesj
 *
 */
public interface ArmifiedEvaluator extends Evaluator<IndexedDocument, ArmifiedAttributeValuePair> {

    /** Evaluate the given extractor on documents (associated with their respective annotations) 
     * @throws IOException */
    default RefComparison evaluate(EvaluatedExtractor<IndexedDocument, ArmifiedAttributeValuePair, ?> extractor,
            List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth,
            ArmAssociator armAssociator, ArmsExtractor armExtractor) throws IOException {
        List<RefComparison> res = new ArrayList<>();
        for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> docAndAnnotations : groundTruth) {
            Collection<Arm> arms = armExtractor.extract(docAndAnnotations.getLeft()).stream().map(x -> x.getAnswer()).collect(Collectors.toSet());
            Collection<ArmifiedAttributeValuePair> relevantAnnotations = extractor.getRelevant(docAndAnnotations.getRight());
            RefComparison singleDocumentEval = evaluate(extractor, docAndAnnotations.getLeft(), relevantAnnotations, armAssociator, arms);
            res.add(singleDocumentEval);
        }
        return aggregate(res);
    }

    default RefComparison evaluate(EvaluatedExtractor<IndexedDocument, ArmifiedAttributeValuePair, ?> extractor, IndexedDocument document, Collection<ArmifiedAttributeValuePair> expected, Associator armAssoc, Collection<Arm> arms) throws IOException {
        Collection<? extends Candidate<ArmifiedAttributeValuePair>> predictedCandidates = extractor.extract(document);
//        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> contextualizedCandidates = predictedCandidates.stream()
//                .map(x -> new CandidateInPassage(null, new ArmifiedAttributeValuePair(x.getAnswer().getAttribute(), x.getAnswer().getValue(), "", Arm.EMPTY, ""), x.getScore(), 0.0))
//                .collect(Collectors.toList());
        Collection<Candidate<ArmifiedAttributeValuePair>> armifiedCandidates = armAssoc.associate(predictedCandidates, arms);
        Collection<ArmifiedAttributeValuePair> predicted = armifiedCandidates.stream().map(Candidate::getAnswer).collect(Collectors.toList());
        return compareWithRef(predicted, expected);
    }
    
}
