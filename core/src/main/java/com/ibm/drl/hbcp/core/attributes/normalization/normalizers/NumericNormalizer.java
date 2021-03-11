package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

/**
 * Default normalizer for regular numeric values.
 * The normalized value is the first number in the string value of the attribute.
 * @author dganguly
 */
public class NumericNormalizer implements Normalizer<AttributeValuePair> {
    Map<String, Float> defaultValues;

    public final static boolean normalizeIn0_1 = false;

    public final static Pattern NUMBER_REGEX = Pattern.compile("\\d+\\.\\d+|\\d+"); //"\\\\d+(\\\\.\\\\d+)?";  // "\\\\d+\\\\.?\\\\d+";
    public final static Pattern NUMBER_WITH_PERCENT_REGEX = Pattern.compile("\\d+\\.\\d+%|\\d+%"); //"\\\\d+(\\\\.\\\\d+)?";  // "\\\\d+\\\\.?\\\\d+";
    
    public NumericNormalizer() { }
    
    public NumericNormalizer(Properties prop) {
        defaultValues = new HashMap<>();
        
        String[] attrIdDefaultValuePairs = prop.getProperty("prediction.attribtype.numerical.defaults").split(",");
        if (attrIdDefaultValuePairs.length > 1) {  // there may be no default values
            for (String attrIdDefaultValuePair : attrIdDefaultValuePairs) {
                String[] tokens = attrIdDefaultValuePair.split(":");
                defaultValues.put(tokens[0], Float.parseFloat(tokens[1]));
            }
        }
    }
    
    @Override
    public String getNormalizedValue(AttributeValuePair a) {
        String annotatedText = a.getValue();
        
        Matcher matcher = NUMBER_REGEX.matcher(annotatedText); // try with the longer regexp including the % symbol
        if (matcher.find()) {
            String numericalValue = matcher.group();
            double value = Double.parseDouble(numericalValue);
            String doubleString = String.valueOf(value);
            return clean(doubleString);
        }

        if (a.getAttribute() != null && defaultValues != null) {
            Float dv = defaultValues.get(a.getAttribute().getId());
            if (dv != null) {
                return dv.toString();
            }
        }
        
        // if not a number returning original text.  Calling method must handle if not a number.
        return annotatedText;
    }

    private String clean(String doubleString) {
        String res = doubleString;
        res = res.endsWith(".0") ? res.substring(0, res.length() - 2) : res;
        return res;
    }
    
    public static void main(String[] args) {
        String[] tests = {"014.3% of people stopped smoking", "2.15% of", "10%" };
        
        for (String test: tests) {
            //Matcher matcher = NUMBER_WITH_PERCENT_REGEX.matcher(test);
            Matcher matcher = NUMBER_WITH_PERCENT_REGEX.matcher(test);
            if (matcher.find())
                System.out.println(matcher.group());
        }
    }
} 
