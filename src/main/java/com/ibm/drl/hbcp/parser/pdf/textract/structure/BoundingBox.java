package com.ibm.drl.hbcp.parser.pdf.textract.structure;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class BoundingBox {
    private double width;
    private double height;
    private double left;
    private double top;
}
