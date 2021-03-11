package com.ibm.drl.hbcp.predictor.api;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@NoArgsConstructor
@Getter @Setter
public class AttributeValue {
    private String id;
    private String type;
    private Object value;

    public AttributeValuePair toAvp() {
        Attribute attribute = Attributes.get().getFromId(id);
        String value = type.equalsIgnoreCase("boolean") ? "1" : this.value.toString();
        return new AttributeValuePair(attribute, value);
    }
}
