package com.ibm.drl.hbcp.core.attributes;

import com.drew.lang.StringUtil;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.util.ParsingUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.w3c.dom.Attr;

import javax.json.Json;
import javax.json.JsonObjectBuilder;
import javax.json.JsonValue;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Describes an attribute, like "Min Age". It has, among other things, an ID, a name, a type.
 * @author marting
 */
public class Attribute implements Jsonable, Comparable<Attribute> {
    protected final String id;
    protected final AttributeType type;
    protected final String name;

    private static final int VERY_SHORT_NAME_MAX_LENGTH = 4;

    public Attribute(String id, AttributeType type, String name) {
        this.id = id;
        this.type = type;
        this.name = name;
    }

    public static Attribute newTypedWithParents(String id, String name, List<Attribute> parents) {
        return new Attribute(id, getRealType(parents, name), name);
    }

    private static AttributeType getRealType(List<Attribute> parents, String name) {
        // first check on the attributes with a non-"new prioritized" type
        Optional<Attribute> firstAttributeWithRealType = parents.stream()
                .filter(parent -> parent.getType() != AttributeType.NONE)
                .findFirst();
        if (firstAttributeWithRealType.isPresent()) {
            return firstAttributeWithRealType.get().getType();
        } else {
            // use the name of the current attribute
            return AttributeType.fromName(name);
        }
    }

    public final String getId() { return id; }

    public final AttributeType getType() { return type; }

    public final String getName() { return name; }

    public final String getNameForLabel() { return name.replaceAll("[^0-9a-zA-Z]", "_"); }

    public final boolean isOutcomeValue() { return "Outcome value".toLowerCase().equals(name.toLowerCase()); }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        Attribute attribute = (Attribute) o;
        return Objects.equals(id, attribute.id);
    }

    public String getVeryShortName() {
        String name = StringUtils.capitalize(this.name.trim());
        if (name.length() <= VERY_SHORT_NAME_MAX_LENGTH) {
            return name;
        } else {
            // fix the name (only applies to "1.1.Goal setting" :D)
            if (name.startsWith("1.1.G")) name = "1.1 G" + name.substring("1.1.G".length());
            // take the first token
            int firstSpace = name.indexOf(" ");
            if (0 <= firstSpace && firstSpace <= VERY_SHORT_NAME_MAX_LENGTH) {
                return name.substring(0, firstSpace);
            } else {
                // return the first 2 initials or letters of the name
                String alphaName = name.replaceAll("[^a-zA-Z]+", " ");
                alphaName = alphaName.trim();
                String[] splits = alphaName.split(" +");
                if (splits.length < 2) {
                    return name.substring(0, 2);
                } else {
                    return ("" + splits[0].charAt(0) + splits[1].charAt(0)).toUpperCase();
                }
            }
        }
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    @Override
    public String toString() {
        return String.valueOf(id) + "(" + name + ")";
    }

    @Override
    public JsonValue toJson() {
        JsonObjectBuilder builder = Json.createObjectBuilder()
                .add("id", id)
                .add("type", type.toString())
                .add("name", name);
        if (type == AttributeType.INTERVENTION) {
            builder.add("shortName", getVeryShortName());
        }
        return builder.build();
    }

    @Override
    public int compareTo(@NotNull Attribute attribute) {
        return Double.compare(ParsingUtils.parseFirstDouble(getId()), ParsingUtils.parseFirstDouble(attribute.getId()));
    }
}
