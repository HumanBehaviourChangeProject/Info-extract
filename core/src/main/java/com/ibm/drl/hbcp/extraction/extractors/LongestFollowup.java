package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

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
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extractor for getting all arm names. Best arm names are not selected per
 * passage or document but all candidate arm names are extracted.
 *
 * @author yhou
 *
 */
public class LongestFollowup extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    public final static String QUERY_STRING = "months OR days OR weeks OR years";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile(" and (\\d+) (days|weeks|months|years)"));
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;
    public static final String ATTRIB_NAME = "Longest follow up";

    public LongestFollowup(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public LongestFollowup(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES, false, new Identity<>());
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        String candidate1 = matcher.group(1);
        String candidate2 = matcher.group(2);
        return Sets.newHashSet(candidate1 + " " + candidate2);

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
        String preProcessedWindow = preProcess(passage.getText());
        Set<String> matches = getAllMatchingCandidates(preProcessedWindow);

        String[] tokens = preProcessedWindow.split("\\s+");

        List<Pair<String, Integer>> candidatesWithPosition = new ArrayList<>();
        List<Pair<String, Integer>> queryTermsWithPosition = new ArrayList<>();

        List<Pair<String, Integer>> withPosition = matches.stream().map(c -> Pair.of(c, passage.getText().indexOf(c))).collect(Collectors.toList());
        candidatesWithPosition.addAll(withPosition);

        for (int i = 0; i < tokens.length; i++) {
            int position = i; // forced to use an extra variable for the lambda
            String token = tokens[i];
            token = PaperIndexer.analyze(passage.getIndexManager().getAnalyzer(), token);

            // see if that token is a query term and set the position
            int index = Arrays.binarySearch(this.queryTerms, token);
            if (index > -1) {
                queryTermsWithPosition.add(Pair.of(token, position)); // i is the position of the query term in 'window'.
            }
        }

        List<Pair<String, Double>> res = getScoredCandidates(candidatesWithPosition, queryTermsWithPosition);
//        for(Pair pair: res){
//           System.err.println(pair.getLeft()  + ":" + pair.getRight() + "--"+ passage.getText());
//        }
        return res.stream()
                .map(candidateWithScore -> newCandidate(candidateWithScore.getLeft(), candidateWithScore.getRight(), passage))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return ATTRIB_NAME;
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        LongestFollowup followupExtractor = new LongestFollowup(100);
        try (IndexManager index = followupExtractor.getDefaultIndexManager(props)) {
            JSONRefParser refParser = new JSONRefParser(props);
            Cleaners cleaners = new Cleaners(props);
            List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth
                    = followupExtractor.getGroundTruthForEvaluation(index, refParser, cleaners);
            for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> docAndAnnotations : groundTruth) {
                Collection<ArmifiedAttributeValuePair> relevantAnnotations = followupExtractor.getRelevant(docAndAnnotations.getRight());
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> prediction = followupExtractor.extract(docAndAnnotations.getKey());
                System.err.println("doc:" + docAndAnnotations.getKey().getDocName());
                System.err.println("annotation:" + relevantAnnotations);
                System.err.println("prediction:" + prediction);
            }
        }
    }

}
