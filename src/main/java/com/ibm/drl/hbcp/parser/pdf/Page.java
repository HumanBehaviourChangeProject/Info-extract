package com.ibm.drl.hbcp.parser.pdf;

import com.google.common.collect.Lists;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public interface Page extends Element {

    List<? extends Block> getBlocks();

    @Override
    default String getValue() {
        return getBlocks().stream().map(Block::getValue).collect(Collectors.joining("\n\n"));
    }
}
