package com.ibm.drl.hbcp.extraction.extractors;

import static java.lang.Math.abs;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.util.Props;

import javassist.bytecode.stackmap.TypeData.ClassName;

/**
 * Associate extracted entities with arms
 * Input: entities per doc and arm per doc
 * Output : 
 * @author francesca
 */
public class ArmAssociator implements Associator<ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>> {
    
    private static final Logger LOGGER = LoggerFactory.getLogger( ClassName.class.getName() );
    
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> associate(
            Collection<CandidateInPassage<ArmifiedAttributeValuePair>> predictedCandidates,
            Collection<Arm> extractedArms) {
        
        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armifiedAVPs = new ArrayList<>();
        
        //start scanning the entities
        for (CandidateInPassage<ArmifiedAttributeValuePair> toBeArmifiedCandidate : predictedCandidates) {
            
            Passage candPassage = toBeArmifiedCandidate.getPassage();
            ArmifiedAttributeValuePair toBeArmifiedAvp = toBeArmifiedCandidate.getAnswer();
            int minDistance = 1000000;
            Arm closestArm = null;
            String closestArmName = "";

            String docName = candPassage.getDocname();
            String candidateText = candPassage.getText();
            int valuePosition = candidateText.indexOf(toBeArmifiedAvp.getValue());  // location of AVP in passage
            
            //start scanning arms per entity
            for (Arm arm : extractedArms) {
                for (String armName : arm.getAllNames()) {
                    int armPosition = candidateText.indexOf(armName);  // location of arm name in passage
                    if (armPosition == -1) {
                        LOGGER.debug("This arm name not found in passage{0}\n{1}", new Object[]{candidateText, armName});
                        continue;
                    }
                    int dist =  abs(valuePosition - armPosition);
                    if (dist < minDistance) {
                        minDistance = dist;
                        closestArm = arm;
                        closestArmName = armName;
                    }
                }
            }
            
            //for this entity, assign the closest arm to ARM
            ArmifiedAttributeValuePair armifiedAvp;
            if (closestArm !=null) {   
                // copy ArmifiedAttributeValuePair: all values are the same as in the unArmified,  except for the Arm

                //armified AVP with closest name 
                armifiedAvp = new ArmifiedAttributeValuePair(toBeArmifiedAvp.getAttribute(), toBeArmifiedAvp.getValue(), docName, closestArmName, toBeArmifiedAvp.getContext());
            
                LOGGER.debug("==========================================\nValue: {0}\n Passage: {1}\n val pos: {2}\n arm pos: {3}\n closest arm: {4}\n with Arm Name: {5}"
                         + "\n=====================================================", 
                         new Object[]{toBeArmifiedAvp.getValue(), candidateText, valuePosition, minDistance, 
                             closestArm.getStandardName(), closestArmName});
            } else {
                 //if no Arm as been assigned, add default arm to AttributeValuePair
                 armifiedAvp = new ArmifiedAttributeValuePair(toBeArmifiedAvp.getAttribute(), toBeArmifiedAvp.getValue(), docName, Arm.EMPTY, toBeArmifiedAvp.getContext());
            }
            // make candidate with new armified AVP with closest Arm or default Arm
            CandidateInPassage<ArmifiedAttributeValuePair> armifiedCandidate = new CandidateInPassage(candPassage, armifiedAvp, toBeArmifiedCandidate.getScore(), toBeArmifiedCandidate.getAggregationScore());
            armifiedAVPs.add(armifiedCandidate);
       
        }
        return armifiedAVPs;
    }
    
    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        ArmAssociator aa = new ArmAssociator();
        //IndexBasedAVPExtractor extractor = new PopulationMinAge(ResearchDoc.FIELD_CONTENT,5);
        IndexBasedAVPExtractor extractor = new PopulationMeanAge(IndexingMethod.NONE,5);
        //IndexBasedAVPExtractor extractor =  new OutcomeValue(ResearchDoc.FIELD_CONTENT,  5);
        ArmsExtractor armsExtractor = new ArmsExtractor();
        
        try (IndexManager index = extractor.getDefaultIndexManager(props)) {
            List<IndexedDocument> allDocs = index.getAllDocuments();
            
            for (IndexedDocument doc : allDocs) {
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> values = extractor.extract(doc);
                Collection<Arm> arms = armsExtractor.extract(doc).stream().map(x -> x.getAnswer()).collect(Collectors.toSet());
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> valuesWithArms = aa.associate(values, arms);
                
                System.out.println(values.size() +"-"+valuesWithArms.size() );
                System.out.println(doc.getDocName());
                
                valuesWithArms.forEach((valueWA) -> {
                    System.out.println(
                            valueWA.getAnswer().getAttribute().getName() +" : "
                            +valueWA.getAnswer().getValue() +" : " 
                            + valueWA.getAnswer().getArm().getStandardName()
                            + " : " + valueWA.getPassage().getText()
                            );
                });
            
            }
               
        }    
    
    }

}