package com.ibm.drl.hbcp.parser.jsonstructure;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class JsonReference {

    private int itemId;
    private String title;
    private String shortTitle;
    private String Abstract;
    private JsonCode[] codes = new JsonCode[0]; // empty codes can happen when the document has no annotation
}
