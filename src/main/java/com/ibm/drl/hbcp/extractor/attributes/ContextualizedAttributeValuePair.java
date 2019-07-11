/**
 * 
 */
package com.ibm.drl.hbcp.extractor.attributes;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

/**
 * @author charlesj
 *
 */
public class ContextualizedAttributeValuePair extends AttributeValuePair {

    protected final String context;
    
    /**
     * Attribute-value pair with added context that is needed for extraction
     * 
     * @param attribute
     * @param value
     * @param context 
     */
    public ContextualizedAttributeValuePair(Attribute attribute, String value, String context) {
        super(attribute, value);
        this.context = context;
    }

    public String getContext() {
        return context;
    }
}
