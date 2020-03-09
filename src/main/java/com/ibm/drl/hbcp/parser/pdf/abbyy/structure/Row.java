package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @ToString
public class Row {

    @XStreamImplicit(itemFieldName="cell")
    private List<Cell> cells = new ArrayList<>();

    public String getValue() {
        return StringUtils.join(cells.stream().map(Cell::getValue).collect(Collectors.toList()), " ");
    }
}
