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
import com.ibm.drl.hbcp.extractor.RefComparison;
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
 * Feature of the attribute: type of material on which the intervention is distributed

* usually text type, as in booklets, brochures etc... 
> 
* 
*
* 
*/

public class PrintedMaterial  extends RegexQueryExtractor<ArmifiedAttributeValuePair>
        implements IndexBasedAVPExtractor {
    private static final String ATTRIB_NAME = "Printed material";
    
    
   
    public final static String QUERY_STRING = "(voucher OR booklet OR manual OR manuals OR booklets OR materials OR material OR brochure OR kit OR kits OR pamphlets OR pamphlet OR postcard OR phone calls)";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
          //  Pattern.compile("(?<all>\\w+ (booklet|manual|material|brochure|kit|"
                    //+ "pamphlet|postcard|phone call)(s)?)")
              Pattern.compile("(?<all>(booklet|manual|material|brochure|kit|"
                    + "pamphlet|postcard|phone call|voucher)(s)?)")
            
            );
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;

    protected PrintedMaterial(IndexingMethod indexingMethod, int numberOfTopPassages, Attribute attribute) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = attribute;
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

       public PrintedMaterial(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }
       
       
     public PrintedMaterial(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

     
     @Override
         protected Set<String> getValidMatches(Matcher matcher) {
        String pm = matcher.group("all");
        //String ov2 = matcher.group(5);
        return Sets.newHashSet(pm);
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
    
    
    
    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
     
       int [] windowsSizes ={10,20,30,100,200};
       for (int i=0; i<windowsSizes.length;i++){
       PrintedMaterial extractor = new PrintedMaterial(windowsSizes[i]);
       System.out.println("Windows size: "+windowsSizes[i]);
       try (IndexManager index = extractor.getDefaultIndexManager(props)) {
           JSONRefParser refParser = new JSONRefParser(props);
           Cleaners cleaners = new Cleaners(props);
           List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth
                   = extractor.getGroundTruthForEvaluation(index, refParser, cleaners);
           // for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> docAndAnnotations : groundTruth) {
             //   Collection<ArmifiedAttributeValuePair> relevantAnnotations = extractor.getRelevant(docAndAnnotations.getRight());
               // Collection<CandidateInPassage<ArmifiedAttributeValuePair>> prediction = extractor.extract(docAndAnnotations.getKey());
                //System.err.println("doc:" + docAndAnnotations.getKey().getDocName());
                //System.err.println("annotation:" + relevantAnnotations);
                //System.err.println("prediction:" + prediction);
            //}
            for (RefComparison evaluation : extractor.evaluate(props)) {
                System.out.println(evaluation);
            }
        }
       }
        
   }
}
