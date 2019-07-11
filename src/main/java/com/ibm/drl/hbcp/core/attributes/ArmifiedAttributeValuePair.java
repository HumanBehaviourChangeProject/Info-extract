package com.ibm.drl.hbcp.core.attributes;

import java.util.Objects;
import java.util.Set;

import com.google.common.collect.Sets;

/**
 * An extracted attribute-value pair attached to an arm in a study.
 * @author marting
 */
public class ArmifiedAttributeValuePair extends ExtractedAttributeValuePair {

    protected final String arm;

    // marting: TODO: this is only used in the non-armified setting, and should be avoided if the arm is handled
    public static final String DUMMY_ARM = "DUMMY-ARM";

    public ArmifiedAttributeValuePair(Attribute attribute, String value, Set<String> docNames, String arm) {
        super(attribute, value, docNames);
        this.arm = arm;
    }

    public ArmifiedAttributeValuePair(Attribute attribute, String value, String docName, String arm) {
        this(attribute, value, Sets.newHashSet(docName), arm);
    }

    public final String getArm() { return arm; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ArmifiedAttributeValuePair that = (ArmifiedAttributeValuePair) o;
        return Objects.equals(arm, that.arm);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), arm);
    }
}
