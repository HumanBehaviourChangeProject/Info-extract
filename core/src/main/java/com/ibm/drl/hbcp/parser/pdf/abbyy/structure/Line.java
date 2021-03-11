package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class Line implements com.ibm.drl.hbcp.parser.pdf.Line {

    /* the <formatting> tag might contain interesting information, but right now I've confirmed that
     * <line> always has exactly one <formatting> and its String value is the most important element*/
    @XStreamAlias("formatting")
    private Formatting formatting;

    @Override
    public String toString() {
        return "Line(\"" + formatting.getValue() + "\")";
    }

    @Override
    public String getValue() { return formatting.getValue(); }
}
