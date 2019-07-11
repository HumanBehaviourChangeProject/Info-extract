/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

/**
 * Aggregator for information units...
 * @author dganguly
 */

public class InformationUnits {
    HashMap<String, RetrievedUnit> iunits;

    public InformationUnits() {
        iunits = new HashMap<>();
    }
    
    /**
     * Aggregates a score with respect to a particular candidate answer,
     * e.g. the value of an attribute,
     * over the top ranked passages.
     * @param iu InformationUnit
     * @param rank The rank of this retrieved passage.
     * @param sim Similarity of the retrieved passage.
     */    
    public void aggregate(InformationUnit iu, int rank, float sim) {
        CandidateAnswer key = iu.getBestAnswer(); // get the most likely one, the one which minimizes inter-distances between positions
        if (key == null)
            return;        
        
        // Is there any previous evidence for this answer? If so, aggregate
        RetrievedUnit retunit = iunits.get(key.getKey());
        if (retunit == null) {
            retunit = new RetrievedUnit(iu, 0, rank);
            iunits.put(key.getKey(), retunit);
        }
        retunit.weight += iu.weight; // update the weight.. add to zero for the first time...
    }
    
    /**
     * Returns the 'InformationUnit' (with the best likely extracted value)
     * after aggregating over all possible candidate values.
     * @return 
     */
    public InformationUnit getPredicted() {
        if (iunits.size() == 0)
            return null;
        
        // Sort in descending order by the weights
        List<RetrievedUnit> runitList = new ArrayList<>(iunits.size());
        for (RetrievedUnit runit : iunits.values()) {            
            runitList.add(runit);
        }
        
        Collections.sort(runitList);
        
        // Write back the values to the IU object        
        return runitList.get(0).writeBackBestValues();
    }
    
   InformationUnit getArmsPrediction(){
        if (iunits.size() == 0)
            return null;
        
        // Sort in descending order by the weights
        List<RetrievedUnit> runitList = new ArrayList<>(iunits.size());
        for (RetrievedUnit runit : iunits.values()) {            
            runitList.add(runit);
        }
        
        Collections.sort(runitList);
        
        
        // Write back the arms information from all IUs to the returned IU object        
        String armsPrediction = "";
        for(String s: iunits.keySet()){
           armsPrediction = armsPrediction + ":::" + s;
        }
        runitList.get(0).mostLikelyAnswer.setKey(armsPrediction);;
        return runitList.get(0).writeBackBestValues();
        
    }    
    
}

