package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Default normalizer for regular numeric values.
 * The normalized value is the first number in the string value of the attribute.
 * @author dganguly
 */
public class NumericNormalizer implements Normalizer<AttributeValuePair> {

    public final static boolean normalizeIn0_1 = false;

    public final static Pattern NUMBER_REGEX = Pattern.compile("\\d+\\.\\d+|\\d+"); //"\\\\d+(\\\\.\\\\d+)?";  // "\\\\d+\\\\.?\\\\d+";

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

        return annotatedText;
    }

    private String clean(String doubleString) {
        String res = doubleString;
        res = res.endsWith(".0") ? res.substring(0, res.length() - 2) : res;
        return res;
    }
}
