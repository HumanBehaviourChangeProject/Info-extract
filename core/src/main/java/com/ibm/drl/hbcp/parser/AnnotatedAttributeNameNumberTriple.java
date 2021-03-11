package com.ibm.drl.hbcp.parser;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Comparator;
import java.util.List;
import java.util.Set;

public class AnnotatedAttributeNameNumberTriple extends AnnotatedAttributeValuePair {

    @Getter
    private final String valueName;
    private final String valueNumber;

    public static final Set<String> ATTRIBUTE_NAMES = Sets.newHashSet(
            "Proportion identifying as belonging to a specific ethnic group",
            "Aggregate relationship status",
            "Proportion belonging to specified family or household income category",
            "Proportion belonging to specified individual income category",
            "Nicotine dependence",
            "Individual reasons for attrition"
//            "Encountered Intervention"
    );

    public AnnotatedAttributeNameNumberTriple(Attribute attribute, String valueName, String valueNumber, String docName, Arm arm,
                                              String context, String sprintNo, int annotationPage) {
        super(attribute, getWellOrderedNameNumberPair(valueName, valueNumber).getValue(),
                docName, arm, context, getCombinedValue(valueName, valueNumber), sprintNo, annotationPage);
        Pair<String, String> wellOrderedStrings = getWellOrderedNameNumberPair(valueName, valueNumber);
        this.valueName = wellOrderedStrings.getKey();
        this.valueNumber = wellOrderedStrings.getValue();
    }

    @Override
    public AnnotatedAttributeNameNumberTriple withValue(String value) {
        return new AnnotatedAttributeNameNumberTriple(attribute, valueName, value, docName, arm, context, sprintNo, annotationPage);
    }

    @Override
    public AnnotatedAttributeNameNumberTriple withContext(String context) {
        return new AnnotatedAttributeNameNumberTriple(attribute, valueName, valueNumber, docName, arm, context, sprintNo, annotationPage);
    }

    @Override
    public AnnotatedAttributeNameNumberTriple withArm(Arm arm) {
        return new AnnotatedAttributeNameNumberTriple(attribute, valueName, valueNumber, docName, arm, context, sprintNo, annotationPage);
    }

    private static String getCombinedValue(String valueName, String valueNumber) {
        return getWellOrderedNameNumberPair(valueName, valueNumber).toString();
    }

    /** It's possible that name and number were switched up during annotation, this will re-order them correctly */
    private static Pair<String, String> getWellOrderedNameNumberPair(String providedName, String providedNumber) {
        List<String> allStrings = Lists.newArrayList(providedName, providedNumber);
        allStrings.sort(Comparator.comparingDouble(AnnotatedAttributeNameNumberTriple::digitFraction));
        return Pair.of(allStrings.get(0), allStrings.get(1));
    }

    private static double digitFraction(String s) {
        int digitCount = 0;
        for (char c : s.toCharArray()) {
            digitCount += Character.isDigit(c) ? 1 : 0;
        }
        // don't change the provided order if empty strings
        return s.length() == 0 ? 0.0 : (double)digitCount / s.length();
    }

    public String getValueNumber() {
        return getValue();
    }

    @Override
    public String getSingleLineValue() {
        return Pair.of(normalizeWhitespace(valueName), normalizeWhitespace(valueNumber)).toString();
    }
}
