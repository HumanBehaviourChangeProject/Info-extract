package com.ibm.drl.hbcp.parser.pdf.textract;

import com.ibm.drl.hbcp.parser.pdf.Line;
import com.ibm.drl.hbcp.parser.pdf.TableValue;

import java.util.List;

public class TextractLines extends TextractBlock {

    public TextractLines(List<TextractLine> lines) {
        super(Type.LINES, lines, null);
    }

    @Override
    public List<? extends Line> getLines() {
        return lines;
    }

    @Override
    public List<TableValue> getTable() {
        return null;
    }
}
