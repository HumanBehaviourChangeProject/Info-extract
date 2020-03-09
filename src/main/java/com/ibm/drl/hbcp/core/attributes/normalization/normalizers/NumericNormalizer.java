package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

import java.util.HashMap;
import java.util.Map;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default normalizer for regular numeric values.
 * The normalized value is the first number in the string value of the attribute.
 * @author dganguly
 */
public class NumericNormalizer implements Normalizer<AttributeValuePair> {
    Map<String, Float> defaultValues;

    public final static boolean normalizeIn0_1 = false;

    public final static Pattern NUMBER_REGEX = Pattern.compile("\\d+\\.\\d+|\\d+"); //"\\\\d+(\\\\.\\\\d+)?";  // "\\\\d+\\\\.?\\\\d+";
    
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
        Matcher matcher = NUMBER_REGEX.matcher(annotatedText);

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
        
        /*
        String[] tokens = annotatedText.split("\\s+");
        for (String token: tokens) {
            if (!NumberUtils.isNumber(token))
                continue;
            float v = Float.parseFloat(token);
            if (v > 0 && v < 100)
                return String.valueOf(v/100f);  // in [0,1]
            else if (v > 0 && v <= 1)
                return String.valueOf(v);
        }
        */

        // if not a number returning original text.  Calling method must handle if not a number.
        return annotatedText;
    }

    private String clean(String doubleString) {
        String res = doubleString;
        res = res.endsWith(".0") ? res.substring(0, res.length() - 2) : res;
        return res;
    }
}
