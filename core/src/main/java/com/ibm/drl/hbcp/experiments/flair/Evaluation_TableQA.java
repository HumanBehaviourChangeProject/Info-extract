/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.drl.hbcp.experiments.tapas.FlairTableInstancesGenerator;
import com.ibm.drl.hbcp.extraction.extractors.flair.SentenceEntityNew;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;

/**
 *
 * @author yhou
 */
public class Evaluation_TableQA {

    private static final int TP = 0;
    private static final int FP = 1;
    private static final int FN = 2;

    Gson gson;

    public Evaluation_TableQA() {
        gson = new Gson();
    }

    public Map<String, List<String>> extractPrediction(String jsonfile) throws IOException, Exception {
        Map<String, List<String>> entitiesPerDoc = new HashMap<>();
        Type type = new TypeToken<List<SentenceEntityNew>>() {
        }.getType();
        InputStream inputStream = new FileInputStream(new File(jsonfile));
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<SentenceEntityNew> result = gson.fromJson(reader, type);
        for (SentenceEntityNew predict : result) {
//            if(predict.text.contains("has a value of")) continue;
            if (!predict.entities.isEmpty()) {
                for (SentenceEntityNew.Entity entity : predict.entities) {
                    if (entitiesPerDoc.containsKey(entity.labels.get(0)._value.replace("_", " "))) {
                        entitiesPerDoc.get(entity.labels.get(0)._value.replace("_", " ")).add(entity.text);
                    } else {
                        List<String> entities = new ArrayList<>();
                        entities.add(entity.text);
                        entitiesPerDoc.put(entity.labels.get(0)._value.replace("_", " "), entities);
                    }
                }
            }
        }
        return entitiesPerDoc;
    }

    public Map<String, List<Pair<String, String>>> extractPrediction_PlusArm(String jsonfile) throws IOException, Exception {
        List<String> valueAttri = Lists.newArrayList(
                "Outcome value",
                "Mean age",
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Longest follow up",
                "Proportion employed",
                "Proportion achieved university or college education",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed"
        );
        Map<String, List<Pair<String, String>>> entitiesPerDoc = new HashMap<>();
//        for (String attr : valueAttri) {
//            entitiesPerDoc.put(attr, new HashMap<>());
//        }
        Type type = new TypeToken<List<SentenceEntityNew>>() {
        }.getType();
        InputStream inputStream = new FileInputStream(new File(jsonfile));
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<SentenceEntityNew> result = gson.fromJson(reader, type);
        for (SentenceEntityNew predict : result) {
            if (!predict.entities.isEmpty()) {
                List<SentenceEntityNew.Entity> EntitiesPerSent = new ArrayList<>();
                List<SentenceEntityNew.Entity> armNameEntitiesPerSent = new ArrayList<>();
                for (SentenceEntityNew.Entity entity : predict.entities) {
                    if (valueAttri.contains(entity.labels.get(0)._value.replace("_", " "))) {
                        EntitiesPerSent.add(entity);
                    }
                    if (entity.labels.get(0)._value.replace("_", " ").equalsIgnoreCase("Arm name")) {
                        armNameEntitiesPerSent.add(entity);
                    }
                }
                //find paired entity-armname 
                for (SentenceEntityNew.Entity entity : EntitiesPerSent) {
                    int currentDist = 1000;
                    String armname = "all";
                    for (SentenceEntityNew.Entity armNameEntity : armNameEntitiesPerSent) {
                        int distance = Math.abs(entity.start_pos - armNameEntity.start_pos);
                        if (distance < currentDist) {
                            armname = armNameEntity.text;
                            currentDist = distance;
                        }
                    }
                    String attriname = entity.labels.get(0)._value.replace("_", " ");
                    if (entitiesPerDoc.containsKey(attriname)) {
                        entitiesPerDoc.get(attriname).add(new Pair<String, String>(entity.text, armname));
                    } else {
                        List<Pair<String, String>> values = new ArrayList<>();
                        values.add(new Pair<String, String>(entity.text, armname));
                        entitiesPerDoc.put(attriname, values);
                    }
                }
            }
        }
        return entitiesPerDoc;
    }

    private Set<String> calculateRelaxedTP(Set<String> goldValues, Set<String> predictedValues) {
        //enforce one gold value only mapped to one predicted value for the case like:
        //gold: =0.055, prediction[0.055, 0.05]
        //we only map 0.055 which has the longest overlap with the gold annotation
        Map<String, String> gold2PredictionMap = new HashMap<>();
        Set<String> intersection = new HashSet<>();
        for (String predict : predictedValues) {
            for (String gold : goldValues) {
                if (gold.contains(predict)) {
                    if (gold2PredictionMap.containsKey(gold)) {
                        String oldPrediction = gold2PredictionMap.get(gold);
                        if (predict.contains(oldPrediction)) {
                            gold2PredictionMap.put(gold, predict);
                        }

                    } else {
                        gold2PredictionMap.put(gold, predict);
                    }
                    break;
                }
            }
        }
        for (String gold : gold2PredictionMap.keySet()) {
            intersection.add(gold2PredictionMap.get(gold));
        }
        return intersection;
    }

    private Set<String> calculateStrictTP(Set<String> goldValues, Set<String> predictedValues) {
        Set<String> intersection = new HashSet<String>(goldValues); // use the copy constructor
        intersection.retainAll(predictedValues);
        return intersection;
    }
    
    private List<Pair<String, String>> calculateRelaxedTP_plusarm(List<Pair<String, List<String>>> goldValues, List<Pair<String, String>> predictedValues){
        List<Pair<String, String>> intersection = new ArrayList<>();
        for(Pair<String, String> predict: predictedValues){
            for(Pair<String, List<String>> gold: goldValues){
                if((gold.getKey().contains(predict.getKey())||predict.getKey().contains(gold.getKey())&&gold.getValue().contains(predict.getValue()))){
                    intersection.add(new Pair<>(predict.getKey(), predict.getValue()));
                    break;
                }
            }
        }
        return intersection;
    }
    
    private List<Pair<String, String>> calculateStrictTP_plusarm(List<Pair<String, List<String>>> goldValues, List<Pair<String, String>> predictedValues){
        List<Pair<String, String>> intersection = new ArrayList<>();
        for(Pair<String, String> predict: predictedValues){
            for(Pair<String, List<String>> gold: goldValues){
                if((gold.getKey().equalsIgnoreCase(predict.getKey())&&gold.getValue().contains(predict.getValue()))){
                    intersection.add(new Pair<>(predict.getKey(), predict.getValue()));
                    break;
                }
            }
        }
        return intersection;
    }
    
    
    
    
    public void evaluate() throws IOException, Exception {
        Map<String, int[]> evalcount = new LinkedHashMap<>();
        Map<String, int[]> evalcount_plus_armname = new LinkedHashMap<>();
        List<String> targetedAttri_value = Lists.newArrayList(
                "Arm name",
                "Outcome value",
                "Mean age",
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Longest follow up",
                "Proportion employed",
                "Proportion achieved university or college education",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed"
        );
        for (String s : targetedAttri_value) {
            evalcount.put(s, new int[3]);
            if (!s.equalsIgnoreCase("Arm name")) {
                evalcount_plus_armname.put(s, new int[3]);
            }
        }
        FlairTableInstancesGenerator gen = new FlairTableInstancesGenerator();
        Map<String, Map<String, List<FlairTableInstancesGenerator.AnnotatedTable>>> splits = gen.getAnnotatedTablesSplits();
        File dir = new File(BaseDirInfo.getBaseDir() + "../flairExp_tableqa/test_prediction");
//        File dir = new File(BaseDirInfo.getBaseDir() + "../flairExp_tableqa/test_plusnegative_prediction");
        for (File jsonfile : dir.listFiles()) {
            String docname = jsonfile.getName().split("_table")[0];
            int tableid = Integer.valueOf(jsonfile.getName().split("_table")[1].replace(".json", ""));
            System.err.println(jsonfile.getName());
            FlairTableInstancesGenerator.AnnotatedTable goldTable = splits.get("test").get(docname).get(tableid);
            Map<String, List<String>> gold = new HashMap();
            Map<String, List<Pair<String, List<String>>>> gold_plusarm = new HashMap();
            for (FlairTableInstancesGenerator.CellWithAttribute positive : goldTable.getPositives()) {
                String attributeName = positive.getAttribute().getName();
                String attributeValue = positive.getCell().getValue();
                List<String> armnames = new ArrayList();
                if (positive.getAnnotation() != null) {
                    armnames = positive.getAnnotation().getArm().getAllNames();
                }
                if (armnames.isEmpty()) {
                    armnames.add("all");
                }
                if (gold.containsKey(attributeName)) {
                    gold.get(attributeName).add(attributeValue);
                } else {
                    List<String> values = new ArrayList();
                    values.add(attributeValue);
                    gold.put(attributeName, values);
                }
                if (!attributeName.equalsIgnoreCase("Arm name")) {
                    if (gold_plusarm.containsKey(attributeName)) {
                        gold_plusarm.get(attributeName).add(new Pair<>(attributeValue, armnames));
                    } else {
                        List<Pair<String, List<String>>> values = new ArrayList();
                        values.add(new Pair<>(attributeValue, armnames));
                        gold_plusarm.put(attributeName, values);
                    }
                }

            }
            //extract prediction
            Map<String, List<String>> prediction = extractPrediction(jsonfile.getAbsolutePath());
            Map<String, List<Pair<String, String>>> prediction_PlusArm = extractPrediction_PlusArm(jsonfile.getAbsolutePath());
            //evaluate - non-armification
            for (String att : evalcount.keySet()) {
                //value attribute evaluation
                Set<String> goldValues = new HashSet<>();
                Set<String> predictValues = new HashSet<>();
                if (targetedAttri_value.contains(att)) {
                    if (gold.containsKey(att)) {
                        goldValues = Sets.newHashSet(gold.get(att));
                    }
                    if (prediction.containsKey(att)) {
                        predictValues = Sets.newHashSet(prediction.get(att));
                    }
                    if (!goldValues.isEmpty() || !predictValues.isEmpty()) {
                        System.err.println(att);
                        System.err.println("gold:" + goldValues);
                        System.err.println("predict:" + predictValues);
                    }
                    //
                    if (!goldValues.isEmpty() && predictValues.isEmpty()) {
                        evalcount.get(att)[TP] = evalcount.get(att)[TP] + 0;
                        evalcount.get(att)[FP] = evalcount.get(att)[FP] + 0;
                        evalcount.get(att)[FN] = evalcount.get(att)[FN] + goldValues.size();
                    } else if (goldValues.isEmpty() && !predictValues.isEmpty()) {
                        evalcount.get(att)[TP] = evalcount.get(att)[TP] + 0;
                        evalcount.get(att)[FP] = evalcount.get(att)[FP] + predictValues.size();
                        evalcount.get(att)[FN] = evalcount.get(att)[FN] + 0;
                    } else if (!goldValues.isEmpty() && !predictValues.isEmpty()) {
                        Set<String> intersection = calculateStrictTP(goldValues, predictValues);
                        System.err.println("intersect:" + intersection);
                        System.err.println("tp:" + intersection.size());
                        evalcount.get(att)[TP] = evalcount.get(att)[TP] + intersection.size();
                        evalcount.get(att)[FP] = evalcount.get(att)[FP] + predictValues.size() - intersection.size();
                        evalcount.get(att)[FN] = evalcount.get(att)[FN] + goldValues.size() - intersection.size();
                        int fnsize = goldValues.size() - intersection.size();
                        System.err.println("fn:" + fnsize);
                    }
                }
            }
            //evaluate armification
            for (String att : evalcount_plus_armname.keySet()) {
                //value attribute evaluation
                List<Pair<String, List<String>>> goldValues = new ArrayList<>();
                List<Pair<String, String>> predictValues = new ArrayList<>();
                if (gold_plusarm.containsKey(att)) {
                    goldValues = gold_plusarm.get(att);
                }
                if (prediction_PlusArm.containsKey(att)) {
                    predictValues = prediction_PlusArm.get(att);
                }
                if (!goldValues.isEmpty() && predictValues.isEmpty()) {
                    evalcount_plus_armname.get(att)[TP] = evalcount_plus_armname.get(att)[TP] + 0;
                    evalcount_plus_armname.get(att)[FP] = evalcount_plus_armname.get(att)[FP] + 0;
                    evalcount_plus_armname.get(att)[FN] = evalcount_plus_armname.get(att)[FN] + goldValues.size();
                } else if (goldValues.isEmpty() && !predictValues.isEmpty()) {
                    evalcount_plus_armname.get(att)[TP] = evalcount_plus_armname.get(att)[TP] + 0;
                    evalcount_plus_armname.get(att)[FP] = evalcount_plus_armname.get(att)[FP] + predictValues.size();
                    evalcount_plus_armname.get(att)[FN] = evalcount_plus_armname.get(att)[FN] + 0;
                } else if (!goldValues.isEmpty() && !predictValues.isEmpty()) {
//                  //relaxed evaluation
                    List<Pair<String, String>> intersection = calculateStrictTP_plusarm(goldValues, predictValues);
                    System.err.println("intersect:" + intersection);
                    System.err.println("tp:" + intersection.size());
                    evalcount_plus_armname.get(att)[TP] = evalcount_plus_armname.get(att)[TP] + intersection.size();
                    evalcount_plus_armname.get(att)[FP] = evalcount_plus_armname.get(att)[FP] + predictValues.size() - intersection.size();
                    evalcount_plus_armname.get(att)[FN] = evalcount_plus_armname.get(att)[FN] + goldValues.size() - intersection.size();
                    int fnsize = goldValues.size() - intersection.size();
                    System.err.println("fn:" + fnsize);

                }

            }
        }
        System.err.println("non-armification evaluation:");
        calEvalMetric(evalcount);
        System.err.println("armification evaluation:");
        calEvalMetric(evalcount_plus_armname);
    }

    private double recall(int tp, int fn) {
        return precision(tp, fn);
    }

    private double precision(int tp, int fp) {
        if (tp + fp <= 0) {
            return 0.0;
        } else {
            return ((double) tp) / (tp + fp);
        }
    }

    private double f1Score(double prec, double recall) {
        if (prec + recall <= 0.0) {
            return 0.0;
        } else {
            return 2 * prec * recall / (prec + recall);
        }
    }

    private void calEvalMetric(Map<String, int[]> evalcount) {
        System.err.println("att \t precision \t recall \t fscore");
        double macro_precision = 0.0;
        double macro_recall = 0.0;
        double macro_fscore = 0.0;
        for (String att : evalcount.keySet()) {
//            System.err.println(att);
            double precision = precision(evalcount.get(att)[TP], evalcount.get(att)[FP]);
            double recall = recall(evalcount.get(att)[TP], evalcount.get(att)[FN]);
            double fscore = f1Score(precision, recall);
            macro_precision = macro_precision + precision;
            macro_recall = macro_recall + recall;
            macro_fscore = macro_fscore + fscore;
            System.err.println(att + "\t" + precision + "\t" + recall + "\t" + fscore + "\t (" + "tp:" + evalcount.get(att)[TP] + "-fp:" + evalcount.get(att)[FP] + "-fn:" + evalcount.get(att)[FN] + ")");
        }
        System.err.println("Macro:\t" + macro_precision / evalcount.size() + "\t" + macro_recall / evalcount.size() + "\t" + macro_fscore / evalcount.size());
        System.err.println("\n");
    }

    public static void main(String[] args) throws IOException, Exception {
        Evaluation_TableQA tableQAEval = new Evaluation_TableQA();
        tableQAEval.evaluate();

    }

}
