package com.ibm.drl.hbcp.parser.pdf.reparsing.structure;

import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.Page;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

@Getter @Setter
public class ReparseDocument implements Document {
    private List<ReparseBlock> blocks;

    @Override
    public List<? extends Page> getPages() {
        List<Page> res = new ArrayList<>();
        SortedMap<Integer, List<ReparseBlock>> blocksPerPage = getBlocksPerPage();
        for (List<ReparseBlock> blocks : blocksPerPage.values()) {
            res.add(() -> blocks);
        }
        return res;
    }

    private SortedMap<Integer, List<ReparseBlock>> getBlocksPerPage() {
        SortedMap<Integer, List<ReparseBlock>> res = new TreeMap<>();
        for (ReparseBlock block : blocks) {
            res.putIfAbsent(block.getPage(), new ArrayList<>());
            res.get(block.getPage()).add(block);
        }
        return res;
    }
}
