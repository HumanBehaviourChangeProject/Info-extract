package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;
import org.apache.lucene.queryparser.classic.ParseException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;

public class PopulationMinAge extends PopulationMinOrMaxAge {

    public static final String ATTRIB_NAME = "Minimum Age";
    private static final int[] MIN_AGE_VALID_RANGE = {9, 100};

    public PopulationMinAge(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, Attributes.get().getFromName(ATTRIB_NAME));
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        // this is selecting the first subgroup of each age regex (what we hope contain the min value of the range)
        int ageCandidate = Integer.valueOf(matcher.group(1));
        if (ageCandidate > MIN_AGE_VALID_RANGE[0] && ageCandidate < MIN_AGE_VALID_RANGE[1]) {
            return Sets.newHashSet(String.valueOf(ageCandidate));
        } else return new HashSet<>();
    }

    @Override
    protected String postProcess(String token) {
        if (token.matches("\\d+ \\d+")) {
            token = token.split(" ")[0];
        }
        return token;
    }

    @Override
    public String toString() {
        return "Min. Age";
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        PopulationMinAge minAgeExtractor = new PopulationMinAge(IndexingMethod.slidingWindow(50, props),5);
        for (RefComparison evaluation : minAgeExtractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}
