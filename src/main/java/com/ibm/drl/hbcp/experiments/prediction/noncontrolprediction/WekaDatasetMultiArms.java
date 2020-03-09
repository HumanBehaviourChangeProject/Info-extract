package com.ibm.drl.hbcp.experiments.prediction.noncontrolprediction;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.predictor.DataInstance;
import com.ibm.drl.hbcp.predictor.regression.WekaDataset;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class WekaDatasetMultiArms extends WekaDataset {

    public WekaDatasetMultiArms(List<DataInstance> trainInstances, List<DataInstance> testInstances) throws IOException {
        super(trainInstances, testInstances);
    }

    @Override
    protected Map<Attribute, List<String>> getNominalAttributes(Properties props) {
        Map<Attribute, List<String>> unarmifiedNominal = super.getNominalAttributes(props);
        Map<Attribute, List<String>> res = new HashMap<>(unarmifiedNominal);
        // for each of these we create the same, but for the control group
        for (Attribute a : unarmifiedNominal.keySet()) {
            Attribute aControl = NonControlPredictionInstanceCreator.getControlAttribute(a);
            res.put(aControl, unarmifiedNominal.get(a));
        }
        return res;
    }
}
