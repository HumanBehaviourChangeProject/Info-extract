/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.attributes.quantization;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;

/**
 *
 * @author debasis
 */
public class Quantizer {

    public static Pair<Float, Float> getMinMax(Collection<? extends ArmifiedAttributeValuePair> pairs) {
        float val;
        float minVal = Float.MAX_VALUE, maxVal = 0;
        for (ArmifiedAttributeValuePair avp: pairs) {
            try {
                val = Float.parseFloat(avp.getValue());
            }
            catch (NumberFormatException nex) {
                return null;
            }
            if (val < minVal)
                minVal = val;
            if (val > maxVal)
                maxVal = val;
        }
        return Pair.of(minVal, maxVal);
    }
    
    public static List<NormalizedAttributeValuePair> quantize(
            Collection<? extends ArmifiedAttributeValuePair> pairs, int nIntervals, Pair<Float, Float> minMax) {
        if (!pairs.stream().allMatch(pair -> pair.getAttribute().equals(pairs.iterator().next().getAttribute())))
            throw new IllegalArgumentException("These attribute-value pairs should be from the same attribute.");
        if (pairs.isEmpty()) return Lists.newArrayList();
            
        // quantize each value
        return pairs.stream()
                .map(armifiedAVP -> new NormalizedAttributeValuePair(armifiedAVP,
                        getQuantizedValue(armifiedAVP, nIntervals, minMax)))
                .collect(Collectors.toList());
    }
    
    private static String getQuantizedValue(ArmifiedAttributeValuePair avp, int nIntervals, Pair<Float, Float> minMax) {
        if (minMax==null || nIntervals==0)
            return avp.getValue();
        
        if (avp.getAttribute().isOutcomeValue())
            return avp.getValue();  // no quantization on outcome values
        
        float val = Float.parseFloat(avp.getValue());        
        float min = minMax.getLeft();
        float max = minMax.getRight();
        float delta = (max - min)/(float)nIntervals;
        return String.valueOf ((int) ((val - min)/delta) );  // the bin id at which this value falls
    }
}
