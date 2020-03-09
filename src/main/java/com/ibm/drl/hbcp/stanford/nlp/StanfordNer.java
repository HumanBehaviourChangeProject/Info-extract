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

package com.ibm.drl.hbcp.stanford.nlp;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.commons.lang3.tuple.Pair;

import com.ibm.drl.hbcp.core.nlp.HbcpNer;

import edu.stanford.nlp.ling.CoreLabel;
import edu.stanford.nlp.pipeline.CoreDocument;
import edu.stanford.nlp.pipeline.StanfordCoreNLP;

/**
 * Wrapper around Stanford's standard NER interface.
 * 
 * @author charlesj
 *
 */
public class StanfordNer implements HbcpNer {
    
    private static StanfordNer instance;
    private StanfordCoreNLP pipeline;

    private StanfordNer() {
        Properties props = new Properties();
        props.setProperty("annotators", "tokenize,ssplit,pos,lemma,ner");
        // disable fine grained ner
        props.setProperty("ner.applyFineGrained", "false");  // use only location
        props.setProperty("ner.applyNumericClassifiers", "false");  // shouldn't need rest of these
        props.setProperty("ner.buildEntityMentions", "false");
        props.setProperty("ner.useSUTime", "false");
        pipeline = new StanfordCoreNLP(props);
    }
    
    public static StanfordNer getInstance() {
        if (instance == null) {
            instance = new StanfordNer();
        }
        return instance;
    }

    @Override
    public Collection<Pair<String, Double>> findLocations(String document) {
        Collection<Pair<String, Double>> res = new ArrayList<>();
        CoreDocument doc = new CoreDocument(document);
        // annotate the document
        pipeline.annotate(doc);

        String entity = "";
        String prevNerTag = "<prev>";            
        for (CoreLabel token : doc.tokens()) {
            String word = token.word();
            String nerTag = token.ner();
            if (nerTag.equals("LOCATION")) {
                if (prevNerTag.equals("LOCATION")) {
                    entity += token.before() + word;
                } else {
                    entity = word;
                }
            } else {
                if (!entity.equals("")) {
                    // location entity is ended
                    res.add(Pair.of(entity, 1.0));
                    entity = "";
                }
            }
            prevNerTag = nerTag;
        }
        if (!entity.equals("")) {
            // in case the location entity is at the end of the sentence
            res.add(Pair.of(entity, 1.0));
            entity = "";
        }
        return res;
    }
        
}
