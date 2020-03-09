package com.ibm.drl.hbcp.parser.cleaning;

import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Applies a cleaning operation on the original output of the JSONRefParser.
 *
 * @author marting
 */
public interface Cleaner {

    /**
     * Cleans a collection of AAVPs
     * @param original AAVPs that are either the direct output of the JSONRefParser, or of other Cleaners
     * @return a list of the AAVPs with cleaned context and/or value
     */
    List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original);

    default AttributeValueCollection<AnnotatedAttributeValuePair> getCleaned(AttributeValueCollection<AnnotatedAttributeValuePair> original) {
        return new AttributeValueCollection<>(clean(original));
    }

    /**
     * Return the values that changed compared to their originals
     * @param changed a collection of values which has gone through some transformation (potentially only some of the AVPs were affected)
     * @param original the original collection of values
     * @return the elements in {@code changed} that are not contained in the {@code original}
     */
    static Set<AnnotatedAttributeValuePair> delta(Collection<AnnotatedAttributeValuePair> changed, Collection<AnnotatedAttributeValuePair> original) {
        Set<AnnotatedAttributeValuePair> set2 = new HashSet<>(changed);
        set2.removeAll(original);
        return set2;
    }
}
