package com.ibm.drl.hbcp.parser.pdf;

import com.google.common.collect.Lists;

import java.util.List;

public class SimpleTextBlock implements Block {

    private final String text;

    public SimpleTextBlock(String text) {
        this.text = text;
    }

    @Override
    public Type getType() { return Type.LINES; }

    @Override
    public List<? extends Line> getLines() {
        return Lists.newArrayList(() -> text);
    }

    @Override
    public List<TableValue> getTable() {
        return null;
    }

    @Override
    public String getValue() {
        return text;
    }
}
