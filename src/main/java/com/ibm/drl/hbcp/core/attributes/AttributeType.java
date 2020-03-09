package com.ibm.drl.hbcp.core.attributes;

import com.google.common.collect.Lists;

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

    POPULATION("Population", "C", 0,
            Lists.newArrayList("Population")),
    SETTING("Setting", "S", 1,
            Lists.newArrayList("Setting")),
    INTERVENTION("Intervention", "I", 2,
            Lists.newArrayList("Intervention")),
    OUTCOME("Outcome", "O", 3,
            Lists.newArrayList("Outcome (behaviour)")),
    OUTCOME_VALUE("Outcome_value", "V", 4,
            Lists.newArrayList("Outcome (behaviour) value")),
    EFFECT("Effect", "E", 5,
            Lists.newArrayList("Effect")),
    SUPPLEMENT_INFO("Supplement", "Supplement", 6,
            Lists.newArrayList("Supplementary information")),
    ARM("Arm", "A", 7,
            Lists.newArrayList("Arms")),
    NONE("NO_TYPE", "N", 8,
            Lists.newArrayList("New Prioritised Codeset")), // when a match hasn't been found
    MODES_OF_DELIVERY("Modes of Delivery", "M", 9,
            Lists.newArrayList("Modes of Delivery")),
    SOURCE("Source", "SO", 10,
            Lists.newArrayList("Source")),
    REACH("Reach", "R", 11,
            Lists.newArrayList("Reach")),
    SCHEDULE("Schedule", "SC", 12,
            Lists.newArrayList("Schedule")),
    DOSE("Dose", "D", 13,
            Lists.newArrayList("Dose")),
    OUTCOME_DATA_EXTRACTION("OutcomeDataExtraction", "VV", 14,
            Lists.newArrayList("OutcomeDataExtraction"));

    private final String shortString;
    private final String name;
    private final int number;
    private final List<String> possibleNames;

    AttributeType(String name, String shortString, int number, List<String> possibleNames) {
        this.name = name;
        this.shortString = shortString;
        this.number = number;
        this.possibleNames = possibleNames;
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

    public static AttributeType fromName(String name) throws IllegalArgumentException {
        List<AttributeType> withMatchingName = Arrays.stream(AttributeType.values())
                .filter(at -> at.possibleNames.stream().anyMatch(possibleName -> possibleName.equals(name)))
                .collect(Collectors.toList());
        if (withMatchingName.size() > 1)
            throw new IllegalArgumentException("Too many matching AttributeType for: " + name);
        return withMatchingName.size() > 0 ? withMatchingName.get(0) : NONE;
    }

    public static AttributeType[] getSprint1234Types() {
        return new AttributeType[] { POPULATION, INTERVENTION, OUTCOME, OUTCOME_VALUE, EFFECT };
    }

    public int code() { return number; }

    public String getName() { return name; }

    public String getShortString() { return shortString; }

    @Override
    public String toString() { return name; }
}
