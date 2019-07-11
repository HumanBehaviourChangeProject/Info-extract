package com.ibm.drl.hbcp.core.attributes;

import java.util.Objects;

/**
 * An attribute-value pair with its textual context (often the surrounding text in the document).
 *
 * @author marting
 */
public class ContextualizedAttributeValuePair extends ArmifiedAttributeValuePair {

    protected final String context;

    public ContextualizedAttributeValuePair(Attribute attribute, String value, String docName, String arm, String context) {
        super(attribute, value, docName, arm);
        this.context = context;
    }

    public final String getContext() { return context; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ContextualizedAttributeValuePair that = (ContextualizedAttributeValuePair) o;
        return Objects.equals(context, that.context);
    }

    @Override
    public int hashCode() {

        return Objects.hash(super.hashCode(), context);
    }
}
