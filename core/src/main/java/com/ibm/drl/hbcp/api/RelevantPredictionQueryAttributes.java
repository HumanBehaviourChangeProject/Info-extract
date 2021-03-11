package com.ibm.drl.hbcp.api;

import com.google.common.collect.ImmutableSortedSet;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.predictor.api.AttributeInfo;

import java.util.*;
import java.util.stream.Collectors;

/**
 * A static repository of the attributes that are valid query options. Intended to be used mostly by the API to feed
 * as constraints to the frontend.
 *
 * @author mgleize
 */
public class RelevantPredictionQueryAttributes {

    // TODO: put here the valid query options for Population
    private static final List<String> POPULATION_ATTRIBUTE_NAMES = Lists.newArrayList(
        "Mean age",
            "Proportion achieved secondary education",
            "Proportion achieved university or college education",
            "Proportion identifying as female gender",
            "Proportion identifying as male gender",
            "Proportion never used tobacco"
    );

    private static final Set<Attribute> ALL = buildAttributeSet();

    /** Returns the full set of relevant query options */
    public static Set<Attribute> get() {
        return ALL;
    }

    /** Returns the relevant query options of the provided AttributeType */
    public static Set<Attribute> getForType(AttributeType type) {
        return ALL.stream().filter(a -> a.getType() == type).collect(Collectors.toSet());
    }

    private static Set<Attribute> buildAttributeSet() {
        Set<Attribute> res = new HashSet<>();
        // add a selection of population attributes
        res.addAll(getAttributesFromNames(POPULATION_ATTRIBUTE_NAMES));
        // add all BCTs
        res.addAll(Attributes.get().groupedByType().get(AttributeType.INTERVENTION));
        // add all Outcome attributes also (just for the purpose of the demo but they shouldn't be selected)
        res.addAll(Attributes.get().groupedByType().get(AttributeType.OUTCOME));
        return ImmutableSortedSet.copyOf(res);
    }

    private static Set<Attribute> getAttributesFromNames(Collection<String> attributeNames) {
        return attributeNames.stream()
                // look up the name
                .map(Attributes.get()::getFromName)
                // remove the "null" introduced when the attribute wasn't found with the provided name
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
    }

    public static Comparator<AttributeInfo> AttributeInfoComparatorForPredictionDemo = Comparator.comparing((AttributeInfo ai) -> !ai.isEnabled())
            .thenComparing((AttributeInfo ai) -> ai.getName());

}
