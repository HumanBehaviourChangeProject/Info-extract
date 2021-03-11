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
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.extraction.candidates.Candidate;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.extractors.ArmAssociator;
import com.ibm.drl.hbcp.extraction.extractors.flair.ArmNameClusterer;
import com.ibm.drl.hbcp.extraction.extractors.flair.SentenceEntity;
import com.ibm.drl.hbcp.extraction.passages.SimplePassage;
import com.ibm.drl.hbcp.parser.Attributes;

import lombok.Data;

/**
 *
 * @author yhou
 */
public class ExtractPrediction {
    
    private final Gson gson = new Gson();

    /** Reads the output of the Flair REST service and index span annotations by their attribute */
    public Map<String, List<ValueWithContext>> extractPrediction(Reader reader) {
        Map<String, List<ValueWithContext>> entitiesPerDoc = new HashMap<>();
        Type type = new TypeToken<List<SentenceEntity>>() {
        }.getType();
        List<SentenceEntity> result = gson.fromJson(reader, type);
        for (SentenceEntity predict : result) {
            if (!predict.entities.isEmpty()) {
                for (SentenceEntity.Entity entity : predict.entities) {
                    String attributeName = getAttributeName(entity.type);
                    entitiesPerDoc.putIfAbsent(attributeName, new ArrayList<>());
                    entitiesPerDoc.get(attributeName).add(new ValueWithContext(entity.text, predict.text));
                }
            }
        }
        return entitiesPerDoc;
    }

    public Map<String, List<ValueWithContext>> extractPrediction(String jsonfile) throws IOException, Exception {
        InputStream inputStream = new FileInputStream(new File(jsonfile));
        try (Reader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            return extractPrediction(reader);
        }
    }

    private String getAttributeName(String type) {
        return type.replaceAll("_", " ");
    }

    @Data
    public static class ValueWithContext {
        private final String value;
        private final String context;
    }

    public static AttributeValueCollection<ArmifiedAttributeValuePair> armify(AttributeValueCollection<? extends ArmifiedAttributeValuePair> avps) {
        ArmAssociator armifier = new ArmAssociator();
        ArmNameClusterer clusterer = new ArmNameClusterer(2);
        List<ArmifiedAttributeValuePair> res = new ArrayList<>();
        AtomicInteger armId = new AtomicInteger(10);
        for (String docname : avps.getDocNames()) {
            Collection<? extends ArmifiedAttributeValuePair> avpsInDoc = avps.byDoc().get(docname);
            List<String> armNames = avpsInDoc.stream()
                    .filter(avp -> avp.getAttribute().equals(Attributes.get().getFromName("Arm name")))
                    .map(avp -> avp.getValue())
                    .distinct()
                    .collect(Collectors.toList());
            if (armNames.size() < 2) {
                // don't armify
                res.addAll(avpsInDoc);
            } else {
                // cluster the arm names, assuming 2 arms
                Set<Set<String>> twoArms = clusterer.getArmNameClusters(armNames);
                /*
                Set<Set<String>> twoArms = armNames.stream()
                        .map(armName -> Sets.newHashSet(armName))
                        .collect(Collectors.toSet());
                //*/
                // create the arms
                List<Arm> arms = twoArms.stream()
                        .filter(possibleNames -> !possibleNames.isEmpty())
                        .map(possibleNames -> new Arm(String.valueOf(armId.getAndIncrement()), possibleNames.iterator().next(), new ArrayList<>(new HashSet<>(possibleNames))))
                        .collect(Collectors.toList());
                // armify with the default associator
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armifiedAvps = armifier.associate(
                        avpsInDoc.stream()
                                .map(aavp -> new CandidateInPassage<ArmifiedAttributeValuePair>(new SimplePassage(aavp.getContext(), aavp.getDocName(), 1.0), aavp, 1.0, 1.0))
                                .collect(Collectors.toList()),
                        arms
                );
                // to debug
                List<Arm> usedArms = armifiedAvps.stream()
                        .map(cavp -> cavp.getAnswer().getArm())
                        .distinct()
                        .collect(Collectors.toList());
                res.addAll(armifiedAvps.stream().map(Candidate::getAnswer).collect(Collectors.toList()));
            }
        }
        return new AttributeValueCollection<>(res);
    }

    
    public static void main(String[] args) throws IOException, Exception{
        System.out.println(armify(loadFlairExtraction()).size());
    }

    public List<ArmifiedAttributeValuePair> loadFlairAvps(String docName, Map<String, List<ValueWithContext>> flairOutput) {
        List<ArmifiedAttributeValuePair> res = new ArrayList<>();
        for (String att : flairOutput.keySet()) {
            for (ValueWithContext valueWithContext : flairOutput.get(att)) {
                ArmifiedAttributeValuePair avp = new ArmifiedAttributeValuePair(
                        Attributes.get().getFromName(att),
                        valueWithContext.getValue(),
                        docName,
                        Arm.MAIN,
                        valueWithContext.getContext()
                );
                res.add(avp);
            }
        }
        return res;
    }

    public static AttributeValueCollection<ArmifiedAttributeValuePair> loadFlairExtraction() throws Exception {
        return loadFlairExtraction(new File("./flairExp/testfile_entityPrediction/"));
    }

    public static AttributeValueCollection<ArmifiedAttributeValuePair> loadFlairExtraction(File folder) throws Exception {
        List<ArmifiedAttributeValuePair> res = new ArrayList<>();
        ExtractPrediction extractor = new ExtractPrediction();
        for (File jsonfile : folder.listFiles()) {
            Map<String, List<ValueWithContext>> result = extractor.extractPrediction(jsonfile.getAbsolutePath());
            String docName = jsonfile.getName().replaceAll("\\.txt\\.json", "");
            List<ArmifiedAttributeValuePair> resForThisDoc = extractor.loadFlairAvps(docName, result);
            res.addAll(resForThisDoc);
        }
        return new AttributeValueCollection<>(res);
    }
    
}
