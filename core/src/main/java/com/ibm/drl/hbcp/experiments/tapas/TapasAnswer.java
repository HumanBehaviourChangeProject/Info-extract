package com.ibm.drl.hbcp.experiments.tapas;

import lombok.Value;

@Value
public class TapasAnswer {
    String text;
    int rowIndex;
    int columnIndex;
}
