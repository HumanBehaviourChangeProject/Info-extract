package com.ibm.drl.hbcp.parser.pdf.abbyy.structure;

import com.thoughtworks.xstream.annotations.XStreamConverter;
import lombok.Getter;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@XStreamConverter(FormattingConverter.class)
public class Formatting {

    private String value;

    private List<String> characters = new ArrayList<>();

    public String getValue() {
        if (!characters.isEmpty()) {
            StringBuilder sb = new StringBuilder();
            for (String character : characters) {
                sb.append(character);
            }
            setValue(sb.toString());
            return value;
        } else {
            if (value == null) {
                value = "";
            }
            return value;
        }
    }
}
