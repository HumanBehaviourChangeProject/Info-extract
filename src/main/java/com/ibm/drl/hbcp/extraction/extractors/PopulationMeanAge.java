
package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;
/**
 * Implementation of the mean Age extraction. Extends RegexQueryExtractor and implements SingleAttributeExtractor
 * Override isValidCandidate with the mean age specific logic and getQuery with the Mean age specific queries
 * 
 *
 * @author francesca
 */
public class PopulationMeanAge extends RegexQueryExtractor<ArmifiedAttributeValuePair>
    implements IndexBasedAVPExtractor {
    private static final Logger logger = LoggerFactory.getLogger(PopulationMeanAge.class);

    public static final String ATTRIB_NAME = "Mean Age";
    public static final String QUERY_STRING = "(average OR mean) AND (age OR year)";
    private static final List<Pattern> REGEXES = Lists.newArrayList(
           Pattern.compile("[0-9]+([.,][0-9]{1,10})?"));
            //Pattern.compile("([0-9]+[^%]?([.,][0-9]{1,10}[^%])?)"));
    private final static Pattern NUM_PATTERN = Pattern.compile("\\d+(\\S\\d+)?");  // \\S catches bad OCR conversion for periods in floats
    private final static int[] ALLOWABLE_RANGE = { 10, 70 };
    private final Attribute attribute = Attributes.get().getFromName(ATTRIB_NAME);
    private final Set<Attribute> relevantAttributesForEvaluation = Sets.newHashSet(attribute);

    public PopulationMeanAge (IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
    }

    protected Set<String> getValidMatches(Matcher matcher) {
        String numberString = postProcess(matcher.group(0));
        double val = Double.parseDouble(numberString);
        
        if (val > ALLOWABLE_RANGE[0] && val <= ALLOWABLE_RANGE[1])
            return Sets.newHashSet(numberString);
        else return Sets.newHashSet();
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
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateStringUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
            @Override
            public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                return expected.getValue().contains(predicted.getValue());
            }
        };
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
            @Override
            public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                // In some annotations, there're multiple values (with standard deviation)
                // We assume the mean is the first and extract only that
//                System.out.println("Expected: " + expected.getValue() + "; Predicted: " + predicted.getValue());
                Matcher matcher = NUM_PATTERN.matcher(expected.getValue());
                if (matcher.find()) {
                    String expectedNumberStr = matcher.group();
                    double expectedNumber = getFloatVal(expectedNumberStr);
                    // compare the integer part
//                    if (Math.floor(Double.parseDouble(predicted.getValue())) == Math.floor(expectedNumber))
//                        System.out.println("CORRECT");
//                    else
//                        System.out.println("INCORRECT");

                    return Math.floor(Double.parseDouble(predicted.getValue())) == Math.floor(expectedNumber);
                } else {
                    logger.info("Couldn't find number in gold annotation: " + expected.getValue());
//                    System.out.println("INCORRECT");
                    return false;
                }
            }
        };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new MultiValueUnarmifiedEvaluator<>(predicateUnarmifiedEvaluator),
//                predicateStringUnarmifiedEvaluator,
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    protected String postProcess(String token) {
        return cleanMalformedFloats(token);
    }

    private static double getFloatVal(String expectedNumberStr) {
        try {
             return Double.parseDouble(expectedNumberStr);
        } catch (NumberFormatException e) {
            // OCR or something in in PDF conversion leaves other characters we still match with regex (e.g., 39·5 or 45¡13)
            // here replace any non-digit with . (should only match 1 with NUM_PATTERN regex)
            try {
                return Double.parseDouble(cleanMalformedFloats(expectedNumberStr));
            } catch (NumberFormatException e1) {
                return 0.0;
            }
        }
    }

    private static String cleanMalformedFloats(String malformedFloat) {
        return malformedFloat.replaceAll("[^0-9]", ".");
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributesForEvaluation;
    }

    @Override
    public String toString() {
        return "Mean Age";
    }
    
    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        PopulationMeanAge meanAgeExtractor = new PopulationMeanAge(IndexingMethod.slidingWindow(10, props), 5);
        for (RefComparison evaluation : meanAgeExtractor.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}

