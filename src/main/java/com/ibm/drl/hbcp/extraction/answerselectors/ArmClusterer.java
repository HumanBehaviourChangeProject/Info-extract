package com.ibm.drl.hbcp.extraction.answerselectors;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import com.aliasi.cluster.CompleteLinkClusterer;
import com.aliasi.cluster.Dendrogram;
import com.aliasi.cluster.HierarchicalClusterer;
import com.aliasi.spell.EditDistance;
import com.aliasi.util.Distance;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;

public class ArmClusterer implements AnswerSelector<ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>> {

    static final Distance<CharSequence> EDIT_DISTANCE
            = new EditDistance(false);

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> select(Collection<CandidateInPassage<ArmifiedAttributeValuePair>> candidates) {
        Set<String> armNames = new HashSet<>();
        for (CandidateInPassage<ArmifiedAttributeValuePair> candi : candidates) {
            ArmifiedAttributeValuePair cap = candi.getAnswer();
            armNames.add(cap.getValue());
        }

        for (String s1 : armNames) 
            for (String s2: armNames)
                if (s1.compareTo(s2) < 0)
                    System.out.println("distance(" + s1 + "," + s2 + ")="
                                       + EDIT_DISTANCE.distance(s1,s2));
        
        // Complete-Link Clusterer
        HierarchicalClusterer<String> clClusterer
            = new CompleteLinkClusterer<String>(Integer.MAX_VALUE, EDIT_DISTANCE);
        
        
        Dendrogram<String> clDendrogram
            = clClusterer.hierarchicalCluster(armNames);
        System.out.println("\nComplete Link Dendrogram");
        System.out.println(clDendrogram.prettyPrint());  
        
        
                // Dendrograms to Clusterings
        System.out.println("\nComplete Link Clusterings");
        for (int k = 1; k <= clDendrogram.size(); ++k) {
            Set<Set<String>> clKClustering = clDendrogram.partitionK(k);
            System.out.println(k + "  " + clKClustering);
        }
        
        return null;
        // TODO: "candidates" are all the candidates from the entire document, to cluster
    }

}
