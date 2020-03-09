/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;

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
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.indexing.SentenceBasedIndexManager;
import com.ibm.drl.hbcp.extraction.indexing.SlidingWindowIndexManager;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.util.Props;
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

/**
 *
 * @author yhou
 */
public class Evaluation_NameAsCategory {

    private static final int TP = 0;
    private static final int FP = 1;
    private static final int FN = 2;

    Gson gson;

    public Evaluation_NameAsCategory() {
        gson = new Gson();
    }

    public Map<String, List<String>> extractPrediction(String jsonfile) throws IOException, Exception {
        Map<String, List<String>> entitiesPerDoc = new HashMap();
        Type type = new TypeToken<List<SentenceEntity>>() {
        }.getType();
        InputStream inputStream = new FileInputStream(new File(jsonfile));
        Reader reader = new BufferedReader(new InputStreamReader(inputStream));
        List<SentenceEntity> result = gson.fromJson(reader, type);
        for (SentenceEntity predict : result) {
            if (!predict.entities.isEmpty()) {
                for (SentenceEntity.Entity entity : predict.entities) {
                    if (entitiesPerDoc.containsKey(entity.type.replace("_", " "))) {
                        entitiesPerDoc.get(entity.type.replace("_", " ")).add(entity.text);
                    } else {
                        List<String> entities = new ArrayList();
                        entities.add(entity.text);
                        entitiesPerDoc.put(entity.type.replace("_", " "), entities);
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
    

                
    public void evaluate() throws IOException, Exception {        
        List<String> targetedAttri_value = Lists.newArrayList(
                "Arm name",
                "Outcome value", 
                "Mean age", 
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Lower-level geographical region",
                "Longest follow up",
                "Effect size p value",
                "Effect size estimate",
                
                "Proportion employed",
                "Proportion achieved university or college education",
                "Country of intervention",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed",
//                "Proportion identifying as belonging to a specific ethnic group",
//                "Proportion belonging to specified family or household income category",
//                "Proportion belonging to specified individual income category",
//                "Aggregate relationship status",
                "Aggregate health status type",
                "Site",
                "Individual-level allocated",
                "Individual-level analysed",
                "Expertise of Source",
                
                "Biochemical verification",
                "Website / Computer Program / App",
                "Printed material",
                "Digital content type",
                "Pill",
                "Somatic",
                "Health Professional",
                "Psychologist",
                "Researcher not otherwise specified",
                "Interventionist not otherwise specified",
                "Healthcare facility"
                );
        List<String> targetedAttri_present = Lists.newArrayList(
                
                "1.1.Goal setting (behavior)",
                "1.2 Problem solving",
                "1.4 Action planning",
                "2.2 Feedback on behaviour",
                "2.3 Self-monitoring of behavior",
                "3.1 Social support (unspecified)",
                "5.1 Information about health consequences",
                "5.3 Information about social and environmental consequences",
                "11.1 Pharmacological support",
                "11.2 Reduce negative emotions",
                "4.1 Instruction on how to perform the behavior", 
                "4.5. Advise to change behavior",
                "Aggregate patient role",
                "Hospital facility",
                "Doctor-led primary care facility",
                "Smoking", 
                "Self report",
                "Odds Ratio",
                "Face to face",
                "Distance",
                "Patch",
                "Individual",
                "Group-based"
             );
 
        Map<String, int[]> evalcount_value = new LinkedHashMap();
        Map<String, int[]> evalcount_present = new LinkedHashMap();
        for (String s : targetedAttri_value) {
            evalcount_value.put(s, new int[3]);
        }
        for(String s: targetedAttri_present)
            evalcount_present.put(s, new int[3]);

        //gold
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        IndexManager index = getDefaultIndexManager(props);
        Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>> groundTruthPerDoc = new HashMap();
        

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
            groundTruthPerDoc.put(docname, cleaned);
        }

        
        //prediction_baseline
//        Map<String, Map<String, List<String>>> prediction_bl = extract_baseline();
        File dir = new File("./flairExp/testfile_entityPrediction/");
//        File dir = new File("./flairExp/testfile_lrec_entityPrediction/");
//        File dir = new File("./flairExp/testfile_entityPrediction_wotable/");
        for (File jsonfile : dir.listFiles()) {
            String docname = jsonfile.getName().split(".txt")[0];
            System.err.println(docname);
            Map<String, List<String>> gold = new HashMap();
            for (ArmifiedAttributeValuePair cap : groundTruthPerDoc.get(docname)) {
//                gold.put(cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"), cap.getValue());
                if (gold.containsKey(cap.getAttribute().getName().trim())) {
                    if(cap.getAttribute().getId().equalsIgnoreCase("5730447")){
                      gold.get(cap.getAttribute().getName().trim()).addAll(cap.getArm().getAllNames());
                    }else{
                      gold.get(cap.getAttribute().getName().trim()).add(cap.getValue());
                    }
                } else {
                    List<String> values = new ArrayList();
                    if(cap.getAttribute().getName().trim().equalsIgnoreCase("5730447")){
                         values.addAll(cap.getArm().getAllNames());
                    }else{
                         values.add(cap.getValue());
                    }
                    gold.put(cap.getAttribute().getName().trim(), values);
                }
            }
            Map<String, List<String>> prediction = extractPrediction(jsonfile.getAbsolutePath());
//            Map<String, List<String>> prediction = prediction_bl.get(docname);
//            for (String att : result.keySet()) {
//                System.err.println(att + "--" + result.get(att));
//            }
            for (String att : evalcount_present.keySet()) {
                String goldStr = "";
                String predictStr = "";
                Set<String> goldValues = new HashSet();
                Set<String> predictValues = new HashSet();
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
                        evalcount_present.get(att)[TP]++;
                    } else if (goldStr.equalsIgnoreCase("1") && predictStr.equalsIgnoreCase("0")) {
                        evalcount_present.get(att)[FN]++;
                    } else if (goldStr.equalsIgnoreCase("0") && predictStr.equalsIgnoreCase("1")) {
                        evalcount_present.get(att)[FP]++;
                    }

                }
            }
            
            for(String att: evalcount_value.keySet()){
                //value attribute
                String goldStr = "";
                String predictStr = "";
                Set<String> goldValues = new HashSet();
                Set<String> predictValues = new HashSet();
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
                        evalcount_value.get(att)[TP] = evalcount_value.get(att)[TP] + 0;
                        evalcount_value.get(att)[FP] = evalcount_value.get(att)[FP] + 0;
                        evalcount_value.get(att)[FN] = evalcount_value.get(att)[FN] + goldValues.size();
                    } else if (goldValues.isEmpty() && !predictValues.isEmpty()) {
                        evalcount_value.get(att)[TP] = evalcount_value.get(att)[TP] + 0;
                        evalcount_value.get(att)[FP] = evalcount_value.get(att)[FP] + predictValues.size();
                        evalcount_value.get(att)[FN] = evalcount_value.get(att)[FN] + 0;
                    } else if (!goldValues.isEmpty() && !predictValues.isEmpty()) {
                        //strict evaluation
//                        Set<String> intersection = new HashSet<String>(goldValues); // use the copy constructor
//                        intersection.retainAll(predictValues);
//                        //relaxed evaluation
                        Set<String> intersection = calculateRelaxedTP(goldValues, predictValues);
                        System.err.println("intersect:" + intersection);
                        System.err.println("tp:" + intersection.size());
                        evalcount_value.get(att)[TP] = evalcount_value.get(att)[TP] + intersection.size();
                        evalcount_value.get(att)[FP] = evalcount_value.get(att)[FP] + predictValues.size() - intersection.size();
                        evalcount_value.get(att)[FN] = evalcount_value.get(att)[FN] + goldValues.size() - intersection.size();
                        int fnsize = goldValues.size() - intersection.size();
                        System.err.println("fn:" + fnsize);

                    }

                }

            }
        }
        //
        System.err.println("attribute present evaluation:");
        System.err.println("att \t precision \t recall \t fscore");
        for (String att : evalcount_present.keySet()) {
//            System.err.println(att);
            double precision = precision(evalcount_present.get(att)[TP], evalcount_present.get(att)[FP]);
            double recall = recall(evalcount_present.get(att)[TP], evalcount_present.get(att)[FN]);
            double fscore = f1Score(precision, recall);
            System.err.println(att  + "\t" + precision + "\t" + recall + "\t" + fscore + "\t (" + "tp:"  + evalcount_present.get(att)[TP] + "-fp:" + evalcount_present.get(att)[FP] + "-fn:" + evalcount_present.get(att)[FN] + ")");
        }
        System.err.println("attribute value evaluation:");
        System.err.println("att \t precision \t recall \t fscore");
        for (String att : evalcount_value.keySet()) {
//            System.err.println(att);
            double precision = precision(evalcount_value.get(att)[TP], evalcount_value.get(att)[FP]);
            double recall = recall(evalcount_value.get(att)[TP], evalcount_value.get(att)[FN]);
            double fscore = f1Score(precision, recall);
            System.err.println(att  + "\t" + precision + "\t" + recall + "\t" + fscore + "\t (" + "tp:"  + evalcount_value.get(att)[TP] + "-fp:" + evalcount_value.get(att)[FP] + "-fn:" + evalcount_value.get(att)[FN] + ")");
        }

    }
    
    private Set<String> calculateRelaxedTP(Set<String> goldValues, Set<String> predictedValues){
       //enforce one gold value only mapped to one predicted value for the case like:
       //gold: =0.055, prediction[0.055, 0.05]
       //we only map 0.055 which has the longest overlap with the gold annotation
       Map<String, String> gold2PredictionMap = new HashMap();
       Set<String> intersection = new HashSet();
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
        Set<String> testfilename = new HashSet();
        File dir = new File("./flairExp/testfile_new/");
        for (File jsonfile : dir.listFiles()) {
            String docname = jsonfile.getName().split(".txt")[0];
            testfilename.add(docname);
        }
        
        Map<String, Map<String, List<String>>> resultsAllDoc = new HashMap();
        Properties props = Props.loadProperties();
        try (InformationExtractor extractor = new InformationExtractor(props)) {
            IndexManager index = extractor.getDefaultIndexManager(props);
            for (IndexedDocument doc : index.getAllDocuments()) {
                if(!testfilename.contains(doc.getDocName())) continue;
                
                HashMap<String, List<String>> res = new HashMap<String, List<String>>();
                
                Collection<CandidateInDoc<Arm>> candidateArms = extractor.getArmExtractor().extract(doc);
                Collection<Arm> arms = candidateArms.stream().map(x -> x.getAnswer()).collect(Collectors.toSet());
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results = extractor.extract(doc);
                List<String> armNames = new ArrayList();
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
                        List<String> values = new ArrayList();
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

        Evaluation_NameAsCategory extractor = new Evaluation_NameAsCategory();
//       extractor.extractPrediction();
        extractor.evaluate();
//         extractor.extract_baseline();
    }

}
