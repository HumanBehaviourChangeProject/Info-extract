/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DecimalFormat;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * <p>
 * Returns node instance values for mixed gender type.
 * 
 * Assume the value returned is of the following format.
 * 
 * Male (%-ge) Female (%-ge)
 * 
 * Apply the following heuristics:
 * </p>
 * 
 * <ol>
 * 
 * <li>Look for the words 'male' and 'female'; determine their positions in text. </li>
 * <li> If both found then look for percentages. </li>
 * <li> If percentage not found then look for absolute numbers and calculate percentage from them. </li>
 * <li> If no numbers are found simply return the string "Male (unspecified) Female (unspecified)". </li>
 * <li> If only one gender is found, compute the percentage of the other. </li>
 * 
 * </ol>
 * 
 * <br>
 * Some example outputs:
 * <ol>
 * <li> "%_(N)_Male_79.7_(966)" - to -- "M (79.7) F (21.3)" </li>
 * <li> "%_(No)_of_men_45"  -- to -- "M (unspecified) F (unspecified)" </li>
 * <li> "(62%)_women" -- to -- "M (38) F (62)" </li>
 * <li> "45%female55%male" -- to -- "M (55) F (45)" </li>
 * </ol>
 * 
 * @author dganguly
 */
public class MixedGenderNormalizer implements Normalizer<AttributeValuePair> {
    
    public static String UNSPECIFIED_SYMB = "unspecified";
    private static final String UNSPECIFIED_TXT = fillNormalizedSlots(UNSPECIFIED_SYMB, UNSPECIFIED_SYMB);
    
    static Logger logger = LoggerFactory.getLogger(MixedGenderNormalizer.class);

    static String MALE_REGEX = "(\\b[mM]ale?)|(\\b[mM]en)";
    static Pattern malePattern = Pattern.compile(MALE_REGEX);
    static String FEMALE_REGEX = "([fF]emale)|([Ww]omen)";
    static Pattern femalePattern = Pattern.compile(FEMALE_REGEX);
    
    static String PERCENTAGE_NUMBER_REGEX = "([\\d+\\.]+%)";
    static Pattern strictPercentagePattern = Pattern.compile(PERCENTAGE_NUMBER_REGEX);
    static String ASSUME_PERCENTAGE_NUMBER_REGEX = "(\\d\\d?\\.\\d+%?)";
    static Pattern assumedPercentagePattern = Pattern.compile(ASSUME_PERCENTAGE_NUMBER_REGEX);
    
    static String ABS_NUMBER_REGEX = "\\d+";
    static Pattern absNumPattern = Pattern.compile(ABS_NUMBER_REGEX);

    private static DecimalFormat decimalFormat = new DecimalFormat("#.##");

    @Override
    public String getNormalizedValue(AttributeValuePair attribute) {
        return getNormalForm(attribute.getValue());
    }

    static int posOfMale(String text) {
        Matcher m = malePattern.matcher(text);
        return (m.find())? m.start() : -1;
    }
    
    static int posOfFemale(String text) {
        Matcher m = femalePattern.matcher(text);
        return (m.find())? m.start() : -1;
    }
    
    static String[] percentages(String text) {
        Matcher matcher = strictPercentagePattern.matcher(text);
        String[] values = new String[2];
        boolean found, percentageNotFound = false;
        int i = 0;
        while ((found = matcher.find()) && i < values.length) {
            values[i] = matcher.group();
            int len = values[i].length()-1;
            if (values[i].charAt(len) == '%')
                values[i] = values[i].substring(0, len);
            i++;
        }
        // for debugging check if there are more numbers
        if (matcher.find()) {
            logger.warn("More than two percentages were matched in: " + text);  
        }
        
        // try to match numbers we might assume are percentages (1-2 digits before a decimal with some digits after, no percent sign)
        if (i == 0) {
            matcher = assumedPercentagePattern.matcher(text);
            while ((found = matcher.find()) && i < values.length) {
                values[i++] = matcher.group();
            }
            // for debugging check if there are more numbers
            if (matcher.find()) {
                logger.warn("More than two numbers were matched in: " + text);  
            }
        }
        
        // Try to match whole numbers if percentages not found
        if (i == 0) {
            percentageNotFound = true;
            matcher = absNumPattern.matcher(text);
            while ((found = matcher.find()) && i < values.length) {
                values[i++] = matcher.group();
            }
            // for debugging check if there are more numbers
            if (matcher.find()) {
                logger.warn("More than two numbers were matched in: " + text);  
            }
        }
        
        if (i == 2 && percentageNotFound) {
            // if two absolute numbers
            float v0 = Float.parseFloat(values[0]);
            float v1 = Float.parseFloat(values[1]);
            float sum = v0 + v1;
            
            values[0] = decimalFormat.format(v0/sum*100);
            values[1] = decimalFormat.format(v1/sum*100);
        }
        
        return values;
    }
    
    static String inferPercentages(String[] percentages, boolean onlyMaleFound, boolean percentageFound) {
        float value;
        
        if (percentages[0] != null && (percentages[0].indexOf('.') >= 0 || percentageFound)) {
            // a floating point number
            try {
                value = Float.parseFloat(percentages[0]);
                if (value > 0 && value < 100)
                    return onlyMaleFound?
                            fillNormalizedSlots(value, 100-value) :
                            fillNormalizedSlots(100-value, value);
            }
            catch (NumberFormatException nex) {
                return UNSPECIFIED_TXT;                            
            }
        }
        return UNSPECIFIED_TXT;                            
    }
    
    
    private static String fillNormalizedSlots(float male, float female) {
        return fillNormalizedSlots(decimalFormat.format(male), decimalFormat.format(female));
    }

    private static String fillNormalizedSlots(String male, String female) {
        return "M (" + male + ") F (" + female + ")";
    }

    public static String getNormalForm(String text) {
        String text_lc = text.toLowerCase();
        text_lc = text_lc.replaceAll("_", " ");
        text_lc = text_lc.replaceAll("\\(", " ");
        text_lc = text_lc.replaceAll("\\)", " ");
//        text_lc = " " + text_lc;
        
        int posOfMale = posOfMale(text_lc);
        int posOfFemale = posOfFemale(text_lc);
        String[] percentages = percentages(text_lc);
        
        if (posOfMale < 0 && posOfFemale < 0)  // no mention of male or female
            return UNSPECIFIED_TXT;
        
        if (posOfMale >= 0) {
            if (posOfFemale >= 0) {  // both male and female mentioned
                if (percentages[0] != null && percentages[1] != null) {
                    // only valid case when male, female and two numbers are present
                    return fillNormalizedSlots(percentages[posOfMale < posOfFemale? 0:1], percentages[posOfMale < posOfFemale? 1:0]);
                }
                else
                    return UNSPECIFIED_TXT;
            }
            else {
                // male present -- but no female
                // only valid case if one number is present
                if (percentages[1] != null) {
                    return UNSPECIFIED_TXT;
                }
                else {
                    return inferPercentages(percentages, true, text.contains("%"));
                }
            }
        }
        else {
            // no male present
            if (posOfFemale >= 0) {
                // only female present -- no male
                // only valid case if one number is present
                if (percentages[1] != null) {
                    return UNSPECIFIED_TXT; // presence of two numbers --- ambiguous
                }
                else {
                    return inferPercentages(percentages, false, text.contains("%"));
                }
            }
            else {
                // no male no female
                return UNSPECIFIED_TXT;
            }
        }
    }
    
    public static void main(String[] args) {
        String[] texts = {
                "% (N) Male 79.7 (966)",
                "% (No) of men 45",
                "% fem",
                "(62%) women",
                "(Female)(49%",
                "(female/male)22/65",
                "(n) Men 87",
                "- female 56.8%",
                "142",
                "196181",
                "3.9%",
                "47.5%",
                "54%",
                "330 (65.6)",
                "45%female55%male",
                "48% were female",
                "50% male50% female",
                "50%males",
                "51 male68 female",
                "5 (113 male,",
                "64%) were male and 53 (36%) female",
                "70.0% male",
                "Fem",
                "Fema59Mal40",
                "Female",
                "Female,60.5",
                "Female30.2",
                "Female35%",
                "Female46.0%",
                "Female49",
                "Female60.5Male39.5",
                "Female70.",
                "Female (%)49",
                "Female 60%",
                "Female 61.2%",
                "Female gender46",
                "Gender (% women)69.8",
                "Gender (ma5",
                "Mal4",
                "Male",
                "Male/female46/44",
                "Male21.3",
                "Male43.3%",
                "Male44.7±2.7",
                "Male45",
                "Male46.2Female53.8",
                "Male68Æ67",
                "Male69Female31",
                "Male81.7%Female15.1%Transgender3.2%",
                "Male 100",
                "Male 62.0",
                "Male 98.8Female 1.2",
                "Male sex94.1",
                "Males",
                "Males55",
                "MalesFemales",
                "Men75.80",
                "Men78%",
                "Men95",
                "Men 61",
                "Men 868 (100)",
                "Sex (female) 63.0%",
                "Women",
                "Women26.8Men73.2",
                "Women40%Men60%",
                "Women50.2",
                "Women (%)42",
                "Women comprised 40%",
                "f e m a l e 5 0 . 0",
                "f mal (98",
                "fe- male",
                "female",
                "female45.7",
                "female (90%)",
                "females.",
                "male",
                "male52.8",
                "male64 %",
                "male93%",
                "male 64%",
                "males",
                "men",
                "men44-6%",
                "men46%",
                "men (n=89)women (n=110)",
                "women",
                "women(54%",
                "women60",
                "womenmen",
                };
        
        for (String text: texts) {
            System.out.println(text + "\t" + getNormalForm(text));
//            System.out.println("\"" + getNormalForm(text) + "\",");   // used to generate expected output for test class
        }
    }
}
