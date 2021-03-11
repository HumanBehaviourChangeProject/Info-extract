package com.ibm.drl.hbcp.parser.pdf;

import lombok.Data;

@Data
public class Cell {

    private final String value;
    private final int rowIndex;
    private final int columnIndex;
    private final int rowSpan;
    private final int columnSpan;
}
