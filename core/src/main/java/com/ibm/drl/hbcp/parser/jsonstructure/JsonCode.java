package com.ibm.drl.hbcp.parser.jsonstructure;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class JsonCode {

    private int attributeId;
    private String additionalText;
    private int armId;
    private String armTitle;
    private JsonItemAttributeFullTextDetail[] itemAttributeFullTextDetails = new JsonItemAttributeFullTextDetail[0];
    private String sprintNo = ""; // "SprintNo" is an optional property in the JSONs
}
