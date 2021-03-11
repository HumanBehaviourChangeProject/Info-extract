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

import org.jetbrains.annotations.NotNull;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;

/**
 * An armified evaluator that uses the correctness logic from the unarmfied
 * evaluator ({@link PredicateUnarmifiedEvaluator}) and checks that the arms also match.
 *
 * @author charlesj
 * */
public class PredicateArmifiedEvaluator<Doc> implements PredicateUnarmifiedEvaluator<Doc, ArmifiedAttributeValuePair> {

    private PredicateUnarmifiedEvaluator<Doc, ArmifiedAttributeValuePair> unArmifiedEval;

    public PredicateArmifiedEvaluator(PredicateUnarmifiedEvaluator<Doc, ArmifiedAttributeValuePair> unArmifiedEval) {
        this.unArmifiedEval = unArmifiedEval;
    }

    @Override
    public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
        if (unArmifiedEval.isCorrect(predicted, expected)) {
            // return true is any predicted name matches any expected name
            for (String predName : predicted.getArm().getAllNames()) {
                if (expected.getArm().getAllNames().contains(predName)) {
                    return true;
                }
            }
            return false;
        } else {
            return false;
        }
    }

}
