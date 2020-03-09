package com.ibm.drl.hbcp.parser.jsonstructure;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class JsonCodeSet {

    private String setName;
    private int setId;
    private JsonAttributes attributes;
}
