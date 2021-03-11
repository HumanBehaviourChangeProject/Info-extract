package com.ibm.drl.hbcp.predictor.api;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import lombok.Data;

import javax.json.*;
import java.util.*;

/**
 * Java object to hold similarity results for an arm matching a query in the prediction system.
 */
@Data
public class ArmSimilarityResult implements Jsonable {

    private final Arm arm;
    private final String docName;
    private final String title;
    private final double score;
    // -1.0 for N/A
    private final double outcomeValue;
    private final String followUp;
    private final SortedMap<Attribute, Double> attributeSimilarities;
    private final Set<Attribute> commonInterventions;
    private final Set<Attribute> onlyInQuery;
    private final Set<Attribute> onlyInArm;

    private final boolean isSimilarPopulation;
    private final boolean isSimilarIntervention;


    @Override
    public JsonValue toJson() {
        JsonArrayBuilder attributeSimilaritiesJson = Json.createArrayBuilder();
        for (Map.Entry<Attribute, Double> attributeSim : attributeSimilarities.entrySet()) {
            attributeSimilaritiesJson.add(Json.createObjectBuilder()
                    .add("attribute", attributeSim.getKey().toJson())
                    .add("similarity", attributeSim.getValue())
                    .build());
        }
        return Json.createObjectBuilder()
                .add("doc", docName)
                .add("title", title)
                .add("arm", arm.getStandardName())
                .add("score", score)
                .add("outcomeValue", outcomeValue)
                .add("followUp", followUp)
                .add("isSimilarPopulation", isSimilarPopulation)
                .add("isSimilarIntervention", isSimilarIntervention)
                .add("attributeSimilarities", attributeSimilaritiesJson)
                .add("interventionsInCommon", Jsonable.getJsonArrayFromCollection(new TreeSet<>(commonInterventions)))
                .add("interventionsOnlyInQuery", Jsonable.getJsonArrayFromCollection(new TreeSet<>(onlyInQuery)))
                .add("interventionsOnlyInArm", Jsonable.getJsonArrayFromCollection(new TreeSet<>(onlyInArm)))
                .build();
    }
}
