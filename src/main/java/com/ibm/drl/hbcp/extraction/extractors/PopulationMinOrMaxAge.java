package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;

public abstract class PopulationMinOrMaxAge extends RegexQueryExtractor<ArmifiedAttributeValuePair>
        implements IndexBasedAVPExtractor {

    public final static String QUERY_STRING = "(participant OR smoker OR people OR person OR patient OR client OR respondent) AND (age OR year OR old)";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("from (\\d+) (to) (\\d+)"),
            Pattern.compile("between (\\d+) (to) (\\d+)"),
            Pattern.compile("between (\\d+) (and) (\\d+)"),
            Pattern.compile("(\\d+)(\\-|–)(\\d+)"));
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    protected PopulationMinOrMaxAge(IndexingMethod indexingMethod, int numberOfTopPassages, Attribute attribute) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = attribute;
        this.relevantAttributeSet = Sets.newHashSet(attribute);
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
                new MultiValueUnarmifiedEvaluator<>(predicateUnarmifiedEvaluator),
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }
}
