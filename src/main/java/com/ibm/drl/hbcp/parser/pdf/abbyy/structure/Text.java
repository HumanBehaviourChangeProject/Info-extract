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
public class Text {

    @XStreamImplicit(itemFieldName="par")
    private List<Par> pars = new ArrayList<>();

    private String value = null;

    public String getValue() {
        if (value == null) {
            value = StringUtils.join(getPars().stream()
                    .map(par -> StringUtils.join(par.getLines().stream()
                            .map(line -> line.getValue().trim())
                            .collect(Collectors.toList()), " ")
                            // this removes the end-of-line hyphens (TODO: costly, could be done more efficiently at the Line level)
                            .replaceAll("¬[ \\n]", ""))
                    // add an extra end-of-line at the end of paragraphs
                    .collect(Collectors.toList()), "\n");
        }
        return value;
    }
}
