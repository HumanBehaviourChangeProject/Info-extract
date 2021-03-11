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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.beust.jcommander.internal.Lists;
import com.google.common.collect.Sets;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInDoc;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.extractors.InformationExtractor;
import com.ibm.drl.hbcp.extraction.extractors.flair.SentenceEntity;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.indexing.SentenceBasedIndexManager;
import com.ibm.drl.hbcp.extraction.indexing.SlidingWindowIndexManager;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.util.Props;

/**
 *
 * @author yhou
 */
public class ExtractEntityPrediction {

    private static final int TP = 0;
    private static final int FP = 1;
    private static final int FN = 2;

    Gson gson;

    public ExtractEntityPrediction() {
        gson = new Gson();
    }

    public Map<String, List<String>> extractPrediction(String jsonfile) throws IOException, Exception {
        Map<String, List<String>> entitiesPerDoc = new HashMap<>();
        Type type = new TypeToken<List<SentenceEntity>>() {
        }.getType();
        InputStream inputStream = new FileInputStream(new File(jsonfile));
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<SentenceEntity> result = gson.fromJson(reader, type);
        for (SentenceEntity predict : result) {
            if (!predict.entities.isEmpty()) {
                for (SentenceEntity.Entity entity : predict.entities) {
                    if (entitiesPerDoc.containsKey(entity.type.split("_")[0])) {
                        entitiesPerDoc.get(entity.type.split("_")[0]).add(entity.text);
                    } else {
                        List<String> entities = new ArrayList<>();
                        entities.add(entity.text);
                        entitiesPerDoc.put(entity.type.split("_")[0], entities);
                    }
                }
            }
        }
        return entitiesPerDoc;
    }

    /**
     * Get an index manager as defined by the properties. Can fail if properties
     * are missing or wrongly defined
     */
    IndexManager getDefaultIndexManager(Properties props) throws IOException {
        // figure out the directory of the index
        String indexPath = props.getProperty("index");
        File indexDir = new File(indexPath);
        Directory directory = FSDirectory.open(indexDir.toPath());
        // pick between sentence-based or sliding window paragraphs
        if (Boolean.parseBoolean(props.getProperty("use.sentence.based"))) {
            int numberOfSentencesPerParagraph = Integer.parseInt(props.getProperty("para.number.of.sentences"));
            return new SentenceBasedIndexManager(directory, numberOfSentencesPerParagraph, IndexManager.DEFAULT_ANALYZER);
        } else {
            return new SlidingWindowIndexManager(directory, props.getProperty("window.sizes").split(","), IndexManager.DEFAULT_ANALYZER);
        }
    }

    /**
     * Returns a list of documents paired with their annotations from a doc
     * index and a JSON annotation parser. Useful to quickly evaluate the
     * extractor with a default index/JSON.
     */
    List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getGroundTruthForEvaluation_fromJson(
            IndexManager index,
            JSONRefParser refParser
    ) throws IOException {
        Cleaners cleaners = new Cleaners(Props.loadProperties());
//        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs();
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> res = new ArrayList<>();
        for (String docname : refParser.getAttributeValuePairs().byDoc().keySet()) {
// 	AttributeValueCollection<AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);           
            Collection<? extends AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);
            // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
            if (annotations == null) {
                System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                continue;
            }
            AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(new AttributeValueCollection<>(annotations));
//            res.add(Pair.of(docname, new ArrayList<>(annotations)));
            res.add(Pair.of(docname, new ArrayList<>(cleaned)));
        }
        return res;
    }
    
    
    
    List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> getGroundTruthForEvaluation(
            IndexManager index,
            JSONRefParser refParser
    ) throws IOException {
        List<IndexedDocument> allDocs = index.getAllDocuments();
        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> res = new ArrayList<>();
        for (IndexedDocument doc : allDocs) {
            Collection<? extends ArmifiedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(doc.getDocName());
            // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
            if (annotations == null) {
                String docname = doc.getDocName();
                System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                continue;
            }
            res.add(Pair.of(doc, new ArrayList<>(annotations)));
        }
        return res;
    }

    public void extractPrediction() throws IOException, Exception {
//        File dir = new File("./flairExp/testfile_entityPrediction_full/");
        File dir = new File("./flairExp/testfile_new_entityPrediction/");
//        File dir = new File("./flairExp/testfile_new_bert_entityPrediction/");
        for (File jsonfile : dir.listFiles()) {
            String docname = jsonfile.getName().split(".txt")[0];
            Map<String, List<String>> result = extractPrediction(jsonfile.getAbsolutePath());
            System.err.println(docname);
            for (String att : result.keySet()) {
                System.err.println(att + "--" + result.get(att));
            }
        }
    }
    
 //      Set<String> targetedAttri = Sets.newHashSet("5730447"); //armnames
//      Set<String> targetedAttri = Sets.newHashSet("5140146"); outcome
//      Set<String> targetedAttri = Sets.newHashSet("5579089"); //minAge
//      Set<String> targetedAttri = Sets.newHashSet("5579090"); //maxAge
//      Set<String> targetedAttri = Sets.newHashSet("5579088"); //meanAge
//      Set<String> targetedAttri = Sets.newHashSet("5579092"); //gender, all male
//      Set<String> targetedAttri = Sets.newHashSet("5579093"); //gender, all female
//      Set<String> targetedAttri = Sets.newHashSet("5579095"); //gender, mix, doesn't work, needs to do inference in the doc level

//      Set<String> targetedAttri = Sets.newHashSet("5594105"); //mean number of times tobacco used
//      Set<String> targetedAttri = Sets.newHashSet("5579096"); //Proportion identifying as female gender
//      Set<String> targetedAttri = Sets.newHashSet("5579097"); //Proportion identifying as male gender
//        Set<String> targetedAttri = Sets.newHashSet("5579083"); //Proportion identifying as belonging to a specific ethnic group, too hard, not possible
//        Set<String> targetedAttri = Sets.newHashSet("4788959"); //Lower-level geographical region
//        Set<String> targetedAttri = Sets.newHashSet("3675720"); //4.1 Instruction on how to perform the behavior  
//        Set<String> targetedAttri = Sets.newHashSet("4087191"); //longest follow-up  
//        Set<String> targetedAttri = Sets.newHashSet("3870696"); //Effect size p value    
//        Set<String> targetedAttri = Sets.newHashSet("3870695"); //Effect size estimate      
//        Set<String> targetedAttri = Sets.newHashSet("4087186"); //Biochemical verification        
//        Set<String> targetedAttri = Sets.newHashSet("5579711"); //Proportion employed        
//        Set<String> targetedAttri = Sets.newHashSet("4085489"); //4.5. Advise to change behavior          
//        Set<String> targetedAttri = Sets.newHashSet("5579699"); //Proportion achieved university or college education          
//        Set<String> targetedAttri = Sets.newHashSet("4788958"); //Country of intervention 
//        Set<String> targetedAttri = Sets.newHashSet("4087184"); //self-report
//        Set<String> targetedAttri = Sets.newHashSet("3870686"); //Odds Ratio  
//        Set<String> targetedAttri = Sets.newHashSet("5580216"); //Aggregate patient role
//        Set<String> targetedAttri = Sets.newHashSet("5580235"); //Aggregate health status type
    

    public void evaluate() throws IOException, Exception {        
        Map<String, String> nameMap = new LinkedHashMap<>();
        nameMap.put("5730447", "armnames");
        nameMap.put("5140146", "outcome value");
        nameMap.put("5579089", "min age");
        nameMap.put("5579090", "max age");
        nameMap.put("5579088", "mean age");
        nameMap.put("5579092", "gender, all male");
        nameMap.put("5579093", "gender, all female");
        nameMap.put("5594105", "mean number of times tobacco used");
        nameMap.put("5579096", "Proportion identifying as female gender");
        nameMap.put("5579097", "Proportion identifying as male gender");
        nameMap.put("4788959", "Lower-level geographical region");
        nameMap.put("3675720", "BCT 4.1 Instruction on how to perform the behavior");
        nameMap.put("4087191", "longest follow-up");
        nameMap.put("3870696", "Effect size p value");
        nameMap.put("3870695", "Effect size estimate");
        nameMap.put("4087186", "Biochemical verification");
        nameMap.put("5579711", "Proportion employed");
        nameMap.put("4085489", "BCT 4.5. Advise to change behavior");
        nameMap.put("5579699", "Proportion achieved university or college education");
        nameMap.put("4788958", "Country of intervention");
        nameMap.put("4087184", "self-report");
        nameMap.put("3870686", "Odds Ratio");
        nameMap.put("5580216", "Aggregate patient role");
        nameMap.put("5580235", "Aggregate health status type");
        nameMap.put("3673271", "BCT 1.1.Goal setting (behavior)  ");
        nameMap.put("3673272", "BCT 1.2 Problem solving   ");
        nameMap.put("3673274", "BCT 1.4 Action planning  ");
        nameMap.put("3673283", "BCT 2.2 Feedback on behaviour   ");
        nameMap.put("3673284", "BCT 2.3 Self-monitoring of behavior  ");
        nameMap.put("3675717", "BCT 3.1 Social support (unspecified) ");
        nameMap.put("3673298", "BCT 5.1 Information about health consequences");
        nameMap.put("3673300", "BCT 5.3 Information about social and environmental consequences  ");
        nameMap.put("3675611", "BCT 11.1 Pharmacological support ");
        nameMap.put("3675612", "BCT 11.2 Reduce negative emotions");
        Set<String> targetedAttri_value = Sets.newHashSet("5730447", "5140146", "5579089", "5579090", "5579088","5579092","5579093","5594105","5579096","5579097","4788959","4087191","3870696","3870695","4087186","5579711","5579699","4788958","4087184","3870686","5580235");
        List<String> targetedAttri_present = Lists.newArrayList("3673271", "3673272", "3673274", "3673283", "3673284", "3675717", "3673298", "3673300", "3675611", "3675612","3675720","4085489","5580216");
//        List<String> targetedAttri_value = Lists.newArrayList("5579088");
//        Set<String> targetedAttri_present = Sets.newHashSet("3673271");

        Map<String, int[]> evalcount = new LinkedHashMap<>();
        for (String s : targetedAttri_value) {
            evalcount.put(s, new int[3]);
        }
        for(String s: targetedAttri_present)
            evalcount.put(s, new int[3]);

        //gold
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        IndexManager index = getDefaultIndexManager(props);
//        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser);
//        Map<String, Collection<ArmifiedAttributeValuePair>> groundTruthPerDoc = new HashMap();
//        for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> pairsPerDoc : groundTruth) {
//            IndexedDocument doc = pairsPerDoc.getKey();
//            groundTruthPerDoc.put(doc.getDocName(), pairsPerDoc.getRight());
//        }
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson(index, refParser);
        Map<String, Collection<AnnotatedAttributeValuePair>> groundTruthPerDoc = new HashMap<>();
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> pairsPerDoc : groundTruth) {
            String doc = pairsPerDoc.getKey();
            groundTruthPerDoc.put(doc, pairsPerDoc.getValue());
        }



        
        //prediction_baseline
//        Map<String, Map<String, List<String>>> prediction_bl = extract_baseline();
        //prediction-piolt
//        File dir = new File("./flairExp/testfile_entityPrediction/");
        //prediction-full attri
//        File dir = new File("./flairExp/testfile_entityPrediction_full/");
//        File dir = new File("./flairExp/testfile_new_entityPrediction/");
//        File dir = new File("./flairExp/testfile_new_bert_entityPrediction/");
        File dir = new File("./flairExp/testfile_entityPrediction/");
        for (File jsonfile : dir.listFiles()) {
            String docname = jsonfile.getName().split(".txt")[0];
            System.err.println(docname);
            Map<String, List<String>> gold = new HashMap<>();
            for (ArmifiedAttributeValuePair cap : groundTruthPerDoc.get(docname)) {
//                gold.put(cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"), cap.getValue());
                if (gold.containsKey(cap.getAttribute().getId())) {
                    if(cap.getAttribute().getId().equalsIgnoreCase("5730447")){
                      gold.get(cap.getAttribute().getId()).addAll(cap.getArm().getAllNames());
                    }else{
                      gold.get(cap.getAttribute().getId()).add(cap.getValue());
                    }
                } else {
                    List<String> values = new ArrayList<>();
                    if(cap.getAttribute().getId().equalsIgnoreCase("5730447")){
                         values.addAll(cap.getArm().getAllNames());
                    }else{
                         values.add(cap.getValue());
                    }
                    gold.put(cap.getAttribute().getId(), values);
                }
            }
            Map<String, List<String>> prediction = extractPrediction(jsonfile.getAbsolutePath());
//            Map<String, List<String>> prediction = prediction_bl.get(docname);
//            for (String att : result.keySet()) {
//                System.err.println(att + "--" + result.get(att));
//            }
            for (String att : evalcount.keySet()) {
                String goldStr = "";
                String predictStr = "";
                Set<String> goldValues = new HashSet<>();
                Set<String> predictValues = new HashSet<>();
                //present attribute
                if (targetedAttri_present.contains(att)) {
                    if (gold.containsKey(att)) {
                        goldStr = "1";
                    } else {
                        goldStr = "0";
                    }
                    if (prediction.containsKey(att)) {
                        predictStr = "1";
                    } else {
                        predictStr = "0";
                    }

//                    if (goldStr.equalsIgnoreCase(predictStr)) {
                if(goldStr.equalsIgnoreCase(predictStr)&&goldStr.equalsIgnoreCase("1")){
                        evalcount.get(att)[TP]++;
                    } else if (goldStr.equalsIgnoreCase("1") && predictStr.equalsIgnoreCase("0")) {
                        evalcount.get(att)[FN]++;
                    } else if (goldStr.equalsIgnoreCase("0") && predictStr.equalsIgnoreCase("1")) {
                        evalcount.get(att)[FP]++;
                    }

                }

                //value attribute
                if (targetedAttri_value.contains(att)) {
                    if (gold.containsKey(att)) {
                        goldValues = Sets.newHashSet(gold.get(att));
                    }
                    if (prediction.containsKey(att)) {
                        predictValues = Sets.newHashSet(prediction.get(att));
                    }
                    System.err.println("gold:" + goldValues);
                    System.err.println("predict:" + predictValues);
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
                        //strict evaluation
//                        Set<String> intersection = new HashSet<String>(goldValues); // use the copy constructor
//                        intersection.retainAll(predictValues);
//                        //relaxed evaluation
                        Set<String> intersection = calculateRelaxedTP(goldValues, predictValues);
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
        }
        //
        System.err.println("att \t precision \t recall \t fscore");
        for (String att : evalcount.keySet()) {
//            System.err.println(att);
            double precision = precision(evalcount.get(att)[TP], evalcount.get(att)[FP]);
            double recall = recall(evalcount.get(att)[TP], evalcount.get(att)[FN]);
            double fscore = f1Score(precision, recall);
            System.err.println(att + "--" + nameMap.get(att) + "\t" + precision + "\t" + recall + "\t" + fscore + "\t (" + "tp:"  + evalcount.get(att)[TP] + "-fp:" + evalcount.get(att)[FP] + "-fn:" + evalcount.get(att)[FN]);
        }

    }
    
    private Set<String> calculateRelaxedTP(Set<String> goldValues, Set<String> predictedValues){
       //enforce one gold value only mapped to one predicted value for the case like:
       //gold: =0.055, prediction[0.055, 0.05]
       //we only map 0.055 which has the longest overlap with the gold annotation
       Map<String, String> gold2PredictionMap = new HashMap<>();
       Set<String> intersection = new HashSet<>();
       for(String predict: predictedValues){
           for(String gold: goldValues){
               if(gold.contains(predict)){
                   if(gold2PredictionMap.containsKey(gold)){
                       String oldPrediction = gold2PredictionMap.get(gold);
                       if(predict.contains(oldPrediction)){
                           gold2PredictionMap.put(gold, predict);
                       }
                       
                   }else{
                       gold2PredictionMap.put(gold, predict);
                   }
                   break;
               }
           }
       }
       for(String gold: gold2PredictionMap.keySet()){
           intersection.add(gold2PredictionMap.get(gold));
       }
       return intersection;
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
    
    public Map<String, Map<String, List<String>>> extract_baseline() throws IOException, ParseException {
        Set<String> testfilename = new HashSet<>();
        File dir = new File("./flairExp/testfile/");
        for (File jsonfile : dir.listFiles()) {
            String docname = jsonfile.getName().split(".txt")[0];
            testfilename.add(docname);
        }
        
        Map<String, Map<String, List<String>>> resultsAllDoc = new HashMap<>();
        Properties props = Props.loadProperties();
        try (InformationExtractor extractor = new InformationExtractor(props)) {
            IndexManager index = extractor.getDefaultIndexManager(props);
            for (IndexedDocument doc : index.getAllDocuments()) {
                if(!testfilename.contains(doc.getDocName())) continue;
                
                HashMap<String, List<String>> res = new HashMap<String, List<String>>();
                
                Collection<CandidateInDoc<Arm>> candidateArms = extractor.getArmExtractor().extract(doc);
                Collection<Arm> arms = candidateArms.stream().map(x -> x.getAnswer()).collect(Collectors.toSet());
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results = extractor.extract(doc);
                List<String> armNames = new ArrayList<>();
                for(Arm arm: arms){
                    armNames.addAll(arm.getAllNames());
                }
                res.put("5730447", armNames);
                for(CandidateInPassage<ArmifiedAttributeValuePair> candi: results){
                    String value = candi.getAnswer().getValue();
                    String attribute = candi.getAnswer().getAttribute().getId();
                    if(res.containsKey(attribute)){
                        res.get(attribute).add(value);
                    }else{
                        List<String> values = new ArrayList<>();
                        values.add(value);
                        res.put(attribute, values);
                    }
                }
                resultsAllDoc.put(doc.getDocName(), res);
            }
        }
        return resultsAllDoc;
    }
    

    

    public static void main(String[] args) throws IOException, Exception {

        ExtractEntityPrediction extractor = new ExtractEntityPrediction();
//       extractor.extractPrediction();
        extractor.evaluate();
//         extractor.extract_baseline();
    }

}
