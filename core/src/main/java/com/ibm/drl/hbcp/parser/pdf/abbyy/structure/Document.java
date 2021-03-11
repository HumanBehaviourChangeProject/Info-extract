package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.thoughtworks.xstream.annotations.XStreamAlias;
import com.thoughtworks.xstream.annotations.XStreamImplicit;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Getter @Setter @ToString
@XStreamAlias("document")
public class Document implements com.ibm.drl.hbcp.parser.pdf.Document {

    private String value = null;

    @XStreamImplicit(itemFieldName="page")
    private List<Page> pages = new ArrayList<>();

    public String getValue() {
        if (value == null) {
            value = StringUtils.join(pages.stream().map(Page::getValue).collect(Collectors.toList()), '\n');
        }
        return value;
    }
}
