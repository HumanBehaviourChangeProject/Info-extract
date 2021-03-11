package com.ibm.drl.hbcp.parser.pdf.textract;

import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Page;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class TextractPage implements Page {

    @Getter
    List<TextractBlock> blocks;

    public TextractPage(List<TextractLine> lines, List<TextractTable> tables) {
        // order the lines / tables
        List<TextractBoundingBoxed> elements = new ArrayList<>();
        elements.addAll(lines);
        elements.addAll(tables);
        Collections.sort(elements);
        // unpack the elements into "group of lines", and "tables"
        blocks = mergeLinesAndUnpack(elements);
    }

    private List<TextractBlock> mergeLinesAndUnpack(List<TextractBoundingBoxed> elements) {
        List<TextractBlock> res = new ArrayList<>();
        List<TextractLine> lines = new ArrayList<>();
        for (TextractBoundingBoxed elt : elements) {
            if (elt instanceof TextractLine) {
                lines.add((TextractLine)elt);
            } else {
                // it's a table, first flush the line
                flushLines(lines, res);
                // then just add the table
                res.add((TextractTable)elt);
            }
        }
        flushLines(lines, res);
        return res;
    }

    private void flushLines(List<TextractLine> lines, List<TextractBlock> res) {
        if (!lines.isEmpty()) {
            TextractLines block = new TextractLines(new ArrayList<>(lines));
            res.add(block);
            lines.clear();
        }
    }

    @Override
    public String getValue() {
        return StringUtils.join(blocks.stream().map(Block::getValue).collect(Collectors.toList()), '\n');
    }
}
