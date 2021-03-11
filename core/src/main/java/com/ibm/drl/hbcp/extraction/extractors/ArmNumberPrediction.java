package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import org.apache.lucene.queryparser.classic.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.ArmNumberEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;
import java.util.Collection;
import org.apache.commons.lang3.tuple.Pair;

/**
 * Extract the number of (study) arms in the paper.
 * This should return the candidate number of arms as a string of digits (i.e., "two" will be converted to "2").
 * If no candidate arms are found, methods calling this extractor might want to default to 2.
 * 
 * @author yhou
 *
 */
public class ArmNumberPrediction extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    public final static String QUERY_STRING = "groups";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile(" into (\\d) groups"),
            Pattern.compile(" into (\\w+) groups"),
            Pattern.compile(" (\\d) groups"));
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;
    public static final String ATTRIB_NAME = "Arm name";

    public ArmNumberPrediction(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }
    
    public ArmNumberPrediction(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES, true);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }   

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        String armCandidate = matcher.group(1);
        if (armCandidate.matches("(2|3|4|5|two|three|four|five)")) {
            return Sets.newHashSet(armCandidate);
        }
        return new HashSet<>();
    }

    @Override
    protected CandidateInPassage<ArmifiedAttributeValuePair> newCandidate(String value, double score, Passage passage) {
        String numberString = convertNumericWordToDigit(value);
        return new CandidateInPassage<>(
                passage,
                new ArmifiedAttributeValuePair(attribute, numberString, passage.getDocname(), Arm.EMPTY, passage.getText()),
                score,
                1.0);
    }   
    
    private String convertNumericWordToDigit(String value) {
        // TODO may be possible with one of the normalizers
        String ret = value;
        if (value.equalsIgnoreCase("two")) {
            ret = "2";
        } else if (value.equalsIgnoreCase("three")) {
            ret = "3";
        } else if (value.equalsIgnoreCase("four")) {
            ret = "4";
        } else if (value.equalsIgnoreCase("five")) {
            ret = "5";
        }
        // TODO check if only 2,3,4,5?
        return ret;
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        return Lists.newArrayList(
                new ArmNumberEvaluator()
        );
    }
    
    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        ArmNumberPrediction armnumberExtractor = new ArmNumberPrediction(100);
        try (IndexManager index = armnumberExtractor.getDefaultIndexManager(props)) {
            JSONRefParser refParser = new JSONRefParser(props);
            Cleaners cleaners = new Cleaners(props);
            List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth
                    = armnumberExtractor.getGroundTruthForEvaluation(index, refParser, cleaners);
            for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> docAndAnnotations : groundTruth) {
                System.err.println("doc:" + docAndAnnotations.getKey().getDocName());
                Collection<ArmifiedAttributeValuePair> relevantAnnotations = armnumberExtractor.getRelevant(docAndAnnotations.getRight());
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> prediction = armnumberExtractor.extract(docAndAnnotations.getKey());
                System.err.println("annotation:" + relevantAnnotations.size());
                System.err.println("prediction:" + prediction);
            }
        }
//        for (RefComparison evaluation : armnumberExtractor.evaluate(props)) {
//            System.out.println(String.format("Accuracy: %.1f %%", evaluation.getAccuracy1()*100));
//            System.out.println(evaluation);
//        }

    }
    


}
