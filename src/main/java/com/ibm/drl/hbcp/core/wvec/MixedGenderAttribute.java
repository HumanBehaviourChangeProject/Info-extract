/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import com.ibm.drl.hbcp.core.attributes.normalization.normalizers.MixedGenderNormalizer;

/**
 *
 * @author dganguly
 */
public class MixedGenderAttribute {
    float malePercentage;
    float femalePercentage;

    static float NONSENSE_VAL = 10000;
    static String DELIM = "_";
    static String UNSPECIFIED = MixedGenderNormalizer.UNSPECIFIED_SYMB;
            
    // of the normal form corresponding to mixed genders
    // i.e. M (x in [0, 100] or UNSPECIFIED) F (y in [0, 100] or UNSPECIFIED) 
    public MixedGenderAttribute(String text) {
        String[] tokens = text.split("_");
        String percentage = tokens[1].replace('(', ' ').replace(')', ' ').trim();
        
        malePercentage = percentage.equals(UNSPECIFIED)? NONSENSE_VAL : Float.parseFloat(percentage);
        
        percentage = tokens[3].replace('(', ' ').replace(')', ' ').trim();
        femalePercentage = percentage.equals(UNSPECIFIED)? NONSENSE_VAL : Float.parseFloat(percentage);
    }

    public float getDistance(MixedGenderAttribute that) {
        return (float)Math.sqrt((this.malePercentage - that.malePercentage)*(this.malePercentage - that.malePercentage) +
                (this.femalePercentage - that.femalePercentage)*(this.femalePercentage - that.femalePercentage));
    }
}
