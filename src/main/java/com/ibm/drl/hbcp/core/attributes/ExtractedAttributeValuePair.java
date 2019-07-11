package com.ibm.drl.hbcp.core.attributes;

import com.google.common.collect.Sets;

import java.util.Objects;
import java.util.Set;

/**
 * An attribute-value pair that has been extracted from a document (often a PDF),
 * either directly by humans and recovered in their annotations, or by automatic extraction.
 *
 * @author marting
 */
public class ExtractedAttributeValuePair extends AttributeValuePair {

    protected final Set<String> docNames;

    public ExtractedAttributeValuePair(Attribute attribute, String value, Set<String> docNames) {
        super(attribute, value);
        this.docNames = docNames;
    }

    public ExtractedAttributeValuePair(Attribute attribute, String value, String docName) {
        this(attribute, value, Sets.newHashSet(docName));
    }
    /** Denoted "DocTitle" in the annotation JSONs, generally will be the filenames of the PDFs containing the attribute */
    public final Set<String> getDocNames() { return docNames; }

    // TODO remove that
    @Deprecated
    public final String getDocName() { return docNames.iterator().next(); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        ExtractedAttributeValuePair that = (ExtractedAttributeValuePair) o;
        return Objects.equals(docNames, that.docNames);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), docNames);
    }
}
