package com.ibm.drl.hbcp.parser;

import java.util.Objects;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;

/**
 * An attribute-value pair with all the information that could be gathered from the JSON annotations.
 * including value, docname, arm, context, highlighted text, etc...
 * @author marting
 */
public class AnnotatedAttributeValuePair extends ArmifiedAttributeValuePair {

    protected final String context;
    protected final String highlightedText;
    protected final String sprintNo;
    protected final int annotationPage;

    public AnnotatedAttributeValuePair(Attribute attribute, String value, String docName, String arm,
                                       String context, String highlightedText, String sprintNo, int annotationPage) {
        super(attribute, value, docName, arm);
        this.context = context;
        this.highlightedText = highlightedText;
        this.sprintNo = sprintNo;
        this.annotationPage = annotationPage;
    }

    public String getContext() { return context; }

    public String getHighlightedText() { return highlightedText; }

    public String getSprintNo() { return sprintNo; }

    public int getAnnotationPage() { return annotationPage; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;
        AnnotatedAttributeValuePair that = (AnnotatedAttributeValuePair) o;
        return annotationPage == that.annotationPage &&
                Objects.equals(context, that.context) &&
                Objects.equals(highlightedText, that.highlightedText) &&
                Objects.equals(sprintNo, that.sprintNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), context, highlightedText, sprintNo, annotationPage);
    }

    @Override
    public String toString() {
        // value and hightlighted text should be the same
        return "{attributeId:" + attribute.getId() + ", attributeName:" + attribute.getName() + ", value:" + value + ", context:" + context 
                + ", docName:" + getDocName() + ", annotationPage:" + annotationPage + ", sprintNo:" + sprintNo + ", arm:" + arm + "}";
    }
}
