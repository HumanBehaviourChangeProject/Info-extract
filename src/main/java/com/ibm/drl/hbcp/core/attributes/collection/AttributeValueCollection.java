package com.ibm.drl.hbcp.core.attributes.collection;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Maps;
import com.google.common.collect.Multiset;
import com.google.common.collect.Multisets;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.jetbrains.annotations.NotNull;

import java.util.*;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A collection of attribute-value pairs, enabling all kinds of convenient querying and aggregation of these pairs.
 * An "attribute" is any concept of the behavior change ontology (specifically on smoking cessation), like
 * "Min age", "Max age", "Cigarettes per day", or the interventions.
 * The "value" is the value of the instance of this attribute, as could be found (annotated or extracted) in a science
 * article.
 * @param <E> the type of attribute-value pairs this collection contains
 *
 * @author marting
 */
public class AttributeValueCollection<E extends ArmifiedAttributeValuePair> implements Iterable<E> {

    // all the pairs as provided in the constructor
    private final Collection<? extends E> pairsOriginal;
    // pairs as a multiset
    protected final Multiset<E> pairs;
    // pairs indexed by attribute id
    private final Map<String, Multiset<E>> idToPairs;
    // pairs indexed by document
    private final Map<String, Multiset<E>> documentToPairs;
    // pairs indexed by document (armified version)
    private final Map<String, Map<String, Multiset<E>>> documentToArmifiedPairs;
    
    public AttributeValueCollection(Collection<? extends E> pairs) {
        this.pairsOriginal = pairs;
        this.pairs = Multisets.unmodifiableMultiset(ConcurrentHashMultiset.create(pairsOriginal));
        
        idToPairs = pairs.stream().collect(Collectors.groupingBy(pair -> pair.getAttribute().getId(), Collectors.toCollection(ConcurrentHashMultiset::create)));

        documentToPairs = new HashMap<>();
        for (E pair : pairs) {
            for (String docName : pair.getDocNames()) {
                documentToPairs.putIfAbsent(docName, ConcurrentHashMultiset.create());
                documentToPairs.get(docName).add(pair);
            }
        }
        documentToArmifiedPairs = Maps.toMap(documentToPairs.keySet(), document -> documentToPairs.get(document).stream()
                        .collect(Collectors.groupingBy(E::getArm, Collectors.toCollection(ConcurrentHashMultiset::create))));
        
    }

    public Multiset<E> getAllPairs() { return pairs; }

    public Multiset<E> getPairsOfId(String id) { return idToPairs.get(id); }

    public Multiset<E> getPairsInDoc(String docName) { return documentToPairs.get(docName); }

    public Map<String, Multiset<E>> getArmifiedPairsInDoc(String docName) { return documentToArmifiedPairs.get(docName); }

    public Set<String> getDocNames() { return documentToPairs.keySet(); }
    
    /**
     * For a document, maps each arm to sets of pairs keyed by attribute type, (like POPULATION, INTERVENTION, etc).
     * The pairs are only the attribute instances of this type.
     * This is not stored so the result of this method will be rebuilt each time.
     * @param docName The name of the document
     */
    public Map<String, Map<AttributeType, Multiset<E>>> getArmifiedPairsInDocSplitByType(String docName) {
        Map<String, Multiset<E>> armifiedPairs = documentToArmifiedPairs.get(docName);
        return Maps.toMap(armifiedPairs.keySet(), arm -> armifiedPairs.get(arm).stream()
                .collect(Collectors.groupingBy(pair -> pair.getAttribute().getType(), Collectors.toCollection(ConcurrentHashMultiset::create))));
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        return pairs.iterator();
    }

    public Set<String> getAllAttributeIds() { return idToPairs.keySet(); }

    /** Convert the type parameter of the generic collection: TODO: this is code smell */
    public static <V extends ArmifiedAttributeValuePair, U extends V> AttributeValueCollection<V> cast(AttributeValueCollection<U> collection) {
        return (AttributeValueCollection<V>)collection;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AttributeValueCollection<?> that = (AttributeValueCollection<?>) o;
        return Objects.equals(pairs, that.pairs);
    }

    @Override
    public int hashCode() {
        return Objects.hash(pairs);
    }
}
