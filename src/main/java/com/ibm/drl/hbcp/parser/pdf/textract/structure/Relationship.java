package com.ibm.drl.hbcp.parser.pdf.textract.structure;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Relationship {
    private String type; // CHILD
    private List<String> ids;
}
