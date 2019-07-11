package com.ibm.drl.hbcp.core.attributes;

import java.util.Objects;

/**
 * Describes an attribute, like "Min Age". It has, among other things, an ID, a name, a type.
 * @author marting
 */
public class Attribute {
    protected final String id;
    protected final AttributeType type;
    protected final String name;

    public Attribute(String id, AttributeType type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    public final String getId() { return id; }

    public final AttributeType getType() { return type; }

    public final String getName() { return name; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return Objects.equals(id, attribute.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.valueOf(id) + "(" + name + ")";
    }
}
