package com.ibm.drl.hbcp.parser.pdf.textract;

import com.ibm.drl.hbcp.parser.pdf.Cell;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.Page;
import com.ibm.drl.hbcp.parser.pdf.textract.structure.Block;
import com.ibm.drl.hbcp.parser.pdf.textract.structure.Relationship;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

public class TextractDocument implements Document {

    private String value = null;

    private final Map<String, Cell> idToCell;
    private final Map<String, TextractTable> idToTable;
    private final Map<String, TextractLine> idToLine;
    @Getter
    private final List<TextractPage> pages;

    public TextractDocument(com.ibm.drl.hbcp.parser.pdf.textract.structure.Document jsonDocument) {
        // index every word by its ID
        Map<String, Block> idToWord = jsonDocument.getBlocks().stream()
                .filter(block -> block.getBlockType().equals("WORD"))
                .collect(Collectors.toMap(Block::getId, b -> b, (l, r) -> r));
        // index every cell by its ID
        idToCell = jsonDocument.getBlocks().stream()
                .filter(block -> block.getBlockType().equals("CELL"))
                .collect(Collectors.toMap(Block::getId,
                        b -> new Cell(
                                getChildren(b, idToWord).stream()
                                    .map(Block::getText)
                                    .collect(Collectors.joining(" ")),
                                b.getRowIndex(),
                                b.getColumnIndex(),
                                b.getRowSpan(),
                                b.getColumnSpan()
                        ),
                        (l, r) -> l));
        // index every table by its ID
        idToTable = jsonDocument.getBlocks().stream()
                .filter(block -> block.getBlockType().equals("TABLE"))
                .collect(Collectors.toMap(Block::getId,
                        b -> new TextractTable(b, getChildren(b, idToCell)),
                        (l, r) -> l));
        // index every line by its ID
        idToLine = jsonDocument.getBlocks().stream()
                .filter(block -> block.getBlockType().equals("LINE"))
                .collect(Collectors.toMap(Block::getId,
                        TextractLine::new,
                        (l, r) -> l));
        // build the pages
        pages = jsonDocument.getBlocks().stream()
                .filter(block -> block.getBlockType().equals("PAGE"))
                .map(b -> new TextractPage(getChildren(b, idToLine), getChildren(b, idToTable)))
                .collect(Collectors.toList());
    }

    private <E> List<E> getChildren(Block block, Map<String, E> idToElements) {
        List<Relationship> relationships = block.getRelationships() != null ? block.getRelationships() : new ArrayList<>();
        return relationships.stream()
                .filter(rel -> rel.getType().equals("CHILD"))
                .flatMap(rel -> rel.getIds().stream())
                .map(idToElements::get)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    @Override
    public String getValue() {
        if (value == null) {
            value = StringUtils.join(pages.stream().map(Page::getValue).collect(Collectors.toList()), '\n');
        }
        return value;
    }
}
