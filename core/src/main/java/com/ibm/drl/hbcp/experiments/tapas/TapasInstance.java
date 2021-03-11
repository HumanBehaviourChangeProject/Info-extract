package com.ibm.drl.hbcp.experiments.tapas;

import lombok.Value;

import java.util.List;

@Value
public class TapasInstance {
    String docId;
    String tableId;
    TapasTable table;
    List<TapasQuestion> questions;
}
