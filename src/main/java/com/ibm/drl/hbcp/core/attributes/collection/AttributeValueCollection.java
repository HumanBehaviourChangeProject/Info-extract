package com.ibm.drl.hbcp.core.attributes.collection;

import com.google.common.collect.*;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.ImmutableCollection;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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
public class AttributeValueCollection<E extends ArmifiedAttributeValuePair> implements ImmutableCollection<E> {

    // pairs as a multiset
    protected final Multiset<E> pairs;
    // pairs indexed by attribute id
    private final Map<String, Multiset<E>> idToPairs;
    // pairs indexed by document
    private final Map<String, Multiset<E>> documentToPairs;
    // pairs indexed by document (armified version)
    private final Map<String, Map<Arm, Multiset<E>>> documentToArmifiedPairs;

    // attributes indexed by their id
    private final Map<String, Attribute> idToAttribute;

    private static final double MINIMUM_NUMERIC_VALUE_AMOUNT = 0.80; // attributes will be considered of numeric type if 80% of their values contain a number

    public AttributeValueCollection(Collection<? extends E> pairs) {
        this.pairs = Multisets.unmodifiableMultiset(ConcurrentHashMultiset.create(pairs));
        
        idToPairs = this.pairs.stream().collect(Collectors.groupingBy(pair -> pair.getAttribute().getId(), Collectors.toCollection(ConcurrentHashMultiset::create)));

        documentToPairs = this.pairs.stream().collect(Collectors.groupingBy(ArmifiedAttributeValuePair::getDocName, Collectors.toCollection(ConcurrentHashMultiset::create)));
        documentToArmifiedPairs = Maps.toMap(documentToPairs.keySet(), document -> documentToPairs.get(document).stream()
                        .collect(Collectors.groupingBy(E::getArm, Collectors.toCollection(ConcurrentHashMultiset::create))));
        idToAttribute = Maps.toMap(idToPairs.keySet(), id -> idToPairs.get(id).iterator().next().getAttribute());
    }

    public static <E extends ArmifiedAttributeValuePair> AttributeValueCollection<E> union(AttributeValueCollection<E>... collections) {
        return new AttributeValueCollection<>(Arrays.stream(collections).flatMap(c -> c.getAllPairs().stream()).collect(Collectors.toList()));
    }

    public Multiset<E> getAllPairs() { return pairs; }

    public Map<String, Multiset<E>> byId() { return idToPairs; }

    public Multiset<E> getPairsOfId(String id) { return idToPairs.get(id); }

    public Map<String, Multiset<E>> byDoc() { return documentToPairs; }

    public Multiset<E> getPairsInDoc(String docName) { return documentToPairs.get(docName); }

    public Map<Arm, Multiset<E>> getArmifiedPairsInDoc(String docName) { return documentToArmifiedPairs.get(docName); }

    public Set<String> getDocNames() { return documentToPairs.keySet(); }
    
    /**
     * For a document, maps each arm to sets of pairs keyed by attribute type, (like POPULATION, INTERVENTION, etc).
     * The pairs are only the attribute instances of this type.
     * This is not stored so the result of this method will be rebuilt each time.
     * @param docName The name of the document
     */
    public Map<Arm, Map<AttributeType, Multiset<E>>> getArmifiedPairsInDocSplitByType(String docName) {
        Map<Arm, Multiset<E>> armifiedPairs = documentToArmifiedPairs.get(docName);
        return Maps.toMap(armifiedPairs.keySet(), arm -> armifiedPairs.get(arm).stream()
                .collect(Collectors.groupingBy(pair -> pair.getAttribute().getType(), Collectors.toCollection(ConcurrentHashMultiset::create))));
    }

    /** Returns another collection consisting only of the AVPs whose docName belongs to the list passed as argument */
    public AttributeValueCollection<E> filterByDocs(List<String> docNamesToKeep) {
        Set<String> docs = new HashSet<>(docNamesToKeep);
        return new AttributeValueCollection<>(this.stream()
                        .filter(avp -> docs.contains(avp.getDocName()))
                .collect(Collectors.toList()));
    }

    @Override
    @NotNull
    public Iterator<E> iterator() {
        return pairs.iterator();
    }

    @Override
    public int size() { return pairs.size(); }

    @Override
    public boolean contains(@Nullable Object o) { return pairs.contains(o); }

    public Set<String> getAllAttributeIds() { return idToPairs.keySet(); }

    public Map<String, Attribute> getAttributesById() { return idToAttribute; }

    /** Convert the type parameter of the generic collection: TODO: this is code smell */
    public static <V extends ArmifiedAttributeValuePair, U extends V> AttributeValueCollection<V> cast(AttributeValueCollection<U> collection) {
        return (AttributeValueCollection<V>)collection;
    }

    /** Return a new collection with the values in the empty arm distributed in each of the other arms, and the empty arm removed */
    public AttributeValueCollection<E> distributeEmptyArm() {
        List<E> res = new ArrayList<>();
        for (String doc : documentToArmifiedPairs.keySet()) {
            // get the non-empty arms
            Map<Arm, Multiset<E>> armToValues = documentToArmifiedPairs.get(doc);
            Set<Arm> nonEmptyArms = armToValues.keySet().stream().filter(arm -> !arm.isEmptyArm()).collect(Collectors.toSet());
            // add the current values in the non-empty arms first
            for (Arm arm : nonEmptyArms)
                res.addAll(armToValues.get(arm));
            // copy the values from the empty arm to each of the other arms
            for (E valueInEmptyArm : armToValues.getOrDefault(Arm.EMPTY, HashMultiset.create())) {
                for (Arm arm : nonEmptyArms) {
                    // TODO: this is unchecked, for example if a ContextualizedAVP didn't implement its own withArm, it would fail
                    res.add((E)valueInEmptyArm.withArm(arm));
                }
            }
        }
        return new AttributeValueCollection<>(res);
    }

    public List<Attribute> getNumericAttributes() {
        Set<String> ids = getAllAttributeIds();
        return getAllAttributeIds().stream()
                .map(getAttributesById()::get)
                .filter(attribute -> attribute.getType() != AttributeType.INTERVENTION)
                .filter(attribute -> attribute.getType() != AttributeType.ARM)
                .filter(attribute -> {
                    Multiset<E> values = byId().get(attribute.getId());
                    long numericValueCount = values.stream()
                            .filter(avp -> ParsingUtils.parseAllNumberLines(avp.getValue()).size() == 1)
                            .count();
                    return (double)numericValueCount / values.size() >= MINIMUM_NUMERIC_VALUE_AMOUNT;
                })
                .collect(Collectors.toList());
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

    public static void main(String[] args) throws IOException {
        // print all the numeric attributes
        JSONRefParser parser = new JSONRefParser(new File(Props.loadProperties().getProperty("ref.json")));
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = parser.getAttributeValuePairs();
        List<Attribute> numericAttributes = avps.getNumericAttributes();
        for (Attribute attribute : numericAttributes) {
            System.out.println(attribute);
        }
        System.out.println(numericAttributes.size());
    }
}
