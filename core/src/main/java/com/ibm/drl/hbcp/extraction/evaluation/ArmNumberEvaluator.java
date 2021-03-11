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

import java.util.Collection;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;

/**
 * Evaluator for predicting number of arms in a document.<br>
 * Evaluation differs from most because the ground truth number comes from counting up the unique
 * arms and the predicted values comes from predicting/extracting the number of arms from 
 * the document's text.<br>
 * Currently the reference comparison just looks at accuracy.
 * 
 * @author charlesj
 *
 */
public class ArmNumberEvaluator implements Evaluator<IndexedDocument, ArmifiedAttributeValuePair> {

    @Override
    public RefComparison compareWithRef(Collection<ArmifiedAttributeValuePair> predicted,
            Collection<ArmifiedAttributeValuePair> expected) {
        // evaluation compares gold count of arms and predicted number of arms taken from text
        // get the first candidate (there should only be one at most anyway)
        ArmifiedAttributeValuePair candidateAnswer = predicted.stream().findFirst().orElse(null);
        int predNumArms = 2;
        try {
            if (candidateAnswer != null)
                predNumArms = Integer.parseInt(candidateAnswer.getValue());
        } catch (NumberFormatException e) {
        }
        int expNumArms = expected.size();   // count of gold arms
        RefComparison ret;
        // really only need accuracy, so increment correct with tp&tn and incorrect with fp&fn
        if (predNumArms == expNumArms) {
            ret = new RefComparison(1, 0, 0, 1);
        } else {
            ret = new RefComparison(0, 1, 1, 0);
        }
        return ret;
    }

}
