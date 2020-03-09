package com.ibm.drl.hbcp.parser.pdf.textract;

import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Cell;
import com.ibm.drl.hbcp.parser.pdf.Line;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.List;
import java.util.stream.Collectors;

public abstract class TextractBlock implements Block {

    @Getter
    protected final Type type;
    protected final List<TextractLine> lines;
    protected final List<Cell> cells;

    protected TextractBlock(Type type, List<TextractLine> lines, List<Cell> cells) {
        this.type = type;
        this.lines = lines;
        this.cells = cells;
    }

    @Override
    public String getValue() {
        switch (type) {
            case LINES:
                return StringUtils.join(lines.stream().map(Line::getValue).collect(Collectors.toList()), " ");
            case TABLE:
                return "[TABLE]";
        }
        throw new RuntimeException("I don't know why Java is not able to find that the previous switch is complete.");
    }
}
