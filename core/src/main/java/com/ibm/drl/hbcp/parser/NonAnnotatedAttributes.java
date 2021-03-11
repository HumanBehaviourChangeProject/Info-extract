package com.ibm.drl.hbcp.parser;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.w3c.dom.Attr;

import java.awt.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Describes attributes/entities not annotated in the standard annotations, hence with no extractable definition from a
 * JSON file. Their ID will be re-assigned automatically based on free unused IDs.
 *
 * @author mgleize
 */
public class NonAnnotatedAttributes {

    public static final Attribute ContinuousAbstinence = new Attribute("will be overriden", AttributeType.OUTCOME, "Continuous abstinence");
    public static final Attribute PointPrevalentAbstinence = new Attribute("", AttributeType.OUTCOME, "Point-prevalent abstinence");

    public static final List<Attribute> ALL = Lists.newArrayList(
            ContinuousAbstinence, PointPrevalentAbstinence
    );

    public static Attributes addTo(Attributes attributes) {
        List<Attribute> nonAnnonatedAttributes = new ArrayList<>();
        // reassign ids
        int largestUsedId = getLargestUsedID(attributes);
        for (Attribute nonAnnotatedAttribute : ALL) {
            // copy attribute but with valid ID
            nonAnnonatedAttributes.add(new Attribute(String.valueOf(++largestUsedId),
                    nonAnnotatedAttribute.getType(), nonAnnotatedAttribute.getName()));
        }
        // add the new attributes to the existing ones
        List<Attribute> allFinalAttributes = new ArrayList<>(attributes);
        allFinalAttributes.addAll(nonAnnonatedAttributes);
        return new Attributes(allFinalAttributes);
    }

    private static int getLargestUsedID(Collection<Attribute> attributes) {
        int res = Integer.MIN_VALUE;
        for (Attribute attribute : attributes) {
            try {
                int id = Integer.parseInt(attribute.getId());
                if (id > res) {
                    res = id;
                }
            } catch (NumberFormatException e) {
                // don't update on non-numerical ids
            }
        }
        return res;
    }
}
