/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
import ref.ReferenceAttributeValues;

/**
 * Aggregator for information units.
 * @author dganguly
 */

public class InformationUnits {
    HashMap<String, RetrievedUnit> iunits;

    public InformationUnits() {
        iunits = new HashMap<>();
    }
    
    /**
     * Aggregates over the top ranked passages.
     * @param iu InformationUnit
     * @param rank The rank of this retrieved passage.
     * @param sim Similarity of the retrieved passage.
     */
    public void aggregate(InformationUnit iu, int rank, float sim) {
        CandidateAnswer key = iu.getBestAnswer(); // get the most likely one, the one which minimizes inter-distances between positions
        if (key == null)
            return;        
        
        // Is there any previous evidence for this answer? If so, aggregate
        RetrievedUnit retunit = iunits.get(key.key);
        if (retunit == null) {
            retunit = new RetrievedUnit(iu, 0, rank);
            iunits.put(key.key, retunit);
        }
        retunit.weight += iu.weight; // update the weight.. add to zero for the first time...
    }
    
    /**
     * Get the best predicted value.
     * @return The best value encoded as a part of an 'InformationUnit' object.
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
}

