package com.ibm.drl.hbcp.parser;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;
import lombok.Getter;

import javax.json.Json;
import javax.json.JsonValue;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Repository to get information about the values of an attribute, specifically the value type, and default values if
 * available
 *
 * @author mgleize
 */
public class AttributeValues {

    private final SortedMap<Attribute, Values> attributeToValues;
    private final Set<Attribute> numericAttributes;
    private final Map<Attribute, List<String>> nominalAttributes; // and their values

    /**
     * Returns information about the values of a given attribute, null if said attribute wasn't found in the default JSON annotation file
     */
    public Values getValues(Attribute attribute) {
        return attributeToValues.get(attribute);
    }

    private AttributeValues(AttributeValueCollection<AnnotatedAttributeValuePair> annotations, Properties props) throws IOException {
        numericAttributes = new HashSet<>(annotations.getNumericAttributes());
        nominalAttributes = getNominalAttributes(Props.loadProperties());
        attributeToValues = getAttributeToValuesMap(annotations);
    }

    private SortedMap<Attribute, Values> getAttributeToValuesMap(AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        SortedMap<Attribute, Values> res = new TreeMap<>();
        // get all the attributes used in the dataset
        List<Attribute> attributes = avps.stream()
                .map(AttributeValuePair::getAttribute)
                .sorted()
                .distinct()
                .collect(Collectors.toList());
        // figure out their value information
        for (Attribute attribute : attributes) {
            Values values = getValues(attribute, avps);
            res.put(attribute, values);
        }
        return res;
    }

    protected Values getValues(Attribute attribute, AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        if (attribute.getType() == AttributeType.INTERVENTION) {
            return new Values(ValueType.BOOLEAN, getDefaultBooleanValue(attribute, avps));
        } else if (attribute.getName().startsWith("Outcome value") || numericAttributes.contains(attribute)) {
            return new Values(ValueType.NUMERIC, getDefaultNumericValue(attribute, avps));
        } else if (nominalAttributes.containsKey(attribute)) {
            List<String> categories = nominalAttributes.get(attribute);
            return new Values(ValueType.CATEGORY, getDefaultCategoryValue(attribute, categories, avps), categories);
        } else {
            return new Values(ValueType.TEXT, getDefaultTextValue(attribute, avps));
        }
    }

    private Map<Attribute, List<String>> getNominalAttributes(Properties props) {
        Set<String> nominalAttributeIds = Normalizers.getAttributeIdsFromPropertyPrefix(props, "prediction.categories.");
        Map<Attribute, List<String>> res = new HashMap<>();
        for (String id : nominalAttributeIds) {
            Attribute attribute = Attributes.get().getFromId(id);
            // if this is a valid attribute
            if (attribute != null) {
                String categoryNamesCSV = props.getProperty("prediction.categories." + id);
                List<String> values = Arrays.asList(categoryNamesCSV.split(","));
                res.put(attribute, values);
            }
        }
        return res;
    }

    private JsonValue getDefaultNumericValue(Attribute attribute, AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        return Json.createObjectBuilder().add("number", 0.0).build()
                .getJsonNumber("number");
    }

    private JsonValue getDefaultBooleanValue(Attribute attribute, AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        return JsonValue.FALSE;
    }

    private JsonValue getDefaultCategoryValue(Attribute attribute, List<String> values, AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        return Json.createObjectBuilder().add("s", values.get(0)).build()
                .getJsonString("s");
    }

    private JsonValue getDefaultTextValue(Attribute attribute, AttributeValueCollection<AnnotatedAttributeValuePair> avps) {
        return Json.createObjectBuilder().add("s", "").build()
                .getJsonString("s");
    }

    public static class Values {
        @Getter
        private final ValueType type;
        @Getter
        private final JsonValue defaultValue;
        @Getter
        private final List<String> values; // only relevant for category values

        public Values(ValueType type, JsonValue defaultValue, List<String> values) {
            this.type = type;
            this.defaultValue = defaultValue;
            this.values = values;
        }

        public Values(ValueType type, JsonValue defaultValue) {
            this(type, defaultValue, new ArrayList<>());
        }
    }

    public enum ValueType {
        NUMERIC, BOOLEAN, CATEGORY, TEXT
    }

    // implements the lazy-initialization thread-safe singleton pattern
    private static class LazyHolder {
        private static AttributeValues buildAttributeValues() {
            try {
                Properties props = Props.loadProperties();
                return new AttributeValues(
                        new JSONRefParser(FileUtils.potentiallyGetAsResource(new File(props.getProperty("ref.json")))).getAttributeValuePairs(),
                        props
                );
            } catch (IOException e) {
                e.printStackTrace();
                throw new RuntimeException("IOException when lazy-initializing singleton AttributeValues", e);
            }
        }
        private static final AttributeValues INSTANCE = buildAttributeValues();
    }

    /** Returns the Attributes collection for the JSON annotation file defined in the default properties */
    public static AttributeValues get() {
        return AttributeValues.LazyHolder.INSTANCE;
    }
}
