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

package com.ibm.drl.hbcp.core.nlp;

import java.util.Collection;

import org.apache.commons.lang3.tuple.Pair;

/**
 * This interface should capture the named entity recognition (NER) functionality we need for HBCP.
 * 
 * @author charlesj
 *
 */
public interface HbcpNer {

    /**
     * Return a collection of 'location' named entities as strings (e.g., "New York") and scores.
     * The scores may not be returned by all implementing NER systems and so all entities may have the
     * same score (e.g., 1.0).  
     * 
     * @param document
     * @return
     */
    public Collection<Pair<String, Double>> findLocations(String document);
    
}
