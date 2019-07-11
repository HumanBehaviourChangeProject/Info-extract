package com.ibm.drl.hbcp.core.attributes;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * The type/category of the attribute, representing the kind of information it gives on the study and how it
 * relates to the other elements of the study.
 *
 * @author marting
 */
public enum AttributeType {

    POPULATION("Population", "C", 0),
    INTERVENTION("Intervention", "I", 1),
    OUTCOME("Outcome", "O", 2),
    OUTCOME_VALUE("Outcome_value", "V", 3),
    EFFECT("Effect", "E", 4);

    private final String shortString;
    private final String name;
    private final int number;

    AttributeType(String name, String shortString, int number) {
        this.name = name;
        this.shortString = shortString;
        this.number = number;
    }

    /**
     * Returns an AttributeType from its one-letter code.
     * @throws IllegalArgumentException if the provided code doesn't correspond to any actual AttributeType.
     */
    public static AttributeType getFromShortString(String shortString) throws IllegalArgumentException {
        List<AttributeType> withShortString = Arrays.stream(AttributeType.values())
                .filter(at -> at.shortString.equals(shortString))
                .collect(Collectors.toList());
        if (withShortString.size() != 1) throw new IllegalArgumentException("Wrong short string for AttributeType: " + shortString);
        return withShortString.get(0);
    }

    public int code() { return number; }

    public String getName() { return name; }

    public String getShortString() { return shortString; }

    @Override
    public String toString() { return name; }
}
