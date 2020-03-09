package com.ibm.drl.hbcp.api;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.predictor.api.Jsonable;

import javax.json.Json;
import javax.json.JsonValue;
import java.util.*;

public class ExtractedArmDocument implements Jsonable {

    public final String docName;
    public final String docTitle;
    public final String armName;
    public final String introduction;
    public String minAge = "";
    public String maxAge = "";
    public String meanAge = "";
    public String gender = "";
    public List<InterventionNames> intervention = new ArrayList<>();
    public String outcomeValue = "";
    public List<ArmifiedAttributeValuePair> values = new ArrayList<>();

    protected ExtractedArmDocument(String docName, String docTitle, String introduction, String armName) {
        this.docName = docName;
        this.docTitle = docTitle;
        this.introduction = introduction;
        this.armName = armName;
    }

    public static List<ExtractedArmDocument> getDocuments(String docName, String docTitle, String introduction, List<ArmifiedAttributeValuePair> docValues) {
        // armify the AVPs
        Map<String, List<ArmifiedAttributeValuePair>> armifiedAvps = new HashMap<>();
        for (ArmifiedAttributeValuePair avp : docValues) {
            armifiedAvps.putIfAbsent(avp.getArm().getStandardName(), new ArrayList<>());
            armifiedAvps.get(avp.getArm().getStandardName()).add(avp);
        }
        // add all AVPs in the empty arm to all the other arms
        // they're pushed at the front of the list, so that the arm-specific values override the shared values
        if (armifiedAvps.containsKey("")) {
            for (String armName : armifiedAvps.keySet()) {
                if (!armName.isEmpty()) {
                    armifiedAvps.get(armName).addAll(0, armifiedAvps.get(""));
                }
            }
        }
        // create the "virtual" arm-documents
        List<ExtractedArmDocument> res = new ArrayList<>();
        for (String armName : armifiedAvps.keySet()) {
            ExtractedArmDocument armDoc = new ExtractedArmDocument(docName, docTitle, introduction, armName);
            for (ArmifiedAttributeValuePair avp : armifiedAvps.get(armName)) {
                updateArmDocWithAvp(armDoc, avp);
            }
            res.add(armDoc);
        }
        return res;
    }

    private static void updateArmDocWithAvp(ExtractedArmDocument armDoc, ArmifiedAttributeValuePair avp) {
        // first add it to the values
        armDoc.values.add(avp);
        // then fill the right field of the arm-doc
        String attributeName = avp.getAttribute().getName().toLowerCase();
        if (attributeName.equals("minimum age")) {
            armDoc.minAge = avp.getValue();
        } else if (attributeName.equals("maximum age")) {
            armDoc.maxAge = avp.getValue();
        } else if (attributeName.equals("mean age")) {
            armDoc.meanAge = avp.getValue();
        } else if (attributeName.contains("gender")) {
            armDoc.gender = "Mixed gender";
        } else if (attributeName.contains("female") || attributeName.contains("male")) {
            armDoc.gender = avp.getAttribute().getName();
        } else if (avp.getAttribute().getType() == AttributeType.INTERVENTION) {
            InterventionNames interventionNames = new InterventionNames(avp.getAttribute().getName(), avp.getAttribute().getVeryShortName());
            if (!armDoc.intervention.contains(interventionNames))
                armDoc.intervention.add(interventionNames);
        } else if (attributeName.equals("outcome value")) {
            armDoc.outcomeValue = avp.getValue();
        }
    }

    public static class InterventionNames implements Jsonable {
        public final String name;
        public final String shortName;

        public InterventionNames(String name, String shortName) {
            this.name = name;
            this.shortName = shortName;
        }

        @Override
        public JsonValue toJson() {
            return Json.createObjectBuilder()
                    .add("name", name)
                    .add("shortName", shortName)
                    .build();
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            InterventionNames that = (InterventionNames) o;
            return Objects.equals(name, that.name);
        }

        @Override
        public int hashCode() {
            return Objects.hash(name);
        }
    }

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("doc", docName)
                .add("title", docTitle)
                .add("introduction", introduction)
                .add("arm", armName)
                .add("minAge", minAge)
                .add("maxAge", maxAge)
                .add("meanAge", meanAge)
                .add("gender", gender)
                .add("intervention", Jsonable.getJsonArrayFromList(intervention))
                .add("outcomeValue", outcomeValue)
                .add("values", Jsonable.getJsonArrayFromList(values))
                .build();
    }
}
