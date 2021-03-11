package com.ibm.drl.hbcp.experiments.tapas;

import lombok.Value;

import java.util.List;

@Value
public class TapasQuestion {
    String id;
    String text;
    List<TapasAnswer> answers;
}
