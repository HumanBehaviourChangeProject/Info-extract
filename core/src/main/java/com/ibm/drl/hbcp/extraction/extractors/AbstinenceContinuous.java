package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.NonAnnotatedAttributes;
import org.apache.lucene.queryparser.classic.ParseException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class AbstinenceContinuous extends OrOfKeywordsExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final List<String> QUERY_WORDS = Lists.newArrayList(
            "continuous smoking abstinence",
            "continuous abstinence rate",
            "continuous quit rate",
            "sustained abstinence",
            "prolonged abstinence",
            // how many options here? we can't do numerical wildcard patterns
            "cessation for 3 months",
            "cessation for 6 months",
            "cessation for 12 months",
            "sustained prevalence quitting"
    );

    private final String ATTRIBUTE_NAME = NonAnnotatedAttributes.ContinuousAbstinence.getName();
    private final Attribute attribute = Attributes.get().getFromName(ATTRIBUTE_NAME);

    public AbstinenceContinuous(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_WORDS, false);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return Sets.newHashSet(attribute);
    }

    @Override
    protected CandidateInPassage<ArmifiedAttributeValuePair> newCandidate(String value, double score, Passage passage) {
        return new CandidateInPassage<>(
                passage,
                new ArmifiedAttributeValuePair(attribute, value, passage.getDocname(), Arm.EMPTY, passage.getText()),
                score,
                1.0);
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        // we don't have annotations for this one, therefore cannot evaluate it this way
        return new ArrayList<>();
    }

    @Override
    public String toString() {
        return ATTRIBUTE_NAME;
    }
}
