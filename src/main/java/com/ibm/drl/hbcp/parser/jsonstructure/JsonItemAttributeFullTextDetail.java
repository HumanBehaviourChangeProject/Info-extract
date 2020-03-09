package com.ibm.drl.hbcp.parser.jsonstructure;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class JsonItemAttributeFullTextDetail {

    private int itemDocumentId;
    private String text;
    private boolean isFromPdf;
    private String docTitle;
    private String itemArm;
}
