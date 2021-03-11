package com.ibm.drl.hbcp.predictor.graph;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Arrays;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * An attribute-value pair used as a node in graphs passed to Node2Vec.
 *
 * It provides a unique string identifier of the form:
 * [type]:[id]:[value]
 * where type is in ["C", "I", "O", "V", "E"]
 * id is a String, generally a big arbitrary number without human-readable meaning (has to be matched to the JSON codeset)
 * value is the specific value carried by the node instance for this attribute: can be a String, a number, about anything...
 *
 * This string representation of a node is to be written to a file
 * which is then going to be used to 'interpret' a node, e.g.
 * I:3673271:1 is 'interpreted' to mean that this is a node of type I, 1.1 is the BCT code and 1 is the value (presence of the BCT).
 *
 * @author marting, dganguly, charlesj
 */

public class AttributeValueNode extends AttributeValuePair {

    private final AttributeValuePair attribValue;
    
    public static final String DELIMITER = ":";
    private static final int COMPONENT_COUNT = 3;
    private static final Pattern NODE_ID_REGEX = buildIdValidityRegex();

    protected static final Logger LOGGER = LoggerFactory.getLogger(AttributeValueNode.class);

    public AttributeValueNode(AttributeValuePair avp) {
        super(avp.getAttribute(), normalizeValue(avp.getValue()));
        this.attribValue = avp;
    }
    
    /** The original attribute-value pair carried by this AttributeValueNode */
    public AttributeValuePair getOriginal() {
        return attribValue;
    }

    public static String normalizeValue(String value) {
        String res = value;
        res = res.replaceAll("\\s+", "_");
        return res;
    }
    
    /**
     * Parses a raw node identifier (making it easy to obtain each component of the id)
     * @return AttributeValueNode object if parsing succeeds, null if not
     */
    public static AttributeValueNode parse(String identifier) {
        if (!isValidNodeId(identifier)) {
            // marting: do not bury exceptions under null pointers
            // return null;
            throw new RuntimeException("Wrong format for node identifier: " + identifier);
        }
        String[] splits = identifier.split(DELIMITER, COMPONENT_COUNT);
        String attributeTypeShortString = splits[0];
        AttributeType type = AttributeType.getFromShortString(attributeTypeShortString);
        String attributeId = splits[1];
        String value = splits[2];
        // try to recover the real Attribute
        Attribute attribute = Attributes.get().getFromId(attributeId);
        if (attribute == null) {
            attribute = new Attribute(attributeId, type, "");
        }
        AttributeValuePair avp = new AttributeValuePair(attribute, value);
        return new AttributeValueNode(avp);
    }

    private static boolean isValidNodeId(String text) {
        return NODE_ID_REGEX.matcher(text).matches();
    }

    public boolean isNumeric() {
        return getNumericValue() != null;
    }

    public Double getNumericValue() {
        try {
            return Double.parseDouble(this.attribValue.getValue());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    /** Shortcut method: ID means the ID of the attribute */
    public String getId() { return getAttribute().getId(); }

    public static String create(String attributeName, Object value) {
        Attribute attribute = Attributes.get().getFromName(attributeName);
        if (attribute == null) throw new RuntimeException("Attribute not found: " + attributeName);
        AttributeValuePair avp = new AttributeValuePair(attribute, value.toString());
        return new AttributeValueNode(avp).toString();
    }

    private static Pattern buildIdValidityRegex() {
        String attributeTypeCheck = StringUtils.join(Arrays.stream(AttributeType.values()).map(AttributeType::getShortString).collect(Collectors.toList()), '|');
        return Pattern.compile("(?:" + attributeTypeCheck + ")" + DELIMITER + "[0-9]+" + DELIMITER + "[^\\t]*");
    }

    @Override
    public String toString() {
        return getAttribute().getType().getShortString()
                + DELIMITER
                + getAttribute().getId()
                + DELIMITER
                + getValue();
    }
    
    public String getStringWOValue() {
        return getAttribute().getType().getShortString()
                + DELIMITER
                + getAttribute().getId()
                + DELIMITER
        ;        
    }
}
