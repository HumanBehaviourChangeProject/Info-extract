package com.ibm.drl.hbcp.core.attributes.normalization;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.quantization.Quantizer;
import static com.ibm.drl.hbcp.core.attributes.quantization.Quantizer.getMinMax;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A collection of normalized attribute-value pairs, enabling all kinds of convenient querying and aggregation,
 * but performing a normalization instead of keeping the original value.
 * @param <E> the type of original attribute-value pairs this collection builds from
 *
 * @author marting
 */
public class NormalizedAttributeValueCollection<E extends ArmifiedAttributeValuePair> extends AttributeValueCollection<NormalizedAttributeValuePair> {
    
    public NormalizedAttributeValueCollection(Normalizers normalizers, AttributeValueCollection<E> pairs) {
        super(normalizeAllValues(normalizers, 0, pairs));
    }

    public NormalizedAttributeValueCollection(Normalizers normalizers, int numquanta, AttributeValueCollection<E> pairs) {
        super(normalizeAllValues(normalizers, numquanta, pairs));
    }

    private static <E extends ArmifiedAttributeValuePair> Collection<NormalizedAttributeValuePair> normalizeAllValues(
            Normalizers normalizers, int numquanta, AttributeValueCollection<E> pairs
    ) {
        return 
            pairs.getAllAttributeIds().stream()
                .flatMap(id -> {
                    Multiset<E> values = pairs.getPairsOfId(id);
                    List<NormalizedAttributeValuePair> normalizedValues = normalizers.normalize(values);
                    
                    if (numquanta > 0) {
                        Pair<Float, Float> minMax = getMinMax(normalizedValues);
                        normalizedValues = Quantizer.quantize(normalizedValues, numquanta, minMax);
                    }
                    return normalizedValues.stream();
                })
                .collect(Collectors.toList())
        ;
    }
}
