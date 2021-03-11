package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

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
public class Page implements com.ibm.drl.hbcp.parser.pdf.Page {

    @XStreamImplicit(itemFieldName="block")
    private List<Block> blocks = new ArrayList<>();

    public String getValue() {
        return StringUtils.join(blocks.stream().map(Block::getValue).collect(Collectors.toList()), "\n");
    }
}
