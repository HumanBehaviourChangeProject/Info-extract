package com.ibm.drl.hbcp.predictor;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.jetbrains.annotations.NotNull;

import com.aliasi.util.Iterators;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
//import com.ibm.drl.hbcp.experiments.prediction.noncontrolprediction.NonControlPredictionInstanceCreator;
import com.ibm.drl.hbcp.util.ParsingUtils;

import lombok.Data;

/**
 * A DataInstance represents an (x, y) vector in the prediction problem.
 * The goal is to predict y (the outcome value usually) given the input x, describing a behavior change scenario.
 *
 * @author mgleize
 */
@Data
public class DataInstance implements Iterable<ArmifiedAttributeValuePair> {
    /** the input of a prediction problem (set of context, intervention, ... entities) */
    private final AttributeValueCollection<ArmifiedAttributeValuePair> x;
    /** the output of a prediction problem, typically the Outcome Value */
    private final ArmifiedAttributeValuePair y;


    /** Returns true if a double can be extracted from y... Additional check to ensure that the outcome values are less than 100 */
    public boolean hasNumericY() {
        return !ParsingUtils.parseAllDoubleStrings(y.getValue()).isEmpty() && getYNumeric() < 100;
    }

    /** Returns the numeric (double) value of y */
    public double getYNumeric() {
        return ParsingUtils.parseFirstDouble(y.getValue());
    }

    /**
     * Get the data instances for the specified doc (one instance per arm). The "y" (outcome value, most of the time) has to be
     * numeric, or the instance is discarded otherwise.
     * Assumes that the empty arm ("whole study" annotations) has been distributed to all the named arms beforehand.
     * @param allValues the entire collection of entities, over multiple documents
     * @param docName the name of the document
     * @return one data instance per arm in the document
     */
    public static List<DataInstance> getInstancesForDoc(AttributeValueCollection<ArmifiedAttributeValuePair> allValues, String docName) {
        List<DataInstance> res = new ArrayList<>();
        Map<Arm, Multiset<ArmifiedAttributeValuePair>> armifiedAvps = allValues.getArmifiedPairsInDoc(docName);
        if (armifiedAvps == null)
            return res;
        
        // pull out the outcome value
        for (Arm arm : armifiedAvps.keySet()) {
            Multiset<ArmifiedAttributeValuePair> avps = armifiedAvps.get(arm);
            Optional<DataInstance> instance = get(avps, aavp -> aavp.getAttribute().isOutcomeValue());
            // only add it if the outcome value is numeric
            if (instance.isPresent() && instance.get().hasNumericY()) {
                res.add(instance.get());
            }
            else {
                // System.out.println("Skipping study " + docName + " because no outcome was extracted...");
            }
        }
        return res;
    }
    
    public static Optional<DataInstance> get(Collection<ArmifiedAttributeValuePair> avps, Predicate<ArmifiedAttributeValuePair> outputValueCheck) {
        Optional<ArmifiedAttributeValuePair> outcomeValue = avps.stream()
                .filter(outputValueCheck)
                .findFirst();
        if (outcomeValue.isPresent()) {
            AttributeValueCollection<ArmifiedAttributeValuePair> withoutOutcomeValue = new AttributeValueCollection<>(
                    avps.stream().filter(aavp -> !outputValueCheck.test(aavp)).collect(Collectors.toList())
            );
            return Optional.of(new DataInstance(new AttributeValueCollection<>(withoutOutcomeValue), outcomeValue.get()));
        } else {
            return Optional.empty();
        }
    }

    @NotNull
    @Override
    public Iterator<ArmifiedAttributeValuePair> iterator() {
        return Iterators.sequence(x.iterator(), Iterators.singleton(y));
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        List<ArmifiedAttributeValuePair> avp_list = x.stream().collect(Collectors.toList());
        
        for (ArmifiedAttributeValuePair avp: avp_list) {
            buff
                .append("(")
                .append(avp.getAttribute())
                .append(":")
                .append(avp.getValue().replaceAll("\\s+", " "))
                .append("), ")
            ;
        }
        return buff.toString();
    }
}
