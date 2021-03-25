package com.ibm.drl.hbcp.predictor.api;

import lombok.Value;

import javax.json.Json;
import javax.json.JsonValue;
import java.util.List;

@Value
public class RecommendedIntervention implements Jsonable {

    /** The IDs of the Behavior-Change techniques making up the intervention. */
    List<String> bctIds;
    /** Prediction result: predicted outcome value and confidence */
    double predictedValue;
    double confidence;

    @Override
    public JsonValue toJson() {
        return Json.createObjectBuilder()
                .add("intervention", Jsonable.getJsonArrayFromStrings(bctIds))
                .add("predictedValue", predictedValue)
                .add("confidence", confidence)
                .build();
    }
}
