package com.ibm.drl.hbcp.parser;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonAnnotationFile;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonAttribute;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonAttributes;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonCodeSet;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.ImmutableCollection;
import com.ibm.drl.hbcp.util.Props;

/**
 * A cache of Attribute objects, indexed both by ID and by name for easy retrieval in other tasks
 * (when only the ID or name is available).
 * Usually built using the Attribute's from a JsonRefParser, but can also be built standalone.
 *
 * @author marting
 */
public class Attributes implements ImmutableCollection<Attribute> {
    // attributes provided in the constructor stripped of duplicates
    private final Set<Attribute> attributes;
    // attributes indexed by ID
    private final Map<String, Attribute> idToAttribute;
    // attributes indexed by name (normalized)
    private final Map<String, List<Attribute>> nameToAttributes;
    // attributes grouped by type
    private final Map<AttributeType, Set<Attribute>> typeToAttributes;

    public Attributes(Collection<Attribute> attributes) {
        // store all the attributes with the same normalized name
        nameToAttributes = attributes.stream().collect(Collectors.toMap(a -> getNormalizedName(a.getName()), Lists::newArrayList, this::concatenateAndSort));
        // map each id to a representative attribute (that might not actually have this ID, it's the attribute of lowest ID with the same name)
        idToAttribute = attributes.stream().collect(Collectors.toMap(Attribute::getId, a -> getFromName(a.getName()), (a1, a2) -> a1));
        // this version of the original constructor argument is free of duplicates
        this.attributes = new HashSet<>(idToAttribute.values());
        typeToAttributes = Maps.toMap(this.attributes.stream().map(Attribute::getType).collect(Collectors.toSet()),
                type -> this.attributes.stream().filter(attr -> attr.getType() == type).collect(Collectors.toSet()));
    }

    private Attributes(File jsonFile) throws IOException {
        this(JSONRefParser.getJsonAnnotationFile(jsonFile));
    }

    Attributes(JsonAnnotationFile json) {
        this(getAttributes(json));
    }

    private static List<Attribute> getAttributes(JsonAnnotationFile json) {
        List<Attribute> res = new ArrayList<>();
        for (JsonCodeSet codeSet : json.getCodeSets()) {
            // retrieve potential attribute type (if no match, discard the codeset)
            AttributeType type = AttributeType.fromName(codeSet.getSetName());
            // build a fake Attribute representing the CodeSet
            Attribute codeSetAttribute = new Attribute("Set" + codeSet.getSetId(), type, codeSet.getSetName());
            List<Attribute> parents = Lists.newArrayList(codeSetAttribute);
            if (type != null) retrieveAttributes(codeSet.getAttributes(), parents, res);
        }
        return res;
    }

    private static void retrieveAttributes(JsonAttributes attributes, List<Attribute> parents, List<Attribute> res) {
        if (attributes != null) { // attributes is null if the node is a leaf node
            for (JsonAttribute attribute : attributes.getAttributesList()) {
                // build the attribute
                Attribute newAttribute = Attribute.newTypedWithParents(String.valueOf(attribute.getAttributeId()), attribute.getAttributeName(), parents);
                res.add(newAttribute);
                // update the parents
                List<Attribute> newParents = new ArrayList<>(parents);
                newParents.add(newAttribute);
                // fetch the children in the tree
                retrieveAttributes(attribute.getAttributes(), newParents, res);
            }
        }
    }

    /** A reasonable bijective normalization of the attribute name */
    private String getNormalizedName(String actualName) {
        return actualName.toLowerCase().replaceAll("[^0-9a-z]", "");
    }

    private <T extends Comparable<T>> List<T> concatenateAndSort(List<T> l1, List<T> l2) {
        List<T> res = new ArrayList<>();
        res.addAll(l1);
        res.addAll(l2);
        Collections.sort(res);
        return res;
    }

    // implements the lazy-initialization thread-safe singleton pattern
    private static class LazyHolder {
        private static Attributes buildAttributes() {
            try {
                // attributes from the annotations
                Attributes annotatedAttributes = new Attributes(FileUtils.potentiallyGetAsResource(new File(Props.loadProperties().getProperty("ref.json"))));
                // attributes that were not annotated and created at runtime instead
                Attributes res = NonAnnotatedAttributes.addTo(annotatedAttributes);
                return res;
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("IOException when lazy-initializing singleton Attributes", e);
            }
        }
        private static final Attributes INSTANCE = buildAttributes();
    }

    /** Returns the Attributes collection for the JSON annotation file defined in the default properties */
    public static Attributes get() {
        return LazyHolder.INSTANCE;
    }

    public Attribute getFromId(String id) { return idToAttribute.get(id); }

    public Attribute getFromName(@NotNull String name) {
        List<Attribute> res = getAllWithName(name);
        if (res == null) return null; // TODO: I don't like this :(
        return res.get(0);
    }

    public List<Attribute> getAllWithName(@NotNull String name) {
        String normalizedName = getNormalizedName(name);
        return nameToAttributes.get(normalizedName);
    }

    public Attribute getFromName(String... names) {
        for (String name : names) {
            Attribute res = getFromName(name);
            if (res != null)
                return res;
        }
        String attributeNames = StringUtils.join(names, "' or '");
        throw new RuntimeException("No attribute '" + attributeNames + "' in the JSON");
    }

    public Map<AttributeType, Set<Attribute>> groupedByType() { return typeToAttributes; }

    @Override
    public int size() {
        return attributes.size();
    }

    @Override
    public boolean contains(Object o) {
        if (o instanceof Attribute) {
            Attribute a = (Attribute)o;
            return getFromName(a.getName()) != null || getFromId(a.getId()) != null;
        } else return false;
    }

    @NotNull
    @Override
    public Iterator<Attribute> iterator() { return attributes.iterator(); }

    public Set<Attribute> getAttributeSet() { return attributes; }
}
