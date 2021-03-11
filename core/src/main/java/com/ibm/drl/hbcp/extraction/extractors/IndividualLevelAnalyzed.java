/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
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
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.util.Props;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

/**
 *
 * @author francesca
 * 
 * Feature of the attribute: number of patients analyzed. 
 * Integer not a percentage that in our data ranges between 

* summary(d)
      X444       
 Min.   :   0.0  
 1st Qu.:  57.5  
 Median : 162.0  
 Mean   : 338.9  
 3rd Qu.: 401.0  
 Max.   :8144.0  
> 
Some noise in the data.
* usual context: very different. The only consistent is N or n = x and x subjects or  similar words
* Needs long windows ( 100/300)
* 
*
* 
*/

public class IndividualLevelAnalyzed  extends RegexQueryExtractor<ArmifiedAttributeValuePair>
        implements IndexBasedAVPExtractor {
    private static final String ATTRIB_NAME = "Individual-level analysed";
    
    
   
    public final static String QUERY_STRING = "(participants OR users OR people OR sample OR patients OR subjects OR group)";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile("([Nn]( )*=( )*(?<number>\\d+))")
            //the following pattern improves drastically recall, but lower a bit precision.
            //Pattern.compile("(?<number>\\d+) (subjects|participants|patients)")
            
            );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    protected IndividualLevelAnalyzed(IndexingMethod indexingMethod, int numberOfTopPassages, Attribute attribute) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = attribute;
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

       public IndividualLevelAnalyzed(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }
       
       
     public IndividualLevelAnalyzed(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

     
     @Override
         protected Set<String> getValidMatches(Matcher matcher) {
        String iLA = matcher.group("number");
        //String ov2 = matcher.group(5);
        return Sets.newHashSet(iLA);
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

    @Override
    public String toString() {
        return ATTRIB_NAME;
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        IndividualLevelAnalyzed extractor = new IndividualLevelAnalyzed(30);
       try (IndexManager index = extractor.getDefaultIndexManager(props)) {
            JSONRefParser refParser = new JSONRefParser(props);
            Cleaners cleaners = new Cleaners(props);
            List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth
                    = extractor.getGroundTruthForEvaluation(index, refParser, cleaners);
            for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> docAndAnnotations : groundTruth) {
                Collection<ArmifiedAttributeValuePair> relevantAnnotations = extractor.getRelevant(docAndAnnotations.getRight());
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> prediction = extractor.extract(docAndAnnotations.getKey());
                System.err.println("doc:" + docAndAnnotations.getKey().getDocName());
                System.err.println("annotation:" + relevantAnnotations);
                System.err.println("prediction:" + prediction);
            }
            for (RefComparison evaluation : extractor.evaluate(props)) {
                System.out.println(evaluation);
            }
        }
        
        
   }
}
