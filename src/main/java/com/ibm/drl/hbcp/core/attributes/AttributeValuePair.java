package com.ibm.drl.hbcp.core.attributes;

import org.jetbrains.annotations.NotNull;

import java.util.Comparator;
import java.util.Objects;

/**
 * A pair of an attribute and a value (of an instance of that attribute).
 * Serves at the base class of every variation of such pair that has to be used in either in JSON parsing, extraction,
 * or prediction.
 *
 * @author marting
 */
public class AttributeValuePair implements Comparable<AttributeValuePair>{

    protected final Attribute attribute;
    protected final String value;

    public AttributeValuePair(Attribute attribute, String value) {
        this.attribute = attribute;
        this.value = value;
    }

    /** The ID of the attribute, as defined in the annotation scheme */
    public final Attribute getAttribute() { return attribute; }

    public final String getValue() { return value; }

    @Override
    public int hashCode() {
        return Objects.hash(attribute, value);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeValuePair that = (AttributeValuePair) o;
        return Objects.equals(attribute, that.attribute) &&
                Objects.equals(value, that.value);
    }

    @Override
    public String toString() {
        return attribute + ":" + value;
    }

    @Override
    public int compareTo(AttributeValuePair o) {
       return Comparator.<AttributeValuePair, Integer>comparing(avp -> avp.attribute.type.ordinal())
               .thenComparing(avp -> avp.value, Comparator.naturalOrder())
               .compare(this, o);
    }
}
