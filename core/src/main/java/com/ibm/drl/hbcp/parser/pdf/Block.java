package com.ibm.drl.hbcp.parser.pdf;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;

/**
 * Either a sequence of lines of text, or a table.
 *
 * @author mgleize
 */
public interface Block extends Element {

    enum Type {
        LINES,
        TABLE,
        OTHER
    }

    Type getType();

    List<? extends Line> getLines();

    List<TableValue> getTable();

    static List<Cell> getAllCells(Cell[][] table) {
        return Arrays.stream(table)
                .flatMap(Arrays::stream)
                .distinct()
                .collect(Collectors.toList());
    }

    static Block fromText(String text) {
        return new Block() {
            public String getValue() { return text; }
            public Type getType() { return Type.LINES; }
            public List<? extends Line> getLines() { return Lists.newArrayList(() -> text); }
            public List<TableValue> getTable() { return null; }
        };
    }
}
