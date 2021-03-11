package com.ibm.drl.hbcp.core.attributes;

import java.util.Objects;

/**
 * An attribute-value pair that has been extracted from a document (often a PDF),
 * either directly by humans and recovered in their annotations, or by automatic extraction.
 *
 * @author marting
 */
public class ExtractedAttributeValuePair extends AttributeValuePair {

    protected final String docName;

    public ExtractedAttributeValuePair(Attribute attribute, String value, String docName) {
        super(attribute, value);
        this.docName = docName;
    }

    public final String getDocName() { return docName; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExtractedAttributeValuePair that = (ExtractedAttributeValuePair) o;
        return Objects.equals(docName, that.docName);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), docName);
    }
}
