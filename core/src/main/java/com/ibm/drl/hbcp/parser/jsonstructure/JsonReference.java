package com.ibm.drl.hbcp.parser.jsonstructure;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class JsonReference {

    private int itemId;
    private String title;
    private String shortTitle;
    private String Abstract;
    // empty codes can happen when the document has no annotation
    private JsonCode[] codes = new JsonCode[0];
    // exclusively used in physical activity, not smoking cessation
    private JsonOutcome[] outcomes = new JsonOutcome[0];
}
