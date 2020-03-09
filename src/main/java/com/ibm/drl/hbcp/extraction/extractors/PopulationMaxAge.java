package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import org.apache.lucene.queryparser.classic.ParseException;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;

public class PopulationMaxAge extends PopulationMinOrMaxAge {

    public static final String ATTRIB_NAME = "Maximum Age";
    static final int[] MAX_AGE_VALID_RANGE = {9, 100};

    protected PopulationMaxAge(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, Attributes.get().getFromName(ATTRIB_NAME));
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        // this is selecting the third subgroup of each age regex (what we hope contain the max value of the range)
        int ageCandidate = Integer.valueOf(matcher.group(3));
        if (ageCandidate > MAX_AGE_VALID_RANGE[0] && ageCandidate < MAX_AGE_VALID_RANGE[1]) {
            return Sets.newHashSet(String.valueOf(ageCandidate));
        } else return new HashSet<>();
    }

    @Override
    protected String postProcess(String token) {
        if (token.matches("\\d+ \\d+")) {
            token = token.split(" ")[1];
        }
        return token;
    }
    
    @Override
    public String toString() {
        return "Max. Age";
    }
}
