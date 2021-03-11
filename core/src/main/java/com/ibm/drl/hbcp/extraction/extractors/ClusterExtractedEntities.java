package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import org.apache.lucene.queryparser.classic.ParseException;

import com.aliasi.cluster.CompleteLinkClusterer;
import com.aliasi.cluster.Dendrogram;
import com.aliasi.cluster.HierarchicalClusterer;
import com.aliasi.spell.EditDistance;
import com.aliasi.util.Distance;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;

import java.util.HashMap;

/**
 * Extracts arms found in the paper by combining the number of predicted arms ({@link ArmNumberPrediction})
 * and the extracted names ({@link ArmNames}).
 * 
 * @author yhou
 *
 */
public class ClusterExtractedEntities {

    private ArmNumberPrediction armNumberPrediction;
    static final Distance<CharSequence> EDIT_DISTANCE = new EditDistance(false);


    public ClusterExtractedEntities() throws ParseException {
        armNumberPrediction = new ArmNumberPrediction(100);
    }
    
    static Distance<String> ArmName_DISTANCE = new Distance<String>() {
        public double distance(String name1, String name2) {
            return 1.0 - EDIT_DISTANCE.distance(name1, name2);
        }
    };
    

    public Map<Arm, Collection<CandidateInPassage<ArmifiedAttributeValuePair>>> cluster(IndexedDocument doc, Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results) throws IOException {
        Map<Arm, Collection<CandidateInPassage<ArmifiedAttributeValuePair>>> clusterRes= new HashMap<>();
        Set<String> armNames = new HashSet<>();
        for (CandidateInPassage<ArmifiedAttributeValuePair> candi : results) {
            ArmifiedAttributeValuePair cap = candi.getAnswer();
            armNames.add(cap.getArm().getStandardName());
        }
        
        // if there's no arm names, we can't cluster arm names, then put everything
        // in one cluster
        if(armNames.isEmpty()) {
            Arm arm = new Arm("general");
            clusterRes.put(arm, results);
            return clusterRes;
        }

        // determine number of clusters from number of arm prediction/extraction
        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armNumbersCollection = armNumberPrediction.extract(doc);
        int armNumber = 2;  // default number of arms
        if (!armNumbersCollection.isEmpty()) {
            String armNumPrediction = armNumbersCollection.stream().findFirst().get().getAnswer().getValue();
            try {
                // number of arms should be an integer (as a String) at this point
                armNumber = Integer.parseInt(armNumPrediction);  
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }



        // Complete-Link Clusterer
        HierarchicalClusterer<String> clClusterer
                = new CompleteLinkClusterer<String>(Integer.MAX_VALUE, EDIT_DISTANCE);

        Dendrogram<String> clDendrogram = clClusterer.hierarchicalCluster(armNames);
//        System.out.println("\nComplete Link Dendrogram");
//        System.out.println(clDendrogram.prettyPrint());

        // Dendrograms to Clusterings
        int clusterNum = armNumber;
        if (armNames.size() < armNumber) {
            // use the predicted number of arms unless there are fewer arm names than predicted arms
            clusterNum = armNames.size();
        }
//        System.out.println("\nComplete Link Clusterings");
        Set<Set<String>> clKClustering = clDendrogram.partitionK(clusterNum);
//        System.out.println(clKClustering);

        for (Set<String> arm_mentions: clKClustering) {
            // pick arm name
            String standardArmName = arm_mentions.stream().findFirst().get();
            Arm arm = new Arm(standardArmName);
            for (String str: arm_mentions) {
                arm.addName(str);
            }
            clusterRes.put(arm, new ArrayList<CandidateInPassage<ArmifiedAttributeValuePair>>());
            //change the arm for the results
            for (CandidateInPassage<ArmifiedAttributeValuePair> result : results) {
                ArmifiedAttributeValuePair cap = result.getAnswer();
                if(arm.getAllNames().contains(cap.getArm().getStandardName())){
                    clusterRes.get(arm).add(result);
                }
            }
            
        }
        return clusterRes;
    }
}
