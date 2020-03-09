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
import com.ibm.drl.hbcp.core.nlp.HbcpNer;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.stanford.nlp.StanfordNer;
import com.ibm.drl.hbcp.util.Props;

import edu.emory.mathcs.backport.java.util.Arrays;

/**
 * The Geographical location extractor handles 'Country of intervention' and 'Lower-level geographical region'.
 * NER identifies 'location' strings and if they are found in the list of countries we consider them the former attribute (country)
 * and if they are not in the list they are the latter attribute (region).
 * 
 * @author charlesj
 *
 */
public class GeographicalLocation extends LuceneQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final String COUNTRY_ATTRIB = "Country of intervention";
    private static final String REGION_ATTRIB = "Lower-level geographical region";
    
    private static final String[] GENERIC = {"location", "state", "country", "county", "region", "site", "area", "community", "site"};
    private static final String[] DIRECTIONS = {"north", "south", "east", "west", "northeast", "southeast", "southwest", "northwest"};
    private static final String[] COUNTRIES = {"Afghanistan","Albania","Algeria","Andorra","Angola","Anguilla","Antigua & Barbuda","Argentina","Armenia","Australia","Austria","Azerbaijan",
            "Bahamas","Bahrain","Bangladesh","Barbados","Belarus","Belgium","Belize","Benin","Bermuda","Bhutan","Bolivia","Bosnia & Herzegovina","Botswana","Brazil","Brunei Darussalam","Bulgaria","Burkina Faso","Burundi",
            "Cambodia","Cameroon","Canada","Cape Verde","Cayman Islands","Central African Republic","Chad","Chile","China","China - Hong Kong / Macau","Colombia","Comoros","Congo","Congo, Democratic Republic of (DRC)","Costa Rica","Croatia","Cuba","Cyprus","Czech Republic",
            "Denmark","Djibouti","Dominica","Dominican Republic","Ecuador","Egypt","El Salvador","Equatorial Guinea","Eritrea","Estonia","Eswatini","Ethiopia",
            "Fiji","Finland","France","French Guiana","Gabon","Gambia, Republic of The","Georgia","Germany","Ghana","Great Britain","Greece","Grenada","Guadeloupe","Guatemala","Guinea","Guinea-Bissau","Guyana",
            "Haiti","Honduras","Hungary","Iceland","India","Indonesia","Iran","Iraq","Israel and the Occupied Territories","Italy","Ivory Coast (Cote d'Ivoire)","Jamaica","Japan","Jordan",
            "Kazakhstan","Kenya","Korea, Democratic Republic of (North Korea)","Korea, Republic of (South Korea)","Kosovo","Kuwait","Kyrgyz Republic (Kyrgyzstan)",
            "Laos","Latvia","Lebanon","Lesotho","Liberia","Libya","Liechtenstein","Lithuania","Luxembourg",
            "Madagascar","Malawi","Malaysia","Maldives","Mali","Malta","Martinique","Mauritania","Mauritius","Mayotte","Mexico","Moldova, Republic of","Monaco","Mongolia","Montenegro","Montserrat","Morocco","Mozambique","Myanmar/Burma",
            "Namibia","Nepal","New Zealand","Nicaragua","Niger","Nigeria","North Macedonia, Republic of","Norway","Oman",
            "Pacific Islands","Pakistan","Panama","Papua New Guinea","Paraguay","Peru","Philippines","Poland","Portugal","Puerto Rico","Qatar",
            "Reunion","Romania","Russian Federation","Rwanda","Saint Kitts and Nevis","Saint Lucia","Saint Vincent and the Grenadines","Samoa","Sao Tome and Principe","Saudi Arabia","Senegal","Serbia","Seychelles","Sierra Leone","Singapore","Slovak Republic (Slovakia)","Slovenia","Solomon Islands","Somalia","South Africa","South Sudan","Spain","Sri Lanka","Sudan","Suriname","Sweden","Switzerland","Syria",
            "Tajikistan","Tanzania","Thailand","Netherlands","Timor Leste","Togo","Trinidad & Tobago","Tunisia","Turkey","Turkmenistan","Turks & Caicos Islands",
            "Uganda","Ukraine","United Arab Emirates","United States of America (USA)","Uruguay","Uzbekistan","Venezuela","Vietnam","Virgin Islands (UK)","Virgin Islands (US)","Yemen",
            "Zambia","Zimbabwe",
            "United States", "UK", "USA", "US"
    };
    private static final List<String> COUNTRIES_LIST = Arrays.asList(COUNTRIES);
    private static final String[] US_STATES = {
            "Alabama", "Alaska", "Arizona", "Arkansas", "California", "Colorado", "Connecticut", "Delaware", "Florida", "Georgia", "Hawaii", 
            "Idaho", "Illinois", "Indiana", "Iowa", "Kansas", "Kentucky", "Louisiana", "Maine", "Maryland", "Massachusetts", "Michigan", "Minnesota", "Mississippi", "Missouri", "Montana", 
            "Nebraska", "Nevada", "New Hampshire", "New Jersey", "New Mexico", "New York", "North Carolina", "North Dakota", "Ohio", "Oklahoma", "Oregon", 
            "Pennsylvania", "Rhode Island", "South Carolina", "South Dakota", "Tennessee", "Texas", "Utah", "Vermont", "Virginia", 
            "Washington", "West Virginia", "Wisconsin", "Wyoming"
    };
    
    private static final String QUERY_STRING = 
            String.join(" OR ", GENERIC) + " OR " +
            String.join(" OR ", DIRECTIONS) + " OR " +
            String.join(" OR ", COUNTRIES) + " OR " +
            String.join(" OR ", US_STATES);

    private final Attribute countryAttribute;
    private final Attribute regionAttribute;
    private final Set<Attribute> relevantAttributeSet;
    private final HbcpNer ner;

    public GeographicalLocation(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public GeographicalLocation(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING);
        this.countryAttribute = Attributes.get().getFromName(COUNTRY_ATTRIB);
        this.regionAttribute = Attributes.get().getFromName(REGION_ATTRIB);
        this.relevantAttributeSet = Sets.newHashSet(countryAttribute, regionAttribute);
        ner = StanfordNer.getInstance();
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
                return expected.getValue().toLowerCase().contains(predicted.getValue().toLowerCase());
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
        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> res = new ArrayList<>();
        Collection<Pair<String, Double>> locEntities = ner.findLocations(passage.getText());
        for (Pair<String, Double> locEntity : locEntities) {
            ArmifiedAttributeValuePair cAvp;
            if (COUNTRIES_LIST.contains(locEntity.getLeft())) {
                cAvp = new ArmifiedAttributeValuePair(countryAttribute, locEntity.getLeft(), passage.getDocname(), Arm.EMPTY, passage.getText());
            } else {
                cAvp = new ArmifiedAttributeValuePair(regionAttribute, locEntity.getLeft(), passage.getDocname(), Arm.EMPTY, passage.getText());
            }
            res.add(new CandidateInPassage<>(passage, cAvp, passage.getScore(), 1.0));
        }
        return res;
    }

    @Override
    public String toString() {
        return "Geographical location extractor(s) (country and region)";
    }

    /**
     * @param args
     */
    public static void main(String[] args) {
        try {
            Properties props = Props.loadProperties("init.properties");
            GeographicalLocation geoExtractor = new GeographicalLocation(5);
            for (RefComparison evaluation : geoExtractor.evaluate(props)) {
                System.out.println(evaluation);
            }
        } catch (IOException | ParseException e) {
            e.printStackTrace();
        }
    }

}
