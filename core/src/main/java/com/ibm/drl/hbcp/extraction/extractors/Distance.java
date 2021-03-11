package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;

public class Distance extends OrOfKeywordsExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final List<String> QUERY_WORDS = Lists.newArrayList(
            "telephone",
            //"call", "calls",
            "phone",
            "quit line", "quitline", "hotline"
    );
    private static final String ATTRIB_NAME = "Distance";

    private final Attribute attribute;

    public Distance(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_WORDS, true);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
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
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
                    @Override
                    public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                        return expected.getContext().contains(predicted.getValue());
                    }
                };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public String toString() {
        return ATTRIB_NAME;
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        Distance distance = new Distance(IndexingMethod.slidingWindow(50, props),5);
        for (RefComparison evaluation : distance.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}