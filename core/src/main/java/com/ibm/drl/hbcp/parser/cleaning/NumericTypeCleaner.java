package com.ibm.drl.hbcp.parser.cleaning;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Generic cleaner only applied to values that are detected as numeric.
 *
 * @author marting
 */
public abstract class NumericTypeCleaner implements Cleaner {

    private final Set<Attribute> numericAttributes;

    NumericTypeCleaner(List<String> numericAttributeIds) {
        this.numericAttributes = numericAttributeIds.stream().map(id -> Attributes.get().getFromId(id)).collect(Collectors.toSet());
    }

    @Override
    public List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original) {
        // also automatically detect extra numeric attributes here
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = new AttributeValueCollection<>(original);
        Set<Attribute> extraNumericAttributes = getAutomaticallyDetectedNumericAttributeIds(avps);
        numericAttributes.addAll(extraNumericAttributes);
        // clean every numeric-type value
        return avps.getAllPairs().stream()
                .map(aavp -> numericAttributes.contains(aavp.getAttribute()) ? clean(aavp) : aavp)
                .collect(Collectors.toList());
    }

    protected abstract AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair numericTypeAvp);

    private Set<Attribute> getAutomaticallyDetectedNumericAttributeIds(AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        return new HashSet<>(avps.getNumericAttributes());
    }

}
