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

public class AbstinencePointPrevalent extends OrOfKeywordsExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final List<String> QUERY_WORDS = Lists.newArrayList(
            "point prevalence",
            "point-prevalence",
            // not sure if these two are different queries, most likely not
            "past 7 day abstinence",
            "past 7-day abstinence",
            "7-day abstinence",
            "PPA",
            "brief abstinence",
            "7-day biochemically confirmed abstinence",
            "7-day smoking cessation rate",
            "7-day smoking cessation",
            "Point prevalence smoking cessation rate",
            "Point prevalent smoking abstinence",
            "Point prevalent abstinence",
            "Abstinent for past 7 days",
            "7-day point prevalence cigarette abstinence",
            "past 30 day abstinence",
            "past 30-day abstinence",
            "30-day abstinence",
            "30-day biochemically confirmed abstinence",
            "30-day smoking cessation rate",
            "30-day smoking cessation",
            "Abstinent for past 30 days",
            "30-day point prevalence cigarette abstinence"
    );

    private final String ATTRIBUTE_NAME = NonAnnotatedAttributes.PointPrevalentAbstinence.getName();
    private final Attribute attribute = Attributes.get().getFromName(ATTRIBUTE_NAME);

    public AbstinencePointPrevalent(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
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
