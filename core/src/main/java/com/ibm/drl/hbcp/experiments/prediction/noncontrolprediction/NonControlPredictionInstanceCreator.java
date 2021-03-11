package com.ibm.drl.hbcp.experiments.prediction.noncontrolprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.predictor.DataInstance;
import com.ibm.drl.hbcp.util.ParsingUtils;

import lombok.Getter;

public class NonControlPredictionInstanceCreator {

    private final Map<String, String> docnameToControlArm;
    private final AttributeValueCollection<ArmifiedAttributeValuePair> collection;
    @Getter
    private final List<DataInstance> train;
    @Getter
    private final List<DataInstance> test;

    public NonControlPredictionInstanceCreator(
            AttributeValueCollection<ArmifiedAttributeValuePair> baseCollection,
            Map<String, String> docnameToControlArm,
            List<String> trainDocs, List<String> testDocs) {
        collection = baseCollection;
        this.docnameToControlArm = docnameToControlArm;
        train = trainDocs.stream()
                .flatMap(docname -> getInstancesForDoc(docname).stream())
                .collect(Collectors.toList());
        test = testDocs.stream()
                .flatMap(docname -> getInstancesForDoc(docname).stream())
                .collect(Collectors.toList());
    }

    private List<DataInstance> getInstancesForDoc(String docname) {
        List<DataInstance> res = new ArrayList<>();
        // there shouldn't be an empty arm left here, they've been distributed
        Map<Arm, Multiset<ArmifiedAttributeValuePair>> avps = collection.getArmifiedPairsInDoc(docname);
        Optional<Arm> controlArm = avps.keySet().stream()
                .filter(arm -> arm.getAllNames().stream().anyMatch(name -> docnameToControlArm.get(docname).equals(name)))
                .findFirst();
        if (controlArm.isPresent()) {
            List<Arm> nonControlArms = avps.keySet().stream()
                    .filter(arm -> !arm.equals(controlArm.get()))
                    .collect(Collectors.toList());

            for (Arm nonControlArm : nonControlArms) {
                // put the control avps together with the non-control avps
                List<ArmifiedAttributeValuePair> multiArmAvps = new ArrayList<>(avps.get(nonControlArm));
                multiArmAvps.addAll(getControlValues(avps.get(controlArm.get())));
                // make an instance out of the 2-arm avp set (the OV of the non-control arm will be the output Y)
                Optional<DataInstance> instance = DataInstance.get(multiArmAvps, aavp -> aavp.getAttribute().isOutcomeValue());
                if (instance.isPresent() && isRelevantMultiArmInstance(instance.get())) {
                    res.add(instance.get());
                }
            }
        }
        return res;
    }

    /** Check that the input contains a numeric control outcome value, and that the output is a numeric outcome value */
    private boolean isRelevantMultiArmInstance(DataInstance instance) {
        return instance.getX().stream()
                .anyMatch(aavp -> aavp.getAttribute().getName().startsWith("Outcome value (Control)") && !ParsingUtils.parseAllDoubleStrings(aavp.getValue()).isEmpty())
                &&
                instance.hasNumericY();
    }

    private List<ArmifiedAttributeValuePair> getControlValues(Multiset<ArmifiedAttributeValuePair> controlArmAvps) {
        // copy the avps, but with a slightly modified "Control" version of their attributes
        return controlArmAvps.stream()
                .map(aavp -> new ArmifiedAttributeValuePair(
                        getControlAttribute(aavp.getAttribute()),
                        aavp.getValue(),
                        aavp.getDocName(),
                        aavp.getArm()
                ))
                .collect(Collectors.toList());
    }

    public static Attribute getControlAttribute(Attribute a) {
        return new Attribute(a.getId() + "(Control)", a.getType(), a.getName() + " (Control)");
    }
}
