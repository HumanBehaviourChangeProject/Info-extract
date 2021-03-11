package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.google.common.collect.Lists;
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

@Getter
@Setter
@ToString
public class Cell {

    // This is used when blockType is "Text"
    @XStreamImplicit(itemFieldName="text")
    private List<Text> texts = new ArrayList<>();

    @XStreamAlias("rowSpan")
    @XStreamAsAttribute
    private String rowSpanString;

    @XStreamAlias("colSpan")
    @XStreamAsAttribute
    private String colSpanString;

    public String getValue() {
        return StringUtils.join(texts.stream().map(Text::getValue).collect(Collectors.toList()), "\n");
    }

    public int getRowSpan() {
        return rowSpanString != null ? Integer.parseInt(rowSpanString) : 1;
    }

    public int getColSpan() {
        return colSpanString != null ? Integer.parseInt(colSpanString) : 1;
    }

    public static Cell createCell(String text, int rowSpan, int colSpan) {
        Formatting f = new Formatting();
        f.setValue(text);
        Line l = new Line();
        l.setFormatting(f);
        Par p = new Par();
        p.setLines(Lists.newArrayList(l));
        Text t = new Text();
        t.setPars(Lists.newArrayList(p));
        Cell res = new Cell();
        res.setTexts(Lists.newArrayList(t));
        res.setRowSpanString(String.valueOf(rowSpan));
        res.setColSpanString(String.valueOf(colSpan));
        return res;
    }
}
