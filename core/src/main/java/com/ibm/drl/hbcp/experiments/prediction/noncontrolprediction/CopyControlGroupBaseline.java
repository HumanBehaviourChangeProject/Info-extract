package com.ibm.drl.hbcp.experiments.prediction.noncontrolprediction;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.predictor.DataInstance;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.util.ParsingUtils;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

public class CopyControlGroupBaseline {

    public static List<PredictionTuple> predict(List<DataInstance> multiArmTestInstances) {
        return multiArmTestInstances.stream()
                .map(CopyControlGroupBaseline::predict)
                .collect(Collectors.toList());
    }

    private static PredictionTuple predict(DataInstance instance) {
        // pull out the outcome value in the input (there should be one definitely)
        Optional<ArmifiedAttributeValuePair> controlOV = instance.getX().stream()
                .filter(aavp -> aavp.getAttribute().getName().equals("Outcome value (Control)"))
                .findFirst();
        if (!controlOV.isPresent()) {
            System.err.println("No control outcome value in the input whaaaaa?");
            return new PredictionTuple(instance.getYNumeric(), 0.0);
        } else {
            return new PredictionTuple(instance.getYNumeric(), ParsingUtils.parseFirstDouble(controlOV.get().getValue()));
        }
    }
}
