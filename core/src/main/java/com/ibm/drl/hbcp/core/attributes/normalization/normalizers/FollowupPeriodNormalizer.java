/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * For follow up attributes, normalize the values to weeks. Guess the unit
 * from additional text if not present in the annotated text.
 * 
 * @author dganguly
 */

public class FollowupPeriodNormalizer implements Normalizer<ArmifiedAttributeValuePair> {
    
    static String[] UNITS = { "week", "month" };
    static String[] DIGITS_IN_WORDS = { "zero", "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "ten", "eleven", "twelve"
    }; 
    static WEEK_MONTH_ENUM[] UNIT_VALUES = { WEEK_MONTH_ENUM.WEEK, WEEK_MONTH_ENUM.MONTH };    
    
    static String NUMBER_REGEX = "\\d+\\.\\d+|\\d+"; //"\\\\d+(\\\\.\\\\d+)?";  // "\\\\d+\\\\.?\\\\d+";
    static Pattern pattern = Pattern.compile(NUMBER_REGEX);
    static final int WEEK_THRESHOLD = 12;  //higher than this ==> classify as weeks

    @Override
    public String getNormalizedValue(ArmifiedAttributeValuePair attributeValue) {
        return getValueInUnits(attributeValue.getContext(), attributeValue.getValue());
    }

    static WEEK_MONTH_ENUM getUnitType(String text) {
        int i;
        WEEK_MONTH_ENUM unit;
        // search in annotated text
        for (i=0; i < UNITS.length; i++) {
            int index = text.indexOf(UNITS[i]);
            if (index >= 0) {
                unit = UNIT_VALUES[i];
                return unit;
            }
        }
        return WEEK_MONTH_ENUM.NONE;
    }
    
    private static String getValueInUnits(String context_param, String value_param) {
        WEEK_MONTH_ENUM unit;
        String context = context_param.toLowerCase();
        String value = value_param.toLowerCase();
        
        Matcher matcher = pattern.matcher(value);
        String numericalValue = null;
        float valueInUnits = 0.0f;
        
        boolean found = matcher.find();        
        if (found) {
            numericalValue = matcher.group();
        }
        else {   
            for (int i = 0; i < DIGITS_IN_WORDS.length; i++) {
                String word = DIGITS_IN_WORDS[i];
                if (value_param.indexOf(word) >= 0) {
                    found = true;
                    numericalValue = String.valueOf(i);
                    break;
                }
            }
            if (!found)
                return value_param;  // no numerical value was found... so, this back-off strategy is to return the whole annotated text
        }

        unit = getUnitType(value_param);
        // if not found then search in additional text
        if (unit == WEEK_MONTH_ENUM.NONE)
            unit = getUnitType(context);

        /*
        if (unit == WEEK_MONTH_ENUM.NONE)
            unit = WEEK_MONTH_ENUM.WEEK; // default of week
        */
        
        
        float fvalue = Float.parseFloat(numericalValue);
        valueInUnits = fvalue;
        
        if (unit == WEEK_MONTH_ENUM.NONE)
            unit = fvalue >= WEEK_THRESHOLD? WEEK_MONTH_ENUM.WEEK : WEEK_MONTH_ENUM.MONTH;
        
        // convert months to weeks
        if (unit == WEEK_MONTH_ENUM.MONTH) {
            valueInUnits = fvalue * 4.5f;
        }
        return String.valueOf(valueInUnits);
    }

    protected enum WEEK_MONTH_ENUM {
        NONE,
        WEEK,
        MONTH
    };

    public static void main(String[] args) {
        System.out.println(FollowupPeriodNormalizer.getValueInUnits("follow-up of 2.3 weeks", "2.3 weeks"));
        System.out.println(FollowupPeriodNormalizer.getValueInUnits("follow-up of 2 weeks", "2 weeks"));
        System.out.println(FollowupPeriodNormalizer.getValueInUnits("follow-up of 3.weeks for 12 months.", "3.weeks"));
        System.out.println(FollowupPeriodNormalizer.getValueInUnits("follow-up of 6 months", "6 months"));
        System.out.println(FollowupPeriodNormalizer.getValueInUnits("Additionally,\\rself-report 7-day point-prevalence smoking abstinence was collected at 3 and 6 months, and\\rverified biochemically at 6 months using saliva cotinine.", "6"));
    }
}
