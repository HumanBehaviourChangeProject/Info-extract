package com.ibm.drl.hbcp.parser.pdf.reparsing.structure;

import java.util.List;

import lombok.Getter;
import lombok.Setter;

@Getter @Setter
public class ReparseTableValue {
    private String value;
    private List<String> rowHeaders;
    private List<String> columnHeaders;
}
