package com.ibm.drl.hbcp.experiments.tapas;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.experiments.flair.GenerateTrainingData_NameAsCategory;
import com.ibm.drl.hbcp.parser.Attributes;
import edu.emory.mathcs.backport.java.util.Collections;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

public class QAEntities {

    private static final double TRAIN_FRACTION_OF_TRAIN = 0.9;

    public static final Set<Attribute> ENTITIES = Lists.newArrayList(
            "Mean age",
            "Mean number of times tobacco used",
            "Proportion identifying as female gender",
            "Proportion identifying as male gender",
            "Proportion employed",
            "Proportion achieved university or college education",
            "Mean number of years in education completed",
            //"Longest follow up", // should be taken care of in Outcome Value
            "Outcome value",
            "Proportion in a legal marriage or union"
            //"Arm name", // should be taken care of in other values
    ).stream().map(name -> Attributes.get().getFromName(name)).collect(Collectors.toSet());

    private static final Logger log = LoggerFactory.getLogger(QAEntities.class);

    public static Map<String, List<TapasInstanceBuilder>> split(List<TapasInstanceBuilder> allInstances) throws Exception {
        Map<String, List<String>> trainTestDocNames = new GenerateTrainingData_NameAsCategory().generateTrainingTestingFiles();
        trainTestDocNames = splitTrainIntoTrainDev(trainTestDocNames);
        Map<String, List<TapasInstanceBuilder>> trainTestInstances = trainTestDocNames.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, docNames ->
                        allInstances.stream().filter(instance -> docNames.getValue().contains(instance.getDocName())).collect(Collectors.toList())));
        for (Attribute attribute : ENTITIES) {
            System.out.println(attribute);
            for (List<TapasInstanceBuilder> instances : trainTestInstances.values()) {
                long count = instances.stream().flatMap(i -> i.getQuestions().stream()).filter(q -> q.getAttribute().equals(attribute)).count();
                System.out.println("\t" + count);
            }
        }
        return trainTestInstances;
    }

    private static Map<String, List<String>> splitTrainIntoTrainDev(Map<String, List<String>> trainTestDocNames) {
        List<String> trainDocs = trainTestDocNames.get("train");
        // shuffle
        Collections.shuffle(trainDocs, new Random(0));
        int splitIndex = (int)Math.round(TRAIN_FRACTION_OF_TRAIN * trainDocs.size());
        List<String> train = trainDocs.subList(0, splitIndex);
        List<String> dev = trainDocs.subList(splitIndex, trainDocs.size());
        Map<String, List<String>> res = new HashMap<>(trainTestDocNames);
        // replace train
        res.put("train", train);
        // add dev
        res.put("dev", dev);
        return res;
    }
}
