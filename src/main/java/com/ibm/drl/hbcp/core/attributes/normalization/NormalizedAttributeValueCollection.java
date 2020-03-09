package com.ibm.drl.hbcp.core.attributes.normalization;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;

import java.util.Collection;
import java.util.stream.Collectors;

/**
 * A collection of normalized attribute-value pairs, enabling all kinds of convenient querying and aggregation,
 * but performing a normalization instead of keeping the original value.
 * @param <E> the type of original attribute-value pairs this collection builds from
 *
 * @author marting
 */
public class NormalizedAttributeValueCollection<E extends ArmifiedAttributeValuePair> extends AttributeValueCollection<NormalizedAttributeValuePair> {

    public NormalizedAttributeValueCollection(Normalizers normalizers, AttributeValueCollection<E> pairs) {
        super(normalizeAllValues(normalizers, pairs));
    }

    private static <E extends ArmifiedAttributeValuePair> Collection<NormalizedAttributeValuePair> normalizeAllValues(Normalizers normalizers, AttributeValueCollection<E> pairs) {
        return pairs.getAllAttributeIds().stream()
                .flatMap(id -> {
                    Multiset<E> values = pairs.getPairsOfId(id);
                    return normalizers.normalize(values).stream();
                })
                .collect(Collectors.toList());
    }
}
