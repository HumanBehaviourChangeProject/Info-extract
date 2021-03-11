package com.ibm.drl.hbcp.predictor.api;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.parser.AttributeValues;
import lombok.Data;
import lombok.Getter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonValue;
import java.util.List;

/**
 * An Attribute object which also carries the type of the value (numeric, binary, etc...)
 * and which can be converted to JSON for use in the API.
 *
 * @see com.ibm.drl.hbcp.core.attributes.Attribute
 * @author marting
 */
public class AttributeInfo extends Attribute implements Jsonable {
    @Getter
    private final String shortStringType;
    @Getter
    private final AttributeValueType valueType;
    @Getter
    private final JsonValue defaultValue;
    @Getter
    private final boolean enabled;

    private static final Logger log = LoggerFactory.getLogger(AttributeInfo.class);

    public AttributeInfo(Attribute attribute, AttributeValueType valueType, JsonValue defaultValue, boolean enabled) {
        super(attribute.getId(), attribute.getType(), attribute.getName());
        shortStringType = attribute.getType().getShortString();
        this.valueType = valueType;
        this.defaultValue = defaultValue;
        this.enabled = enabled;
    }

    public AttributeInfo(Attribute attribute, AttributeValueType valueType, JsonValue defaultValue) {
        this(attribute, valueType, defaultValue, true);
    }

    public static AttributeInfo fromAttribute(Attribute attribute, boolean enabled) {
        AttributeValues.Values values = AttributeValues.get().getValues(attribute);
        if (values != null) {
            return new AttributeInfo(attribute,
                    AttributeValueType.fromValues(values),
                    values.getDefaultValue(),
                    enabled);
        } else {
            log.debug("No values for attribute: " + attribute);
            return new AttributeInfo(attribute,
                    new AttributeValueType(AttributeValues.ValueType.TEXT, Lists.newArrayList()),
                    JsonValue.NULL,
                    enabled);
        }
    }

    public static AttributeInfo fromAttribute(Attribute attribute) {
        return fromAttribute(attribute, true);
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("type", shortStringType)
                .add("name", name.trim())
                .add("enabled", enabled)
                .add("valueType", valueType.toJson())
                .add("defaultValue", defaultValue)
                .build();
    }

    @Data
    public static class AttributeValueType implements Jsonable {
        private final AttributeValues.ValueType valueType;
        private final List<String> values;

        public static AttributeValueType fromValues(AttributeValues.Values values) {
            return new AttributeValueType(values.getType(), values.getValues());
        }

        @Override
        public JsonValue toJson() {
            JsonArrayBuilder jsonValues = Json.createArrayBuilder();
            for (String value : values) jsonValues.add(value);
            return Json.createObjectBuilder()
                    .add("valueType", valueType.toString().toLowerCase())
                    .add("values", jsonValues.build())
                    .build();
        }
    }
}
