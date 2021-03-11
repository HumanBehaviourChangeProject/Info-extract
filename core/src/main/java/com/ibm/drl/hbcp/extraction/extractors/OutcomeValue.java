package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.answerselectors.Identity;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;
import org.apache.lucene.queryparser.classic.ParseException;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.jetbrains.annotations.NotNull;

public class OutcomeValue extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    public final static String QUERY_STRING = "(confirmed OR verified OR verification OR validated OR prevalence OR chemically OR continuous OR biochemical OR biochemically) AND (quit OR abstinence OR cessation)";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
//                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochemical|biochemically) .*?(quit|abstinence|cessation) .*?\\(.*?(\\d+\\%|\\d+\\.\\d+\\%).*?\\).*? (control|controlled) .*?\\(.*?(\\d+\\%|\\d+\\.\\d+\\%).*?\\).*?"),
//                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochemical|biochemically) .*?(quit|abstinence|cessation) .*?\\((\\d+\\%|\\d+\\.\\d+\\%)\\).*?(control|controlled) .*?\\((\\d+\\%|\\d+\\.\\d+\\%)\\).*?"),
//                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochemical|biochemically) .*?(quit|abstinence|cessation) .*?(\\d+\\%|\\d+\\.\\d+\\%) .*?(vs.|vs|compared to|and|versus|than) .*?(\\d+\\%|\\d+\\.\\d+\\%) .*? control group .*?"),
//                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochmical|biochemically) .*?(quit|abstinence|cessation) .*?(\\d+\\%|\\d+\\.\\d+\\%) .*?(vs.|vs|compared to|and|versus|than) (\\d+\\%|\\d+\\.\\d+\\%) (in|for|of) .*?"));
                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochemical|biochemically) .*?(quit|abstinence|cessation) .*?\\(.*?(\\d+\\%|\\d+\\.\\d+\\%).*?\\).*? (control|controlled) .*?\\(.*?(\\d+\\%|\\d+\\.\\d+\\%).*?\\).*?"),
                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochemical|biochemically) .*?(quit|abstinence|cessation) .*?\\((\\d+\\%|\\d+\\.\\d+\\%)\\).*?(control|controlled) .*?\\((\\d+\\%|\\d+\\.\\d+\\%)\\).*?"),
                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochemical|biochemically) .*?(quit|abstinence|cessation) .*?(\\d+\\%|\\d+\\.\\d+\\%) .*?(vs.|vs|compared to|and|versus|than) .*?(\\d+\\%|\\d+\\.\\d+\\%) .*? control group .*?"),
                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochemical|biochemically) .*?(quit|abstinence|cessation|abstinent) .*?(\\d+\\%|\\d+\\.\\d+\\%) .*?(vs.|vs|compared to|and|versus|than) .*?(\\d+\\%|\\d+\\.\\d+\\%).*?"),
                Pattern.compile(".*?(confirmed|verified|verification|validated|prevalence|chemically|continuous|biochmical|biochemically) .*?(quit|abstinence|cessation) .*?(\\d+\\%|\\d+\\.\\d+\\%) .*?(vs.|vs|compared to|and|versus|than) (\\d+\\%|\\d+\\.\\d+\\%) (in|for|of) .*?"));
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;
//    public static final String ATTRIB_ID = "5140146";
    public static final String ATTRIB_NAME = "Outcome value";

    public OutcomeValue(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES, true, new Identity<>());
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    public OutcomeValue(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        String ov1 = matcher.group(3);
        String ov2 = matcher.group(5);
        return Sets.newHashSet(ov1, ov2);
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
    public String toString() {
        return "Outcome value";
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        OutcomeValue ovExtractor = new OutcomeValue(50);
        for (RefComparison evaluation : ovExtractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}
