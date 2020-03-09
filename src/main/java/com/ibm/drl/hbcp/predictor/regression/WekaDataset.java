package com.ibm.drl.hbcp.predictor.regression;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.predictor.DataInstance;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Data;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;
import weka.core.DenseInstance;
import weka.core.Instance;
import weka.core.Instances;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * High-level representation of a Weka-compatible dataset. Specifies a header for each feature, with the value type
 * (among numeric, nominal and text). The outcome value is positioned at the end of the features.
 *
 * @author mgleize
 */
public class WekaDataset {

    @Getter
    private final List<AttributeHeader> headers;
    @Getter
    private final List<List<String>> trainData;
    @Getter
    private final List<List<String>> testData;

    private final Set<Attribute> numericAttributes;
    private final Map<Attribute, List<String>> nominalAttributes; // and their values
    private final List<Attribute> interventionAttributes;
    // rest is assumed to be text
    private final Map<Attribute, Integer> headerPosition;
    
    int numClasses;
    static List<String> labels;

    public WekaDataset(List<DataInstance> trainInstances, List<DataInstance> testInstances) throws IOException {
        this(trainInstances, testInstances, 0);
    }
    
    public WekaDataset(List<DataInstance> trainInstances, List<DataInstance> testInstances, int numClasses) throws IOException {
        this.numClasses = numClasses;
        
        List<DataInstance> allInstances = new ArrayList<>(trainInstances);
        allInstances.addAll(testInstances);
        numericAttributes = getNumericAttributes(allInstances);
        nominalAttributes = getNominalAttributes(Props.loadProperties());
        headers = getHeaders(allInstances);
        interventionAttributes = headers.stream()
                .map(AttributeHeader::getAttribute)
                .filter(a -> a.getType() == AttributeType.INTERVENTION)
                .collect(Collectors.toList());
        headerPosition = getHeaderPosition(headers);
        trainData = getRows(trainInstances);
        testData = getRows(testInstances);
    }

    static public List<String> getLabelsSet(int numClasses) {
        if (labels != null) return labels;
        labels = new ArrayList<>();
        for (int i=0; i < numClasses; i++)
            labels.add(String.valueOf(i));
        return labels;
    }
    
    private List<List<String>> getRows(List<DataInstance> instances) {
        return instances.stream()
                .map(this::getRow)
                .collect(Collectors.toList());
    }

    boolean useOVLabels() { return numClasses > 1; }
    
    String getLabel(String value) {
        float value_f = Float.parseFloat(value);
        float delta = 100/(float)numClasses;
        return String.valueOf((int)(value_f/delta));
    }
    
    private List<String> getRow(DataInstance instance) {
        String[] rawRow = new String[headers.size()];
        for (ArmifiedAttributeValuePair avp : instance) {
            if (!useOVLabels())
                rawRow[headerPosition.get(avp.getAttribute())] = avp.getValue();
            else {
                if (avp.getAttribute().getType() == AttributeType.OUTCOME_VALUE) {
                    rawRow[headerPosition.get(avp.getAttribute())] = getLabel(avp.getValue());
                }
            }
        }
        // override BCTs: a special type of nominal attributes. Presence is 1, absence is 0.
        for (Attribute bct : interventionAttributes) {
            rawRow[headerPosition.get(bct)] = rawRow[headerPosition.get(bct)] != null ? "1" : "0";
        }
        // convert to List, with "?" replacing null cells
        return Arrays.stream(rawRow)
                .map(value -> value != null ? value : "?")
                .collect(Collectors.toList())
        ;
    }

    private List<AttributeHeader> getHeaders(List<DataInstance> allInstances) {
        // get all the attributes used in the dataset
        List<Attribute> attributes = allInstances.stream()
                .flatMap(instance -> instance.getX().stream().map(AttributeValuePair::getAttribute))
                .distinct()
                .collect(Collectors.toList());
        Collections.sort(attributes);
        // get an instance of Outcome value attribute (should be unique)
        List<Attribute> outcomeValues = allInstances.stream()
                .map(instance -> instance.getY().getAttribute())
                .distinct()
                .collect(Collectors.toList());
        // add the outcome value at the very end
        if (!outcomeValues.isEmpty()) // this actually should never happen unless instances is empty
            attributes.add(outcomeValues.get(0));
        // figure out their value type
        return attributes.stream().map(this::getHeader).collect(Collectors.toList());
    }

    protected AttributeHeader getHeader(Attribute attribute) {
        AttributeHeader ah;
        if (attribute.getType() == AttributeType.INTERVENTION) {
            ah = new AttributeHeader(attribute, ValueType.NOMINAL, Lists.newArrayList("0", "1"));
        } else if (attribute.getName().startsWith("Outcome value") || numericAttributes.contains(attribute)) {
            ah = new AttributeHeader(attribute, ValueType.NUMERIC, null);
        } else if (nominalAttributes.containsKey(attribute)) {
            ah = new AttributeHeader(attribute, ValueType.NOMINAL, nominalAttributes.get(attribute));
        }
        else {
            ah = new AttributeHeader(attribute, ValueType.STRING, null);
        }
        
        if (attribute.getType() == AttributeType.OUTCOME_VALUE && useOVLabels()) {
            // outcome value a label in the arff
            return new AttributeHeader(attribute, ValueType.NOMINAL, getLabelsSet(numClasses));
        }
        return ah;
    }
    
    public boolean isNumericOrNominal(Attribute attribute) {
        return attribute.getType() == AttributeType.INTERVENTION ||
               numericAttributes.contains(attribute) ||
               nominalAttributes.containsKey(attribute); 
    }

    private Map<Attribute, Integer> getHeaderPosition(List<AttributeHeader> headers) {
        Map<Attribute, Integer> res = new HashMap<>();
        for (int i = 0; i < headers.size(); i++) {
            res.put(headers.get(i).getAttribute(), i);
        }
        return res;
    }

    private Set<Attribute> getNumericAttributes(List<DataInstance> allInstances) {
        AttributeValueCollection<ArmifiedAttributeValuePair> collection = new AttributeValueCollection<>(allInstances.stream()
                .flatMap(dataInstance -> Stream.concat(dataInstance.getX().stream(), Stream.of(dataInstance.getY())))
                .collect(Collectors.toList()));
        return new HashSet<>(collection.getNumericAttributes());
    }

    protected Map<Attribute, List<String>> getNominalAttributes(Properties props) {
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

    public enum ValueType {
        NUMERIC, NOMINAL, STRING
    }

    @Data
    public static class AttributeHeader {
        private final Attribute attribute;
        private final ValueType type;
        private final List<String> values; // only for NOMINAL value type, null otherwise


        /** To write as type in the header of an ARFF file */
        public String getValueTypeString() {
            switch (type) {
                case NUMERIC:
                    return "NUMERIC";
                case NOMINAL:
                    return "{" + StringUtils.join(values, ',') + "}";
                case STRING:
                    return "STRING";
                default:
                    throw new IllegalStateException("Unexpected value: " + type);
            }
        }
    }

    public static Instances buildWekaInstances(List<AttributeHeader> headers, List<List<String>> data) {
        // first build the attributes
        ArrayList<weka.core.Attribute> wekaAttributes = new ArrayList<>();
        for (AttributeHeader header : headers) {
            weka.core.Attribute attribute;
            switch (header.type) {
                case NUMERIC:
                    attribute = new weka.core.Attribute(header.getAttribute().getName());
                    break;
                case NOMINAL:
                    attribute = new weka.core.Attribute(header.getAttribute().getName(), header.getValues());
                    break;
                case STRING:
                    attribute = new weka.core.Attribute(header.getAttribute().getName(), (List<String>)null);
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + header.type);
            }
            wekaAttributes.add(attribute);
        }
        // initialize the dataset
        Instances instances = new Instances("", wekaAttributes, data.size());
        instances.setClassIndex(headers.size() - 1);
        // add each instance
        for (List<String> row : data) {
            Instance instance = new DenseInstance(row.size());
            int i = 0;
            for (weka.core.Attribute attribute : wekaAttributes) {
                String value = row.get(i++);
                if ("?".equals(value)) {
                    instance.setMissing(attribute);
                } else if (attribute.isNumeric()) {
                    try {
                        double numericValue = ParsingUtils.parseFirstDouble(value);
                        instance.setValue(attribute, numericValue);
                    } catch (NumberFormatException e) {
                        instance.setMissing(attribute);
                    }
                } else {
                    instance.setValue(attribute, value);
                }
            }
            instances.add(instance);
        }
        instances.deleteStringAttributes();
        return instances;
    }
}
