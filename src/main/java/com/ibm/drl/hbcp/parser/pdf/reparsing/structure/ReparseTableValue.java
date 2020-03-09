package com.ibm.drl.hbcp.parser.pdf.reparsing.structure;

import com.ibm.drl.hbcp.parser.pdf.TableValue;
import lombok.Getter;
import lombok.Setter;

import java.util.List;

@Getter @Setter
public class ReparseTableValue {
    private String value;
    private List<String> rowHeaders;
    private List<String> columnHeaders;
}
