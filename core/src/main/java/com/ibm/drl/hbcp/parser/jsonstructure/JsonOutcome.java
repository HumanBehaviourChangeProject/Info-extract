package com.ibm.drl.hbcp.parser.jsonstructure;

import lombok.Getter;
import lombok.Setter;

/**
 * The new Outcome JSON object produced by EPPI reviewer, and first used in Physical Activity annotations
 * @author mgleize
 */
@Getter @Setter
public class JsonOutcome {
    private String outcomeId; // probably useless
    private String outcomeText; // probably useless
    private int outcomeTypeId; // probably useless
    private String outcomeTypeName; // probably useless
    private String title;
    private String shortTitle; // docname (minus the trailing ".pdf")
    // information on timepoint
    private String itemTimepointDisplayValue; // value + unit (can be null :( )
    private String itemTimepointMetric; // unit
    private String itemTimepointValue; // value only
    // information on arms
    private int itemArmIdGrp1;
    private int itemArmIdGrp2;
    private String grp1ArmName;
    private String grp2ArmName;
    // context information: the table caption
    private String outcomeDescription;
    // data: N's
    private String data1; // group 1 N
    private String data1Desc;
    private String data2; // group 2 N
    private String data2Desc;
    // data: means
    private String data3; // group 1 mean
    private String data3Desc;
    private String data4; // group 2 mean
    private String data4Desc;
    // data: confidence intervals
    private String data5; // group 1 CI lower
    private String data5Desc;
    private String data6; // group 2 CI lower
    private String data6Desc;
    private String data7; // group 1 CI upper
    private String data7Desc;
    private String data8; // group 2 CI upper
    private String data8Desc;

    public boolean hasAnyOfImportantFieldsNull() {
        return data1 == null || data2 == null || data3 == null || data4 == null
                || itemTimepointDisplayValue == null;
    }

    public String getTimepointString() {
        return itemTimepointDisplayValue != null ? itemTimepointDisplayValue : itemTimepointValue;
    }

}
