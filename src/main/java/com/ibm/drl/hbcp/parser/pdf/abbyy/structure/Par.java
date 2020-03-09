package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamAsAttribute;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@ToString
public class Par {

    @XStreamImplicit(itemFieldName="line")
    private List<Line> lines = new ArrayList<>();

    @XStreamAlias("align")
    @XStreamAsAttribute
    private String align;
}
