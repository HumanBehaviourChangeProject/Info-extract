package com.ibm.drl.hbcp.predictor.api;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.parser.JSONRefParser;

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
    public final String shortStringType;
    public final AttributeValueType valueType;

    public AttributeInfo(String type, String id, String name, AttributeValueType valueType) {
        super(id, AttributeType.getFromShortString(type), name);
        shortStringType = type;
        this.valueType = valueType;
    }

    public AttributeInfo(String type, String id, String name) {
        this(type, id, name, AttributeValueType.defaultType(type));
    }

    public static String getType(int treeIndex) {
        return AttributeType.values()[treeIndex].getShortString();
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("id", id)
                .add("type", shortStringType)
                .add("name", name)
                .add("valueType", valueType.toJson())
                .build();
    }

    public enum ValueType { NUMERIC, CATEGORICAL, BINARY, TEXT };

    public static class AttributeValueType implements Jsonable {
        public final ValueType valueType;
        public final List<String> values;

        public AttributeValueType(ValueType type, List<String> values) {
            this.valueType = type;
            this.values = values;
        }

        public static AttributeValueType defaultType(String type) {
            if (type.equals("I")) {
                return new AttributeValueType(ValueType.BINARY, Lists.newArrayList());
            } else {
                return new AttributeValueType(ValueType.TEXT, Lists.newArrayList());
            }
        }

        @Override
        public JsonValue toJson() {
            JsonArrayBuilder jsonValues = Json.createArrayBuilder();
            for (String value : values) jsonValues.add(value);
            return Json.createObjectBuilder()
                    .add("valueType", valueType.toString())
                    .add("values", jsonValues.build())
                    .build();
        }
    }
}
