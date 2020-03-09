package com.ibm.drl.hbcp.parser.pdf.textract.structure;

import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class Block {

    private String blockType; // PAGE, LINE, TABLE, CELL, WORD, ...
    private double confidence;
    private String text;
    private int rowIndex;
    private int columnIndex;
    private int rowSpan;
    private int columnSpan;
    private Geometry geometry;
    private String id;
    private List<Relationship> relationships;
    private int page;

    @Override
    public String toString() {
        return text != null ? text : "";
    }
}
