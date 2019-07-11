package com.ibm.drl.hbcp.parser;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import org.jetbrains.annotations.NotNull;

import java.util.Collection;
import java.util.Iterator;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * A cache of Attribute objects, indexed both by ID and by name for easy retrieval in other tasks
 * (when only the ID or name is available).
 * Usually built using the Attribute's from a JsonRefParser.
 *
 * @author marting
 */
public class AttributeCache implements Iterable<Attribute> {
    // attributes provided in the constructor
    private final Collection<Attribute> attributes;
    // attributes indexed by ID
    private final Map<String, Attribute> idToAttribute;
    // attributes indexed by name
    private final Map<String, Attribute> nameToAttribute;

    public AttributeCache(Collection<Attribute> attributes) {
        this.attributes = attributes;
        idToAttribute = attributes.stream().collect(Collectors.toMap(Attribute::getId, a -> a, (a1, a2) -> a1));
        nameToAttribute = attributes.stream().collect(Collectors.toMap(Attribute::getName, a -> a, (a1, a2) -> a1));
    }

    public Attribute getFromId(String id) { return idToAttribute.get(id); }

    public Attribute getFromName(String name) { return nameToAttribute.get(name); }

    @NotNull
    @Override
    public Iterator<Attribute> iterator() { return attributes.iterator(); }
}
