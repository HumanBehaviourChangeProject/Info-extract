package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.ibm.drl.hbcp.parser.pdf.Line;
import com.ibm.drl.hbcp.parser.pdf.TableToText;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyTable;
import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @ToString
public class Block implements com.ibm.drl.hbcp.parser.pdf.Block {

    @XStreamAlias("blockType")
    @XStreamAsAttribute
    private String blockType = "";

    // This is used when blockType is "Text", null otherwise (also usually only 1 Text)
    @XStreamImplicit(itemFieldName="text")
    private List<Text> texts = new ArrayList<>();

    // This is used when blockType is "Table", null otherwise
    @XStreamImplicit(itemFieldName="row")
    private List<Row> rows = new ArrayList<>();

    private List<TableValue> tableValues = null;

    public Block() { }

    public String getValue() {
        if ("Table".equals(blockType)) {
            return StringUtils.join(rows.stream().map(Row::getValue).collect(Collectors.toList()), '\n');
        } else {
            return StringUtils.join(texts.stream().map(Text::getValue).collect(Collectors.toList()), ' ');
        }
    }

    @Override
    public Type getType() {
        switch (blockType) {
            case "Table":
                return Type.TABLE;
            case "Text":
                return Type.LINES;
            default:
                return Type.OTHER;
        }
    }

    /** Get all the lines in the texts */
    @Override
    public List<? extends Line> getLines() {
        return getTexts().stream()
                .flatMap(t -> t.getPars().stream())
                .flatMap(p -> p.getLines().stream())
                .collect(Collectors.toList());
    }

    @Override
    public List<TableValue> getTable() {
        if (tableValues == null) {
            tableValues = new TableToText(getTableInMatrixForm(), this).getValueCellsAndHeaders();
        }
        return tableValues;
    }

    private com.ibm.drl.hbcp.parser.pdf.Cell[][] getTableInMatrixForm() {
        if (getType() != Type.TABLE)
            return null;
        Cell[][] table = new AbbyyTable(this).getMatrixForm();
        com.ibm.drl.hbcp.parser.pdf.Cell[][] res = new com.ibm.drl.hbcp.parser.pdf.Cell[table.length][];
        for (int i = 0; i < table.length; i++) {
            res[i] = new com.ibm.drl.hbcp.parser.pdf.Cell[table[i].length];
            for (int j = 0; j < table[i].length; j++) {
                res[i][j] = new com.ibm.drl.hbcp.parser.pdf.Cell(
                        table[i][j].getValue(),
                        i, //TODO this is probably incorrect, but I'm not sure it even matters anyway
                        j, //TODO this is probably incorrect, but I'm not sure it even matters anyway
                        table[i][j].getRowSpan(),
                        table[i][j].getColSpan()
                );
            }
        }
        return res;
    }
}
