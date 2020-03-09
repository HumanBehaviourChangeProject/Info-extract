package com.ibm.drl.hbcp.parser.pdf.reparsing.structure;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Line;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import lombok.Getter;
import lombok.Setter;

import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter
public class ReparseBlock implements Block {

    @JsonAlias({"type"})
    private String typeInJson;
    private String value;
    private List<ReparseTableValue> cells;
    private int page;

    @Override
    public Type getType() {
        return Type.valueOf(typeInJson);
    }

    @Override
    public List<? extends Line> getLines() {
        return Lists.newArrayList(() -> value);
    }

    @Override
    public List<TableValue> getTable() {
        return cells.stream()
                .map(tv -> new TableValue(tv.getValue(), tv.getRowHeaders(), tv.getColumnHeaders(), this))
                .collect(Collectors.toList());
    }
}
