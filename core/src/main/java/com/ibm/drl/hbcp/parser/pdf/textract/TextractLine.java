package com.ibm.drl.hbcp.parser.pdf.textract;

import com.ibm.drl.hbcp.parser.pdf.Line;
import com.ibm.drl.hbcp.parser.pdf.textract.structure.Block;
import lombok.Getter;

public class TextractLine implements Line, TextractBoundingBoxed {

    private final String text;
    @Getter
    private final double left;
    @Getter
    private final double top;

    public TextractLine(Block lineBlock) {
        if (!lineBlock.getBlockType().equals("LINE")) throw new RuntimeException("Do not call this constructor on a non-LINE Textract Block.");
        text = lineBlock.getText();
        left = lineBlock.getGeometry().getBoundingBox().getLeft();
        top = lineBlock.getGeometry().getBoundingBox().getTop();
    }

    @Override
    public String getValue() {
        return text;
    }
}
