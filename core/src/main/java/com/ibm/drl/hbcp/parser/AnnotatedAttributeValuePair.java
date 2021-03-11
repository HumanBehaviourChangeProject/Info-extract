package com.ibm.drl.hbcp.parser;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;

import java.util.Objects;

/**
 * An attribute-value pair with all the information that could be gathered from the JSON annotations.
 * including value, docname, arm, context, highlighted text, etc...
 * @author marting
 */
public class AnnotatedAttributeValuePair extends ArmifiedAttributeValuePair {

    protected final String highlightedText;
    protected final String sprintNo;
    protected final int annotationPage;

    public AnnotatedAttributeValuePair(Attribute attribute, String value, String docName, String arm,
                                       String context, String highlightedText, String sprintNo, int annotationPage) {
        super(attribute, value, docName, arm, context);
        // TODO: careful, this doesn't pass "annotationPage" to the super constructor
        this.highlightedText = highlightedText;
        this.sprintNo = sprintNo;
        this.annotationPage = annotationPage;
    }

    public AnnotatedAttributeValuePair(Attribute attribute, String value, String docName, Arm arm,
                                       String context, String highlightedText, String sprintNo, int annotationPage) {
        super(attribute, value, docName, arm, context, String.valueOf(annotationPage));
        this.highlightedText = highlightedText;
        this.sprintNo = sprintNo;
        this.annotationPage = annotationPage;
    }
    
    public AnnotatedAttributeValuePair withArm(Arm arm) {
        return new AnnotatedAttributeValuePair(attribute, value, getDocName(), arm,
                context, highlightedText, sprintNo, annotationPage);
    }

    public AnnotatedAttributeValuePair withValue(String value) {
        return new AnnotatedAttributeValuePair(attribute, value, getDocName(), arm,
                context, highlightedText, sprintNo, annotationPage);
    }

    public AnnotatedAttributeValuePair withContext(String context) {
        return new AnnotatedAttributeValuePair(attribute, value, getDocName(), arm,
                context, highlightedText, sprintNo, annotationPage);
    }

    public AnnotatedAttributeValuePair withAttribute(Attribute attribute) {
        return new AnnotatedAttributeValuePair(attribute, value, getDocName(), arm,
                context, highlightedText, sprintNo, annotationPage);
    }
    
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
                Objects.equals(highlightedText, that.highlightedText) &&
                Objects.equals(sprintNo, that.sprintNo);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), highlightedText, sprintNo, annotationPage);
    }

    public boolean isNameNumberPair() {
        return this instanceof AnnotatedAttributeNameNumberTriple;
    }

    @Override
    public String toString() {
        // value and hightlighted text should be the same
        return "{attributeId:" + attribute.getId() + ", attributeName:" + attribute.getName() + ", value:" + getSingleLineValue() + ", context:" + context
                + ", docName:" + getDocName() + ", annotationPage:" + annotationPage + ", sprintNo:" + sprintNo + ", arm:" + arm + "}";
    }

    protected String normalizeWhitespace(String s) { return s.replaceAll("\\s", " "); }

    public String getSingleLineValue() {
        return normalizeWhitespace(value);
    }
}
