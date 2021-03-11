package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.Attributes;

import java.util.*;

public abstract class Metric {

    protected final static Comparator<List<Double>> FIRST_COMPONENT_COMPARE = Comparator.comparingDouble(o -> o.get(0));

    public Map<Attribute, List<Double>> evaluate(AttributeValueCollection<? extends ArmifiedAttributeValuePair> extracted,
                                                 AttributeValueCollection<? extends ArmifiedAttributeValuePair> reference) {
        Map<Attribute, List<Double>> res = new TreeMap<>();
        // need all docs in reference or union of extracted + reference?
        Set<String> referenceDocNames = new HashSet<>(reference.getDocNames());
        //referenceDocNames.addAll(extracted.getDocNames());
        // only evaluate on attributes seen in the reference, rest would be considered "ignored" (e.g. in the annotation)
        for (String attributeId : reference.getAllAttributeIds()) {
            // get a standard, representative Attribute of the same ID
            Attribute attribute = Attributes.get().getFromId(attributeId);
            if (isRelevantAttribute(attribute)) {
                // if this standardized ID wasn't used in the extracted collection, try the first ID
                Collection<? extends ArmifiedAttributeValuePair> extractedCollection = extracted.byId().get(attribute.getId());
                if (extractedCollection == null) {
                    extractedCollection = extracted.byId().get(attributeId);
                }
                if (extractedCollection != null) { // TODO: check that == null doesn't happen too often
                    AttributeValueCollection<? extends ArmifiedAttributeValuePair> extractedValues = new AttributeValueCollection<>(extractedCollection);
                    AttributeValueCollection<? extends ArmifiedAttributeValuePair> referenceValues = new AttributeValueCollection<>(reference.byId().get(attributeId));
                    // evaluate the metric on this attribute
                    List<Double> evaluationResult = evaluate(attribute, extractedValues, referenceValues, referenceDocNames);
                    res.put(attribute, evaluationResult);
                }
            }
        }
        return res;
    }

    /**
     * Returns a list of evaluation metrics computed on the extracted values of a given attributes, compared to a reference set of values
     * (for the same attributes). The two collections are not separated across documents, this is something the implementer
     * has to do inside this method if needed to compute the metrics.
     */
    public abstract List<Double> evaluate(Attribute attribute, AttributeValueCollection<? extends ArmifiedAttributeValuePair> extracted,
                                          AttributeValueCollection<? extends ArmifiedAttributeValuePair> reference, Set<String> referenceDocNames);

    public boolean isRelevantAttribute(Attribute attribute) {
        return true;
    }

    /** Returns a comparator of the agreement measure vectors. Defines what a good/bad agreement is. By default compares the first component of vectors */
    public Comparator<List<Double>> getVectorComparator() {
        return FIRST_COMPONENT_COMPARE;
    }
}
