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
    
    static final int MALE_ONLY = 0;
    static final int FEMALE_ONLY = 1;
    static final int MIXED = 2;
    static final int UNSPECIFIED_CLASS = 3;
    static final String[] CLASS_NAMES = { "MALE_ONLY", "FEMALE_ONLY", "MIXED", "UNSPECIFIED_CLASS"};
            
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
    
    public float getMalePercent() { return malePercentage; }
    public float getFeMalePercent() { return femalePercentage; }
    
    public String getNominalClass() {
        return
            malePercentage!=NONSENSE_VAL && femalePercentage==NONSENSE_VAL? CLASS_NAMES[MALE_ONLY]:
            malePercentage!=NONSENSE_VAL && femalePercentage!=NONSENSE_VAL? CLASS_NAMES[MIXED]:
            malePercentage==NONSENSE_VAL && femalePercentage!=NONSENSE_VAL? CLASS_NAMES[FEMALE_ONLY]:
            CLASS_NAMES[UNSPECIFIED_CLASS]
        ;
    }
    
    static public String getNominalClassNames() {
        StringBuffer buff = new StringBuffer("{");
        for (String className: CLASS_NAMES) {
            buff.append(className).append(",");
        }
        buff.deleteCharAt(buff.length()-1);
        buff.append("}");
        return buff.toString();
    }
    
}
