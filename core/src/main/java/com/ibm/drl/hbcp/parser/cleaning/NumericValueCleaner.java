package com.ibm.drl.hbcp.parser.cleaning;

import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.util.ParsingUtils;

import java.util.List;

/**
 * Cleans numeric type attributes by restricting their value string to the first number it contains.
 *
 * @author marting
 */
public class NumericValueCleaner extends NumericTypeCleaner {

    public NumericValueCleaner(List<String> numericAttributeIds) {
        super(numericAttributeIds);
    }

    @Override
    protected AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair avp) {
        try {
            String originalValue = avp.getValue();
            String cleanedValue = ParsingUtils.parseFirstDoubleString(avp.getValue());
            return avp.withValue(ParsingUtils.parseFirstDoubleString(avp.getValue()));
        } catch (NumberFormatException e) {
            return avp;
        }
    }
}