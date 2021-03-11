package com.ibm.drl.hbcp.parser.jsonstructure;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class JsonAttribute {

    private String attributeName;
    private int attributeSetId;
    private int attributeId;
    private JsonAttributes attributes;
}
