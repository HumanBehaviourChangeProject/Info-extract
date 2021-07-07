/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.indexing.SentenceBasedIndexManager;
import com.ibm.drl.hbcp.extraction.indexing.SlidingWindowIndexManager;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeNameNumberTriple;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.parser.enriching.AnnotationOutcomesMiner;
import com.ibm.drl.hbcp.parser.pdf.reparsing.Reparser;
import com.ibm.drl.hbcp.util.Props;

import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;
import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.FileReader;


/**
 *
 * @author yhou
 */
public class GenerateTrainingData_NameAsCategory {
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yhou
 */
    public List<String> generateTestingSentence(File jsonPdfOutput){
        List<String> sentences = new ArrayList<>();
        try {
            Reparser parser = new Reparser(jsonPdfOutput);
            for (String str : parser.toText().split("\n")) {
                if (str.equalsIgnoreCase("references")) { //part of rank3 entities are about funding infor
                    break;
                }
                if (str.matches(".*?http:.*?")) {
                    continue;
                }
                if (str.split(" ").length < 6) {
                    continue;
                }
                if(str.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)-(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?"))
                    str = splitDashBetweenNumbers(str);
                if(str.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)/(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?"))
                    str = splitSlashBetweenNumbers(str);
                if(str.matches(".*? (\\d+th) .*?")||str.contains("1st")||str.contains("2nd")||str.contains("3rd"))
                    str = splitNumberth(str);
                if(str.matches(".*?(one|two|three|four|five|six|seven|eight|nine|twice)-(month|months|monthly|week|weeks|weekly|day|days|session|sessions|time).*?"))
                    str = splitDashBetweenTokens(str);
                sentences.add(str);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return sentences;        
    }

   public List<String> generateTestingSentence(String docName, String dir) {
//        System.err.println("generate testing sentence:" + docName);
        List<String> sentences = new ArrayList<>();
        File jsonPdfOutput = new File(dir + docName + ".json");
        try {
            Reparser parser = new Reparser(jsonPdfOutput);
            for (String str : parser.toText().split("\n")) {
//                if(str.contains("has a value of")&&str.split(" ").length>30) continue;
//                if (str.equalsIgnoreCase("acknowledgements") || str.equalsIgnoreCase("references")) {
//                    break;
//                }
                if (str.equalsIgnoreCase("references")) { //part of rank3 entities are about funding infor
                    break;
                }
                if (str.matches(".*?http:.*?")) {
                    continue;
                }
                if (str.split(" ").length < 6) {
                    continue;
                }
                if(str.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)-(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?"))
                    str = splitDashBetweenNumbers(str);
                if(str.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)/(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?"))
                    str = splitSlashBetweenNumbers(str);
                if(str.matches(".*? (\\d+th) .*?")||str.contains("1st")||str.contains("2nd")||str.contains("3rd"))
                    str = splitNumberth(str);
                if(str.matches(".*?(one|two|three|four|five|six|seven|eight|nine|twice)-(month|months|monthly|week|weeks|weekly|day|days|session|sessions|time).*?"))
                    str = splitDashBetweenTokens(str);
                sentences.add(str);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return sentences;
    }
     
    
    public List<String> generateTestingSentence_mention(String docName, String dir) {
//        System.err.println("generate testing sentence:" + docName);
        List<String> sentences = new ArrayList<>();
        File jsonPdfOutput = new File(dir + docName + ".json");
        try {
            Reparser parser = new Reparser(jsonPdfOutput);
            for (String str : parser.toText().split("\n")) {
//                if(str.contains("has a value of")&&str.split(" ").length>30) continue;
//                if (str.equalsIgnoreCase("acknowledgements") || str.equalsIgnoreCase("references")) {
//                    break;
//                }
                if (str.equalsIgnoreCase("references")) { //part of rank3 entities are about funding infor
                    break;
                }
                if (str.matches(".*?http:.*?")) {
                    continue;
                }
                if (str.split(" ").length < 6) {
                    continue;
                }
                if(str.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)-(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?"))
                    str = splitDashBetweenNumbers(str);
                if(str.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)/(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?"))
                    str = splitSlashBetweenNumbers(str);
                if(str.matches(".*? (\\d+th) .*?")||str.contains("1st")||str.contains("2nd")||str.contains("3rd"))
                    str = splitNumberth(str);
                if(str.matches(".*?(one|two|three|four|five|six|seven|eight|nine|twice)-(month|months|monthly|week|weeks|weekly|day|days|session|sessions|time).*?"))
                    str = splitDashBetweenTokens(str);
                for(String str1: str.split("(\\. |\\.89 |\\.\\'°01 |\\, \\(|; \\(|\\n|\n|\\?)")){
//                    if(str1.split("\\, ").length>10){
                    if(str1.contains("M.Sc.")){
                        System.err.println("filter:" + str1);
                        continue;
                    }
                    if(isContextFromTable(str1))
                        continue;
                    sentences.add(str1);
                }
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return sentences;
    }
    
    public void generateTrainTestData_ArmAssociation() throws IOException, Exception{
        int traintestsplit = 200;
        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/train_armAssociation.csv"));
        StringBuffer sb1 = new StringBuffer();
        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/test_armAssociation.csv"));
        StringBuffer sb2 = new StringBuffer();
        FileWriter writer3 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/test_armAssociation_hard.csv"));
        StringBuffer sb3 = new StringBuffer();
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        IndexManager index = getDefaultIndexManager(props);
        Map<String, Set<String>> armStat = new HashMap<>();
        Map<String, String> twoarmExp = new HashMap<>();
        Map<String, String> twoarmExp_hard = new HashMap<>();
        int controlCount = 0;
//        Set<String> targetedAttri = Sets.newHashSet("Outcome value");
        Set<String> targetedAttri = Sets.newHashSet(
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

//                "Arm name",
                "Outcome value",
                "Mean age",
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Proportion identifying as belonging to a specific ethnic group",
                "Lower-level geographical region",
                "Smoking",
                "4.1 Instruction on how to perform the behavior",
                
                "Longest follow up",
                "Effect size p value",
                "Effect size estimate",
                "Biochemical verification",
                "Proportion employed",
                "4.5. Advise to change behavior",
                "Proportion achieved university or college education",
                "Country of intervention",
                "Self report",
                "Odds Ratio",
                
                "Aggregate patient role",
                "Aggregate health status type",
                "Aggregate relationship status",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed",
                "Proportion belonging to specified family or household income category",
                "Proportion belonging to specified individual income category", 
                "Healthcare facility", 
                "Doctor-led primary care facility",
                "Hospital facility",
                
                "Site",
                "Individual-level allocated",
                "Individual-level analysed",
                "Face to face",
                "Distance",
                "Printed material",
                "Digital content type",
                "Website / Computer Program / App",
                "Somatic",
                "Patch",
                
                "Pill",
                "Individual",
                "Group-based",
                "Health Professional",
                "Psychologist",
                "Researcher not otherwise specified",
                "Interventionist not otherwise specified",
                "Expertise of Source");
//        List<String> trainingDocs = new ArrayList();
//        List<String> testfiles = new ArrayList();
//        List<String> allDocs = new ArrayList();
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson(refParser);
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> pairsPerDoc : groundTruth) {
            String doc = pairsPerDoc.getKey();
//            allDocs.add(doc);
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String context = cap.getContext();
                String attrName = cap.getAttribute().getName();
                String armName = cap.getArm().getStandardName();
                
                //arm name stats
                if (cap.getAttribute().getId().equalsIgnoreCase("5730447")) {//arm name
                    if(armStat.containsKey(doc)){
                        armStat.get(doc).add(cap.getArm().getStandardName());
                    }else{
                        Set<String> arms = new HashSet();
                        arms.add(cap.getArm().getStandardName());
                        armStat.put(doc, arms);
                    }
                }
            }
        }
        //stat about arm name distribution 
        for(String doc: armStat.keySet()){
            String armnames = "";
            for(String s: armStat.get(doc)){
                armnames = armnames + "#" + s;
            }
            if(armStat.get(doc).size()==2){
                boolean controlGroup = false;
                String names = "";
                for(String s: armStat.get(doc)){
                    if(s.toLowerCase().matches(".*?(standard|control|only|minimal|placebo|usual care|self-help|normal|comparison|alone|basic|comparator|non-|self help|no ).*?")){
                        controlGroup = true;
                        twoarmExp.put(doc, s);
                        break;
                        
                    }
                    else if(s.matches("(CBT|Discontinue patch|Treatment by phone|Brief Advice|Brief advice (BA)|"
                            + "Treatment as usual (TAU)|Cognitive behavioural therapy (CBT)"
                            + "|Conventional counselling|Quitline Referral|mHealth monitoring"
                            + "|Brief Advice|Low-frequency text contact (LFTC)"
                            + "|Monitoring and behavioural support|Static/Educational"
                            + "|Smoking CBT|Smoking cessation advice|Untailored text messages"
                            + "|Text messaging for general health|CBT + NRT|Brief Intervention"
                            + "|4-week arm|Brief-intensity|Voice-Call Intervention)")){
                        twoarmExp_hard.put(doc, s);
                        break;
                    }
                }
                if (controlGroup) {
                    controlCount++;
                }else{
                    for(String s: armStat.get(doc)){
                        names = names + "#" + s;
                    }
//                    System.err.println(names);
                }
            }
//            System.err.println(doc.replace(" ", "_") + "\t" + armStat.get(doc).size() + "\t" + armnames);
        }
            System.err.println(controlCount);    
            System.err.println("easy case:" + twoarmExp.keySet().size());
            System.err.println("hard case:" + twoarmExp_hard.keySet().size());

            for(String s: twoarmExp.keySet()){
                if(twoarmExp_hard.containsKey(s)){
                    System.err.println("duplicate:" + s);
                }
            }
//
        int count  = 0;
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> pairsPerDoc : groundTruth) {
            String doc = pairsPerDoc.getKey();
            //only considering documents with 2 arms, and we can identify one arm belong to the "control" arm
            if(!twoarmExp.containsKey(doc)) continue;
            count++;
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String context = cap.getContext();
                String attrName = cap.getAttribute().getName();
                String armName = cap.getArm().getStandardName();
                String controlGroupName = twoarmExp.get(doc);
                
                
                if (!targetedAttri.contains(cap.getAttribute().getName().trim())) {
                    continue;
                }
                
                
                //filter out context with table sentence
                if(isContextFromTable(context)) continue;
                
                String shortcontext = context.replace("\n", " ");
                if(shortcontext.split(" ").length>100){
                   List<String> splitsent = new ArrayList();
                    for(String str: context.split("( ; |\\.; |\\. ;|\\.;|\\.;|;;;)")){
                        if(str.split(" ").length>100||str.contains("\n")){
                            for(String str1: str.split("(\\. |\\, \\(|; \\(|\\n|\n|\\?)")){
                                splitsent.add(str1);
                            }
                        }else{
                            splitsent.add(str);
                        }
                    }
                    for(String sent: splitsent){
                        if(sent.contains(annotation.replace("\n", " "))){
                            shortcontext = sent;
                            break;
                        }
                    }
                   
                }
                
                if(shortcontext.split(" ").length>100) continue;
 





                String label = "all";
                if(armName.isEmpty()){
                    label = "all";
                }else if(armName.equalsIgnoreCase(controlGroupName)){
                    label = "control";
                }else{
                    label = "intervention";
                }
                
                //annotated context
                String instance = doc.replace(" ", "_") + "\t" + attrName + ":" + annotation.replace("\n", " ") + "\t" + shortcontext.replace("\n", " ") + "\t" + label;
                if(!twoarmExp_hard.containsKey(doc)&&twoarmExp.containsKey(doc)){
                if(count>traintestsplit){
                    sb2.append(instance).append("\n");
                }else{
                    sb1.append(instance).append("\n");
                }
                }
                //hard test instances
                if(twoarmExp_hard.containsKey(doc)&&!twoarmExp.containsKey(doc)){
                    sb3.append(instance).append("\n");
                }
                
            }
        }        
        
        //
        writer1.write(sb1.toString());
        writer1.close();
        writer2.write(sb2.toString());
        writer2.close();
        writer3.write(sb3.toString());
        writer3.close();        

    }
    
    //make sure the training data contains 200 papers which has two arms
    public Map<String, List<String>> generateTrainingTestingFiles() throws IOException, Exception{
        //find files which have two arms
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
//        IndexManager index = getDefaultIndexManager(props);
        Map<String, Set<String>> armStat = new HashMap<>();
        Map<String, String> twoarmExp = new HashMap<>();
        List<String> allDocs = new ArrayList<>();
        List<String> othertwoarmDocs = new ArrayList<>();
        List<String> otherarmDocs = new ArrayList<>();
        int controlCount = 0;
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson(refParser);
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> pairsPerDoc : groundTruth) {
            String doc = pairsPerDoc.getKey();
            allDocs.add(doc);
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String context = cap.getContext();
                String attrName = cap.getAttribute().getName();
                String armName = cap.getArm().getStandardName();
                if(armName.isEmpty()) continue;
//                System.err.println(armName);
                //arm name stats, 32 papers do not have "5730447"
//                if (cap.getAttribute().getId().equalsIgnoreCase("5730447")) {//arm name
                    if(armStat.containsKey(doc)){
                        armStat.get(doc).add(cap.getArm().getStandardName());
                    }else{
                        Set<String> arms = new HashSet();
                        arms.add(cap.getArm().getStandardName());
                        armStat.put(doc, arms);
                    }
//                }
            }
        }
        //stat about arm name distribution 
        for(String doc: armStat.keySet()){
            String armnames = "";
            for(String s: armStat.get(doc)){
                armnames = armnames + "#" + s;
            }
            if(armStat.get(doc).size()==2){
                boolean controlGroup = false;
                String names = "";
                for(String s: armStat.get(doc)){
                    if(s.toLowerCase().matches(".*?(standard|control|only|minimal|placebo|usual care|self-help|normal|comparison|alone|basic|comparator|non-|self help|no ).*?")){
                        controlGroup = true;
                        twoarmExp.put(doc, s);
                        break;
                        
                    }
                }
                if (controlGroup) {
                    controlCount++;
                }else{
                    for(String s: armStat.get(doc)){
                        names = names + "#" + s;
                    }
                    othertwoarmDocs.add(doc);
//                    System.err.println(names);
                }
            }else{
                otherarmDocs.add(doc);
            }
//            System.err.println(doc.replace(" ", "_") + "\t" + armStat.get(doc).size() + "\t" + armnames);
        }
            System.err.println(controlCount);    
            System.err.println("easy case:" + twoarmExp.keySet().size());
            System.err.println("all doc:" + allDocs.size());
            List<String> finalTrain = new ArrayList<>();
            List<String> finalTest = new ArrayList<>();
            List<String> twoArmDoc_easy = new ArrayList<>(twoarmExp.keySet());
            finalTrain.addAll(twoArmDoc_easy.subList(0, 209));
            finalTest.addAll(twoArmDoc_easy.subList(209, twoArmDoc_easy.size()));
            finalTrain.addAll(othertwoarmDocs.subList(0, 48));
            finalTest.addAll(othertwoarmDocs.subList(48, othertwoarmDocs.size()));
            finalTrain.addAll(otherarmDocs.subList(0, 103));
            finalTest.addAll(otherarmDocs.subList(103, otherarmDocs.size()));
            System.err.println("training:" + finalTrain.size());
            System.err.println("testing:" + finalTest.size());
            System.err.println("other two arm papers:" + othertwoarmDocs.size());
            System.err.println("more than two arms paper:" + otherarmDocs.size());
            System.err.println(armStat.keySet().size());
            System.err.println("trainSize:" + finalTrain.size());
            System.err.println("testSize:" + finalTest.size());
//            for(String doc: allDocs){
//                if(!armStat.keySet().contains(doc))
//                    System.err.println(doc);
//            }
            Map<String, List<String>> trainTestSplit = new HashMap<>();
            trainTestSplit.put("train", finalTrain);
            trainTestSplit.put("test", finalTest);
            return trainTestSplit;
    }
    
    public String fixParticalAnnotation(String context, String originalAnnotation){
        //fix some annotation errors
        //e five-week, actions. or actions h
        String annotation = originalAnnotation;
        if(annotation.matches("[a-zA-Z] .*?")){
            annotation = annotation.substring(2);
        }
        if(annotation.matches(".*? [a-zA-Z]")){
            annotation = annotation.substring(0, annotation.length()-2).trim();
        }
                
                
//fixing annotation problems which only annotate part of a token
//context:The brand fading program provided all treatment bytelephone contact.
//highlightedText:[ent bytelep##Distance]
        String context1 = context.replaceAll("\n", " ");
//                System.err.println(annotation + "#" + context1);
        if(!context1.isEmpty()&&!annotation.isEmpty()&&context1.contains(annotation)){
            int start = context1.indexOf(annotation);
            int end = context1.indexOf(annotation) + annotation.length()-1;
            while(!Character.isWhitespace(context1.charAt(start))){
                if(start==0) break;
                start = start - 1;
            }
//            while(!Character.isWhitespace(context1.charAt(end))&&context1.charAt(end)!='-'){
            while(!Character.isWhitespace(context1.charAt(end))){
                end = end + 1;
                if(end==context1.length()) break;
            }
            annotation = context1.substring(start, end).trim();
        }

        if(!annotation.equalsIgnoreCase("u.s.")&&annotation.matches(".*?(\\.|\\,|\\;)")){
            annotation = annotation.substring(0, annotation.length()-1).trim();
        }
        return annotation;
    }
    
    public Map<String, String> generateAnnotationPairsForNameValueEntity(String highlightedText){
        Map<String, String> pairs = new LinkedHashMap<>();
        String[] values = highlightedText.split("\\n");
        for(int i = 0; i<values.length-1; i=i+2){
            if(values[i].matches("\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%")&&!values[i+1].matches("\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%")){
                pairs.put(values[i+1], values[i]);
            }else if (!values[i].matches("\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%")&&values[i+1].matches("\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%")){
                pairs.put(values[i], values[i+1]);
            }else{
                break;
            }
        }
        return pairs;
 
    }
    
    public void generateTestingData_mention_classification() throws IOException, Exception{
        Map<String, List<String>> traintest = generateTrainingTestingFiles();
        String testdir = "../data/All_512Papers_04March20_extracted_fixname23/";
        BufferedWriter writer3 = new BufferedWriter(new FileWriter(new File(BaseDirInfo.getBaseDir() + "../mentionExp/menExt/test.tsv")));
        int filterSent = 0;
        int senttokencount = 0;
        String longestSent = "";
        int sentcount = 0;
        List<String> debug = new ArrayList();
        List<String> addedDoc = new ArrayList();
        for (String doc : traintest.get("test")) {
            List<String> sents = generateTestingSentence_mention(doc, testdir);
            if(sents.isEmpty()){
                System.err.println(doc + ": no corresponding xml file");
                continue;
            }
            addedDoc.add(doc);
            int sentid = 0;
            for (String block : sents) {
                if(block.contains("has a value of")) continue;
                List<String> tokens = new ArrayList();
                for(String s: block.split(" ")){
                    tokens.add(s);
                }
                if(tokens.size()>150) {
                    filterSent ++;
                    System.err.println("filter:"+ tokens.size() + "--" + block);
                    continue;
                }
                if(tokens.size()>senttokencount){
                    senttokencount = tokens.size();
                    longestSent = block;
                }
                for(int i=0; i<tokens.size(); i++) { //start position
                    for(int len = 1; len<=10; len++) {
//                  for(int len = 1; len<=tokens.size(); len++) {
			if(i + len>tokens.size()) break;
			String spanContent = "";
			String newsent = "";
			int originalStart = i;
			int originalEnd = i + len-1;
			for(int k = originalStart; k<=originalEnd; k++) {
                            spanContent = spanContent + " " + tokens.get(k);
			}
			spanContent = spanContent.trim();
			for(int j = 0; j<tokens.size(); j++) {
                            if(j==originalStart && j==originalEnd) {
				newsent = newsent + " [unused1] " + tokens.get(j) + " [unused2]";
                            }else if(j==originalStart) {
				newsent = newsent + " [unused1] " + tokens.get(j);
                            }else if(j==originalEnd) {
				newsent = newsent + " " + tokens.get(j) + " [unused2]";
                            }else {
				newsent = newsent + " " + tokens.get(j);
                            }
			}
			newsent = newsent.trim();
			// detect position of [unused1] and [unused2]
			int position_unused1 = originalStart;
			int position_unused2 = originalEnd + 2;
			String newMentionid = doc + "_sent" + sentid + "_"+position_unused1 + "_"+position_unused2;
			String isMention = "0";
			String gold_mention = "unknown";
			String gold_inforstatus = "unknown";
//                        if(newsent.contains("[unused2]")){
//                            System.err.println(spanContent + ":" + newsent);
//                        }
			writer3.append(isMention + "\t" + newMentionid + "\t" + "unknown" + " " + "unknown"  + "\t"
								+ newsent + "\t" + spanContent + "\t" + "unknown" + "\t" + gold_mention + "\t" + gold_inforstatus).append("\n");
                        sentcount++;
//                        if(sentcount<=1775072 && sentcount>=1775008){
//                            debug.add(newsent);
//                        }
                    }
                } 
            }
            sentid = sentid + 1;
        }
        writer3.close();
        System.err.println("filter sent:" + filterSent + ":" + senttokencount);
        System.err.println("longest sent:" + longestSent);
        for(String s: debug)
            System.err.println(s);
//        for(String s: addedDoc)
//            System.err.print(s);
        
    }
 
	public class ExtractedMention{
		public String mentionid;
		public String docid;
		public int sentid; 
		public int start; 
		public int end; 
		public String mentioncontent;
		public String sentconent;
		public String fullpremention;
		public String partialfullpremention;
		
		public ExtractedMention(String mentionid, String docid, int sentid, int start, int end, String mentioncontent, String sentconent) {
			this.mentionid = mentionid;
			this.docid = docid;
			this.sentid = sentid;
			this.start = start;
			this.end = end;
			this.mentioncontent = mentioncontent;
			this.sentconent = sentconent;
			this.fullpremention = "FALSE";
			this.partialfullpremention = "UNKNOWN";
		}
	}
    
    
    public Map<String, Map<Integer, List<ExtractedMention>>> getExtractedMentionPerDoc() throws IOException, Exception{
	BufferedReader textFileReader = new BufferedReader(new FileReader("/Users/yhou/git/hbcp-tableqa/mentionExp/menExt/mentionPredictions"));
        String line = "";
        Map<String, Map<Integer, List<ExtractedMention>>> mentions_sent_doc = new HashMap();
        Map<String, List<ExtractedMention>> mentionsdoc = new HashMap();
        while ((line = textFileReader.readLine()) != null) {
        	String mentionid = line.split("\t")[0];
        	String docid = mentionid.split("_sent")[0].replace("_", " ");
        	int sentid = Integer.valueOf(mentionid.split("_sent")[1].split("_")[0]);
        	int start = Integer.valueOf(mentionid.split("_sent")[1].split("_")[1]);
        	int end = Integer.valueOf(mentionid.split("_sent")[1].split("_")[2]);
        	String sent = line.split("\t")[2];
        	if(!sent.contains("[unused2]")) {
        		System.err.println(mentionid + ":" + sent);
        	}
        	String mention_content = line.split("\t")[3];
        	ExtractedMention mention = new ExtractedMention(mentionid, docid, sentid, start, end, mention_content, sent);
        	if(mentions_sent_doc.containsKey(docid)) {
        		Map<Integer, List<ExtractedMention>> sent_mentions = mentions_sent_doc.get(docid);
        		if(sent_mentions.containsKey(sentid)) {
        			sent_mentions.get(sentid).add(mention);
        		}else {
        			List<ExtractedMention> sentMentions= new ArrayList();
        			sentMentions.add(mention);
        			sent_mentions.put(sentid, sentMentions);
        		}
        	}else {
    			List<ExtractedMention> sentMentions= new ArrayList();
    			sentMentions.add(mention);
    			Map<Integer, List<ExtractedMention>> sent_mentions = new HashMap();
    			sent_mentions.put(sentid, sentMentions);
    			mentions_sent_doc.put(docid, sent_mentions);
        	}
        	if(mentionsdoc.containsKey(docid)) {
        		mentionsdoc.get(docid).add(mention);
        	}else {
        		List<ExtractedMention> mentions = new ArrayList();
        		mentions.add(mention);
        		mentionsdoc.put(docid, mentions);
            }
        }
        return mentions_sent_doc;
	}
    
    
    
    public void generateTrainingData_mention_classification() throws IOException, Exception{
        Map<String, List<String>> traintest = generateTrainingTestingFiles();
        Map<String, Map<Integer, List<ExtractedMention>>> extractedMentions = getExtractedMentionPerDoc();
        BufferedWriter writer1 = new BufferedWriter(new FileWriter(new File(BaseDirInfo.getBaseDir() + "../mentionExp/train.tsv")));

        BufferedWriter writer2 = new BufferedWriter(new FileWriter(new File(BaseDirInfo.getBaseDir() + "../mentionExp/test.tsv")));
        String testdir = "../data/All_512Papers_04March20_extracted_fixname23/";
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        Set<String> targetedAttri = Sets.newHashSet(
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

                "Arm name",
                "Outcome value",
                "Mean age",
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Proportion identifying as belonging to a specific ethnic group",
                "Lower-level geographical region",
                "Smoking",
                "4.1 Instruction on how to perform the behavior",
                
                "Longest follow up",
                "Effect size p value",
                "Effect size estimate",
                "Biochemical verification",
                "Proportion employed",
                "4.5. Advise to change behavior",
                "Proportion achieved university or college education",
                "Country of intervention",
                "Self report",
                "Odds Ratio",
                
                "Aggregate patient role",
                "Aggregate health status type",
                "Aggregate relationship status",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed",
                "Proportion belonging to specified family or household income category",
                "Proportion belonging to specified individual income category", 
                "Healthcare facility", 
                "Doctor-led primary care facility",
                "Hospital facility",
                
                "Site",
                "Individual-level allocated",
                "Individual-level analysed",
                "Face to face",
                "Distance",
                "Printed material",
                "Digital content type",
                "Website / Computer Program / App",
                "Somatic",
                "Patch",
                
                "Pill",
                "Individual",
                "Group-based",
                "Health Professional",
                "Psychologist",
                "Researcher not otherwise specified",
                "Interventionist not otherwise specified",
                "Expertise of Source",
                
                //rank3
                "Dose",
                "Overall duration",
                "Number of contacts",
                "Contact frequency",
                "Contact duration",
                "Format",
                "Nicotine dependence", 
                "Cognitive Behavioural Therapy", 
                "Mindfulness",
                "Motivational Interviewing",
                "Brief advice",
                "Physical activity",
                "Individual reasons for attrition",
                "Encountered intervention",
                "Completed intervention",
                "Sessions delivered",
                "Pharmaceutical company funding",
                "Tobacco company funding",
                "Research grant funding",
                "No funding",
                "Pharmaceutical company competing interest",
                "Tobacco company competing interest",
                "Research grant competing interest",
                "No competing interest"
                );
        Set<String> nameValueAttri = Sets.newHashSet(
                "Proportion identifying as belonging to a specific ethnic group",
                "Proportion belonging to specified family or household income category",
                "Proportion belonging to specified individual income category",
                "Aggregate relationship status",
                "Nicotine dependence",
                "Individual reasons for attrition"
                );
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson(refParser);
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> pairsPerDoc : groundTruth) {
            String doc = pairsPerDoc.getKey();
            Set<String> matchedTableSent = new HashSet();
            Map<String, Set<String>> annotationPerContext = new HashMap();
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String highlightedText = ((AnnotatedAttributeValuePair)cap).getHighlightedText();
                String context = cap.getContext();
                String attrName = cap.getAttribute().getName();
                if (!targetedAttri.contains(cap.getAttribute().getName().trim())) {
                    continue;
                }
                //annotated context
                if (isContextFromTable(context)) {
                    //context is from table
                    continue;
                }
                if (context.isEmpty() || annotation.isEmpty()) {
                    //context is empty
                    continue;
                }
                if (cap.getAttribute().getId().equalsIgnoreCase("5730447")) {//arm name
                    List<String> armNames = cap.getArm().getAllNames();
                    annotation = "";
                    for (String s : armNames) {
                        annotation = s + "##" + cap.getAttribute().getName().trim();
                        if (annotationPerContext.containsKey(context)) {
                            annotationPerContext.get(context).add(annotation + "##" + cap.getAttribute().getName().trim());
                        } else {
                            Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getName().trim());
                            annotationPerContext.put(context, anno);
                        }
                    }
                }else if(nameValueAttri.contains(cap.getAttribute().getName().trim())){
                       AnnotatedAttributeNameNumberTriple nameNumber = (AnnotatedAttributeNameNumberTriple)cap;
                       String name = nameNumber.getValueName();
                       String value = nameNumber.getValueNumber();
                       if(annotationPerContext.containsKey(context)){
                                annotationPerContext.get(context).add(name + "##" + cap.getAttribute().getName().trim() + "-name");
                                annotationPerContext.get(context).add(value + "##" + cap.getAttribute().getName().trim() + "-value");
                        }else{
                                Set<String> anno = Sets.newHashSet(name + "##" + cap.getAttribute().getName().trim() + "-name");
                                anno.add(value + "##" + cap.getAttribute().getName().trim() + "-value");
                                annotationPerContext.put(context, anno);
                        }
                }else {
                    if (annotationPerContext.containsKey(context)) {
                        annotationPerContext.get(context).add(annotation + "##"  + cap.getAttribute().getName().trim());
                    } else {
                        Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getName().trim());
                        annotationPerContext.put(context, anno);
                    }
                }
            }
            //write to file per doc

            for (String context : annotationPerContext.keySet()) {
                boolean problematicAnnotation = true;
                List<String> splitsent = new ArrayList<>();
                for(String str: context.split("( ; |\\.; |\\. ;|\\.;|\\.;|;;;)")){
                    if(str.split(" ").length>100||(str.contains("\n")&&str.replaceAll("\n", " ").split(" ").length>=100)){
                        for(String str1: str.split("(\\. |\\, \\(|; \\(|\\n|\n|\\?)")){
                            splitsent.add(str1);
                        }
                    }else{
                        splitsent.add(str.replaceAll("\n", " "));
                    }
                }
                  for(String str: splitsent){
                      if(str.contains("has a value of")) continue;
                      if (str.split(" ").length >= 2) {
                        str = splitDashBetweenNumbers(str);
                        str = splitSlashBetweenNumbers(str);
                        str = splitNumberth(str);
                        str = splitDashBetweenTokens(str);
                        Sentence sent = new Sentence(str);
                        List<NERToken> annotation = getAnnotationOnSent(sent, annotationPerContext.get(context));
                        if(annotation==null){
                            //try to fix the partical annotation problem here, using the fixed annotation to match the context again
                            String currentcontext = str; 
                            Set<String> fixedAnnotation = new HashSet<>();
                            for(String annoplustype:annotationPerContext.get(context)){
                                String highlightedText = annoplustype.split("##")[0];
                                String type = annoplustype.split("##")[1];
                                String newhightlightedText = fixParticalAnnotation(currentcontext, highlightedText);
                                fixedAnnotation.add(newhightlightedText + "##" + type);
                            }
                            annotation = getAnnotationOnSent(sent, fixedAnnotation);
                        }
                        if (annotation != null) {
                           if(context.contains("has a value of")){
                               continue;
                           }
                           
                            
                            List<String> results = generateMentionClassifierTrainingData(doc.replace(" ", "_"), annotation);
                            if (traintest.get("train").contains(doc)) {
                                for(String s: results){
                                    writer1.append(s).append("\n");
                                } 
                            }
                        }
                    }
                }
            }
        }
     //write testing data
        for(String doc: traintest.get("test")){
            Map<Integer, List<ExtractedMention>> mentions = extractedMentions.get(doc);
            if(mentions==null) continue;
            System.err.println(doc + ":" + mentions.get(0).size());
            for(Integer sentid: mentions.keySet()){
                for(ExtractedMention men: mentions.get(sentid)){
                    for(String attri: targetedAttri_mentionExp)
                        writer2.append(doc.replace(" ", "_") + "\t" + men.sentconent + "\t" + men.mentioncontent + "\t" + attri + "\t" + "0").append("\n");
                    }
                } 
            }
        writer1.close();
        writer2.close();
    }
 
    Set<String> targetedAttri_mentionExp = Sets.newHashSet(
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

                "Arm name",
                "Outcome value",
                "Mean age",
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Proportion identifying as belonging to a specific ethnic group",
                "Lower-level geographical region",
                "Smoking",
                "4.1 Instruction on how to perform the behavior",
                
                "Longest follow up",
                "Effect size p value",
                "Effect size estimate",
                "Biochemical verification",
                "Proportion employed",
                "4.5. Advise to change behavior",
                "Proportion achieved university or college education",
                "Country of intervention",
                "Self report",
                "Odds Ratio",
                
                "Aggregate patient role",
                "Aggregate health status type",
                "Aggregate relationship status",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed",
                "Proportion belonging to specified family or household income category",
                "Proportion belonging to specified individual income category", 
                "Healthcare facility", 
                "Doctor-led primary care facility",
                "Hospital facility",
                
                "Site",
                "Individual-level allocated",
                "Individual-level analysed",
                "Face to face",
                "Distance",
                "Printed material",
                "Digital content type",
                "Website / Computer Program / App",
                "Somatic",
                "Patch",
                
                "Pill",
                "Individual",
                "Group-based",
                "Health Professional",
                "Psychologist",
                "Researcher not otherwise specified",
                "Interventionist not otherwise specified",
                "Expertise of Source",
                
                //rank3
                "Dose",
                "Overall duration",
                "Number of contacts",
                "Contact frequency",
                "Contact duration",
                "Format",
                "Nicotine dependence", 
                "Cognitive Behavioural Therapy", 
                "Mindfulness",
                "Motivational Interviewing",
                "Brief advice",
                "Physical activity",
                "Individual reasons for attrition",
                "Encountered intervention",
                "Completed intervention",
                "Sessions delivered",
                "Pharmaceutical company funding",
                "Tobacco company funding",
                "Research grant funding",
                "No funding",
                "Pharmaceutical company competing interest",
                "Tobacco company competing interest",
                "Research grant competing interest",
                "No competing interest",
                
                "a specific ethnic group",
                "Proportion identifying as belonging to a specific ethnic group",
                "specified family or household income category",
                "Proportion belonging to specified family or household income category",
                "specified individual income category",
                "Proportion belonging to specified individual income category",
                "Aggregate relationship status name",
                "Aggregate relationship status value",
                "Nicotine dependence name",
                "Nicotine dependence value",
                "Individual reasons for attrition name",
                "Individual reasons for attrition value"
                );
    
    
    public List<String> generateMentionClassifierTrainingData (String doc, List<NERToken> annotation){
        List<String> results = new ArrayList();
        List<String> mentionIndexWithType = new ArrayList();
        Boolean mentionStart = false;
        int index = 0;
        int mentionStartIndex = 0;
        int mentionEndIndex = 0;
        String mentionType = "";
        for(NERToken token: annotation){
            if(!mentionStart&&token.nertag.contains("B-")){
                mentionStart = true;
                mentionStartIndex = index;
                mentionType = token.nertag;
	    }else if(mentionStart && token.nertag.contains("B")) {
		mentionEndIndex = index-1;
                mentionIndexWithType.add(mentionStartIndex + "#" + mentionEndIndex + "#" + mentionType);
                mentionStart = true;
                mentionType = token.nertag;
                mentionStartIndex = index;
                mentionEndIndex = 0;
            }else if(mentionStart && token.nertag.contains("O")) {
		mentionEndIndex = index -1;
                mentionIndexWithType.add(mentionStartIndex + "#" + mentionEndIndex + "#" + mentionType);
                mentionStart = false;
                mentionType = "";
                mentionStartIndex = 0;
                mentionEndIndex = 0;
            }
            index = index + 1;
	}
        for(String s: mentionIndexWithType){
            int start = Integer.valueOf(s.split("#")[0]);
            int end = Integer.valueOf(s.split("#")[1]);
            String type = s.split("#")[2];
            String sent = "";
            String spanContent = "";
            for(int k = start; k<=end; k++) {
		spanContent = spanContent + " " + annotation.get(k).word;
            }
	    spanContent = spanContent.trim();

            for(int i=0; i< annotation.size(); i++){
                NERToken token = annotation.get(i);
                if(start==i && end==i){
                    sent = sent + " [unused1] " + token.word + " [unused2]";
                }else if(start ==i){
                    sent = sent + " [unused1] " + token.word;
                }else if(end ==i){
                    sent = sent + " " + token.word + " [unused2]";
                }else{
                    sent = sent + " " + token.word;
                }
            }
            type = type.replace("B-", "").replaceAll("_", "").replace("-", "").trim();
            if(type.equalsIgnoreCase("Proportion identifying as belonging to a specific ethnic group name")){
                type = "a specific ethnic group";
            }
            if(type.equalsIgnoreCase("Proportion identifying as belonging to a specific ethnic group value")){
                type = "Proportion identifying as belonging to a specific ethnic group";
            }
            if(type.equalsIgnoreCase("Proportion belonging to specified family or household income category name")){
                type = "specified family or household income category";
            }
            if(type.equalsIgnoreCase("Proportion belonging to specified family or household income category value")){
                type = "Proportion belonging to specified family or household income category";
            }
            if(type.equalsIgnoreCase("Proportion belonging to specified individual income category name")){
                type = "specified individual income category";
            }
            if(type.equalsIgnoreCase("Proportion belonging to specified individual income category value")){
                type = "Proportion belonging to specified individual income category";
            }
            results.add(doc + "\t" + sent.trim() + "\t" +spanContent + "\t"+ type + "\t" + "1");
            for(String attri: targetedAttri_mentionExp){
                if(!type.equalsIgnoreCase(attri)){
                    results.add(doc + "\t" + sent.trim() + "\t" + spanContent + "\t"+ attri + "\t" + "0");
                }
            }
        }
        return results;
    }

    public void generateTrainTestData_BIO_Tagging() throws IOException, Exception {
        Map<String, List<String>> traintest = generateTrainingTestingFiles();
        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/train_rank123_oddsratio_wotablesent.csv"));
//        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/train_rank123_augment1_wotablesent.csv"));
//        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/train_rank123_augment1_newtablesent.csv"));
//        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/train_rank123_augment2.csv"));
//        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/train_rank123_augment1.csv"));
//        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/train_rank123_oldtablesent_moreoutcome.csv"));
//        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/train_rank12_wotable.csv"));
        StringBuffer sb1 = new StringBuffer();
//        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/test_rank123_augment1_newtablesent.csv"));
         FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/test_rank123_oddsratio_wotablesent.csv"));
//         FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/test_rank123_augment1_wotablesent.csv"));
//        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/test_rank123_augment2.csv"));
//        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/test_rank123_augment1.csv"));
//        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/test_rank123_oldtablesent_moreoutcome.csv"));
//        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/test_rank12_wotable.csv"));
        StringBuffer sb2 = new StringBuffer();
        FileWriter writer4 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/unmatch_050220.txt"));
        StringBuffer sb4 = new StringBuffer();
        
        String testdir = "../data/All_512Papers_04March20_extracted_fixname23/";

        List<Integer> instanceLength = new ArrayList();
        Map<Integer, String> instanceLengthDebug = new HashMap();
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
//        IndexManager index = getDefaultIndexManager(props);

//priority rank1 + rank2 + rank3
        Set<String> augmentAttri1 = Sets.newHashSet(
                "2.2 Feedback on behaviour",
                "Doctor-led primary care facility",
                "Patch",
                "Mindfulness",
                "Physical activity",
                "Tobacco company funding",
                "No funding",
                "Tobacco company competing interest",
                "Research grant competing interest",
                //value attribute
                "Proportion identifying as male gender",
                "Proportion employed",
                "Proportion achieved university or college education",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed",
                "Website / Computer Program / App",
                "Somatic",
                "Interventionist not otherwise specified",
                "Healthcare facility",
                "Format",
                "Encountered intervention",
                "Sessions delivered"
        );
        Set<String> augmentAttri_namevalue1 = Sets.newHashSet(
                "Proportion belonging to specified family or household income category-name",
                "Proportion belonging to specified individual income category-name",
                "Aggregate relationship status-name",
                "Proportion identifying as belonging to a specific ethnic group-name",
                "Nicotine dependence-name"
        );
        Set<String> augmentAttri_namevalue = Sets.newHashSet(
        );
        Set<String> augmentAttri = Sets.newHashSet(
        );

        Set<String> targetedAttri1 = Sets.newHashSet("Arm name");
        Set<String> targetedAttri2 = Sets.newHashSet(
//                "Proportion identifying as belonging to a specific ethnic group",
//                "Proportion belonging to specified family or household income category",
//                "Proportion belonging to specified individual income category",
//                "Aggregate relationship status",
//                "Nicotine dependence",
//                "Individual reasons for attrition",
//                "Encountered intervention"
//                "Proportion employed"
//                "Proportion identifying as male gender"
//                "2.2 Feedback on behaviour"
                );
        Set<String> nameValueAttri = Sets.newHashSet(
//                "Proportion identifying as belonging to a specific ethnic group"
        );
        Set<String> nameValueAttri1 = Sets.newHashSet(
                "Proportion identifying as belonging to a specific ethnic group",
                "Proportion belonging to specified family or household income category",
                "Proportion belonging to specified individual income category",
                "Aggregate relationship status",
                "Nicotine dependence",
                "Individual reasons for attrition"
                );
        Set<String> targetedAttri3 = Sets.newHashSet(
               "Doctor-led primary care facility"
//                "Proportion identifying as belonging to a specific ethnic group"
        );        
        Set<String> targetedAttri = Sets.newHashSet(
               "Odds Ratio"
//                "Proportion identifying as belonging to a specific ethnic group"
        );        
        Set<String> targetedAttri4 = Sets.newHashSet(
//                "Minimum age",
//                "Maximum age",
//                "All male",
//                "All female",
//                "Mixed gender",
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

                "Arm name",
                "Outcome value",
                "Mean age",
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Proportion identifying as belonging to a specific ethnic group",
                "Lower-level geographical region",
                "Smoking",
                "4.1 Instruction on how to perform the behavior",
                
                "Longest follow up",
                "Effect size p value",
                "Effect size estimate",
                "Biochemical verification",
                "Proportion employed",
                "4.5. Advise to change behavior",
                "Proportion achieved university or college education",
                "Country of intervention",
                "Self report",
                "Odds Ratio",
                
                "Aggregate patient role",
                "Aggregate health status type",
                "Aggregate relationship status",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed",
                "Proportion belonging to specified family or household income category",
                "Proportion belonging to specified individual income category", 
                "Healthcare facility", 
                "Doctor-led primary care facility",
                "Hospital facility",
                
                "Site",
                "Individual-level allocated",
                "Individual-level analysed",
                "Face to face",
                "Distance",
                "Printed material",
                "Digital content type",
                "Website / Computer Program / App",
                "Somatic",
                "Patch",
                
                "Pill",
                "Individual",
                "Group-based",
                "Health Professional",
                "Psychologist",
                "Researcher not otherwise specified",
                "Interventionist not otherwise specified",
                "Expertise of Source",
                
                //rank3
                "Dose",
                "Overall duration",
                "Number of contacts",
                "Contact frequency",
                "Contact duration",
                "Format",
                "Nicotine dependence", 
                "Cognitive Behavioural Therapy", 
                "Mindfulness",
                "Motivational Interviewing",
                "Brief advice",
                "Physical activity",
                "Individual reasons for attrition",
                "Encountered intervention",
                "Completed intervention",
                "Sessions delivered",
                "Pharmaceutical company funding",
                "Tobacco company funding",
                "Research grant funding",
                "No funding",
                "Pharmaceutical company competing interest",
                "Tobacco company competing interest",
                "Research grant competing interest",
                "No competing interest"
                );

        Map<String, List<Integer>> stat = new HashMap();
        Map<String, List<Integer>> stat_train = new HashMap();
        for(String attri: targetedAttri){
                stat.put(attri, Lists.newArrayList(0,0,0,0));
                stat_train.put(attri, Lists.newArrayList(0,0,0,0));
        }
        for(String attri: nameValueAttri){
                stat.put(attri + "-name", Lists.newArrayList(0,0,0,0));
                stat.put(attri + "-value", Lists.newArrayList(0,0,0,0));
                stat_train.put(attri + "-name", Lists.newArrayList(0,0,0,0));
                stat_train.put(attri + "-value", Lists.newArrayList(0,0,0,0));
        }
        
        Map<String, List<List<NERToken>>> annotationPool = new HashMap();
        Map<String, List<List<NERToken>>> annotationPool_nameValue = new HashMap();
        
        List<String> allDocs = new ArrayList();
//        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser);
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson(refParser);
//        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson_w_MinedOutcomevalues(refParser);
//        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson_LrecOpenAccess(index, refParser);

        int count = 0;
        int instanceNum = 0;
        int instanceNum_annotate = 0;
        int problematicAnnotationCount = 0;
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> pairsPerDoc : groundTruth) {
//            IndexedDocument doc = pairsPerDoc.getKey();
            String doc = pairsPerDoc.getKey();
            allDocs.add(doc);
//            System.err.println(doc);
//            if(!doc.contains("Webb Hooper 2017")) continue;
            Set<String> matchedTableSent = new HashSet();
//            System.err.println(doc.getDocName());
            Map<String, Set<String>> annotationPerContext = new HashMap();
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String highlightedText = ((AnnotatedAttributeValuePair)cap).getHighlightedText();
                String context = cap.getContext();
                String attrName = cap.getAttribute().getName();
                if (!targetedAttri.contains(cap.getAttribute().getName().trim())) {
                    continue;
                }
                instanceNum_annotate++;
                //annotated context
                int oldNum = stat.get(cap.getAttribute().getName().trim()).get(0);
                stat.get(cap.getAttribute().getName().trim()).set(0, oldNum + 1);
                if (isContextFromTable(context)) {
//                    System.err.println("table context:" + context);
                    //context is from table
                    int oldNum1 = stat.get(cap.getAttribute().getName().trim()).get(1);
                    stat.get(cap.getAttribute().getName().trim()).set(1, oldNum1 + 1);
                    continue;
                }
                if (context.isEmpty() || annotation.isEmpty()) {
                    //context is empty
                    int oldNum2 = stat.get(cap.getAttribute().getName().trim()).get(2);
                    stat.get(cap.getAttribute().getName().trim()).set(2, oldNum2 + 1);
                    continue;
                }
                if (cap.getAttribute().getId().equalsIgnoreCase("5730447")) {//arm name
                    List<String> armNames = cap.getArm().getAllNames();
                    annotation = "";
//                    System.err.println("context:" + context);
                    for (String s : armNames) {
//                        System.err.println("armName:" + s);
//                        annotation = s + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_");
                        annotation = s + "##" + cap.getAttribute().getName().trim();
                        if (annotationPerContext.containsKey(context)) {
//                            annotationPerContext.get(context).add(annotation + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"));
                            annotationPerContext.get(context).add(annotation + "##" + cap.getAttribute().getName().trim());
                        } else {
//                            Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"));
                            Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getName().trim());
                            annotationPerContext.put(context, anno);
                        }
                    }
                }else if(nameValueAttri.contains(cap.getAttribute().getName().trim())){
//                    System.err.println(generateAnnotationPairsForNameValueEntity(highlightedText));
//                    System.err.println(annotation + "##" + context);
//                    System.err.println();
//                    Map<String, String> pairs = generateAnnotationPairsForNameValueEntity(highlightedText);
                       AnnotatedAttributeNameNumberTriple nameNumber = (AnnotatedAttributeNameNumberTriple)cap;
                       String name = nameNumber.getValueName();
                       String value = nameNumber.getValueNumber();
                       if(annotationPerContext.containsKey(context)){
                                annotationPerContext.get(context).add(name + "##" + cap.getAttribute().getName().trim() + "-name");
                                annotationPerContext.get(context).add(value + "##" + cap.getAttribute().getName().trim() + "-value");
                        }else{
                                Set<String> anno = Sets.newHashSet(name + "##" + cap.getAttribute().getName().trim() + "-name");
                                anno.add(value + "##" + cap.getAttribute().getName().trim() + "-value");
                                annotationPerContext.put(context, anno);
                        }
                }else {
                    if (annotationPerContext.containsKey(context)) {
                        annotationPerContext.get(context).add(annotation + "##"  + cap.getAttribute().getName().trim());
                    } else {
                        Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getName().trim());
                        annotationPerContext.put(context, anno);
                    }
                }
            }
            //write to file per doc
            count++;

            for (String context : annotationPerContext.keySet()) {
//                System.err.println("context:" + context);
//                System.err.println("highlightedText:" + annotationPerContext.get(context));
//                instanceNum_annotate++;
                boolean problematicAnnotation = true;
                List<String> splitsent = new ArrayList<>();
                for(String str: context.split("( ; |\\.; |\\. ;|\\.;|\\.;|;;;)")){
                    
//                    if(str.contains("\n")){
//                        String newStr = str.replaceAll("\n", " ");
//                        if(newStr.split(" ").length<100){
//                            splitsent.add(newStr);
//                        }
//                    }
//                  
                    //choose short table sentences
//                    if(str.contains("has a value of")){
//                        if(str.split(" ").length>30)
//                            continue;
//                    }
//                    
                    if(str.split(" ").length>100||(str.contains("\n")&&str.replaceAll("\n", " ").split(" ").length>=100)){
                        for(String str1: str.split("(\\. |\\, \\(|; \\(|\\n|\n|\\?)")){
                            splitsent.add(str1);
                        }
                    }else{
                        splitsent.add(str.replaceAll("\n", " "));
                    }
                }
//                for (String str : context.split("( ; |.; |. ;|.;|.;)")) {
//                for (String str : context.split("(;|,)")) {
                  for(String str: splitsent){
                      if(str.contains("has a value of")) continue;
                      if (str.split(" ").length >= 2) {
                        str = splitDashBetweenNumbers(str);
                        str = splitSlashBetweenNumbers(str);
                        str = splitNumberth(str);
                        str = splitDashBetweenTokens(str);
                        Sentence sent = new Sentence(str);
                        List<NERToken> annotation = getAnnotationOnSent(sent, annotationPerContext.get(context));
//                        System.err.println(annotation + "--" + annotationPerContext.get(context));
                        if(annotation==null){
//                           System.err.println("context:" + context);
//                           System.err.println("highlightedText:" + annotationPerContext.get(context));
//                           System.err.println("\n");
                            //try to fix the partical annotation problem here, using the fixed annotation to match the context again
                            String currentcontext = str; 
                            Set<String> fixedAnnotation = new HashSet<>();
                            for(String annoplustype:annotationPerContext.get(context)){
                                String highlightedText = annoplustype.split("##")[0];
                                String type = annoplustype.split("##")[1];
                                String newhightlightedText = fixParticalAnnotation(currentcontext, highlightedText);
                                fixedAnnotation.add(newhightlightedText + "##" + type);
                            }
                            annotation = getAnnotationOnSent(sent, fixedAnnotation);
                        }
                        if (annotation != null) {
                           if(context.contains("has a value of")){
                               matchedTableSent.add(context);
//                               continue;
//                               System.err.println("context:" + context);
//                               System.err.println("highlightedText:" + annotationPerContext.get(context));
//                               System.err.println("\n");
                           }else{
                               //collecting annotation pool for each attribute, for data augmentation
                               if(traintest.get("train").contains(doc)){
                                   int typecount = 0;
                                   String attri = "";
                                   for(NERToken token: annotation){
                                       if(token.nertag.contains("B-")){
                                          attri = token.nertag.replace("B-", ""); 
                                          typecount = typecount + 1;
                                       }
                                   }
                                   if(typecount==1){
                                       if(annotationPool.containsKey(attri)){
                                           annotationPool.get(attri).add(annotation);
                                       }else{
                                           List<List<NERToken>> lists = new ArrayList();
                                           lists.add(annotation);
                                           annotationPool.put(attri, lists);
                                       }
                                   }
                                   if(attri.contains("-name")||attri.contains("-value")){
                                       if(annotationPool_nameValue.containsKey(attri)){
                                           annotationPool_nameValue.get(attri).add(annotation);
                                       }else{
                                           List<List<NERToken>> lists = new ArrayList();
                                           lists.add(annotation);
                                           annotationPool_nameValue.put(attri, lists);
                                       }
                                       
                                   }
                               }
                           }
                           problematicAnnotation = false; 
                           if(traintest.get("train").contains(doc)){
                            for(NERToken token: annotation){     
                               if(token.nertag.contains("B-")){
                                   //generated instances
//                                   String attri = token.nertag.split("_")[0].replace("B-", "");
                                   String attri = token.nertag.replace("B-", "");
//                                   System.err.println(attri);
                                   int oldNum3 = stat_train.get(attri).get(3);
                                   stat_train.get(attri).set(3, oldNum3+ 1);
                               }
                           }
                            
                        }
                           for(NERToken token: annotation){     
                               if(token.nertag.contains("B-")){
                                   //generated instances
//                                   String attri = token.nertag.split("_")[0].replace("B-", "");
                                   String attri = token.nertag.replace("B-", "");
                                   int oldNum3 = stat.get(attri).get(3);
                                   stat.get(attri).set(3, oldNum3+ 1);
                               }
                           }
                           instanceLength.add(annotation.size());
                           instanceLengthDebug.put(annotation.size(), str);
                            if (traintest.get("train").contains(doc)) {
                                for (NERToken token : annotation) {
                                    sb1.append(doc.replace(" ", "_") + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                                }
                                sb1.append("\n");
                                instanceNum++;
                            } else {
                                for (NERToken token : annotation) {
                                    sb2.append(doc.replace(" ", "_") + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                                }
                                sb2.append("\n");
                                instanceNum++;
                            }
                        }

                    }
                }
                if(problematicAnnotation){
                    sb4.append("file:" + doc).append("\n");
                    sb4.append("context:" + context).append("\n");
                    sb4.append("highlightedText:" + annotationPerContext.get(context)).append("\n");
                    sb4.append("\n");
                    problematicAnnotationCount++;
                }
            }
//            System.err.print(matchedTableSent);
        //for table sentences, bring in all 'O' sentences
//            List<String> sents = generateTestingSentence(doc, testdir);
//            int tableSentCount = 0;
//            for(String sent: sents){
//                if(sent.contains("has a value of")&&!matchedTableSent.contains(sent)){
//                    Sentence tableSent = new Sentence(sent);
//                    List<Token> tokens_original = tableSent.tokens();
//                    tableSentCount++;
//                    if(tableSentCount>10) break;
//                    if(traintest.get("train").contains(doc)){ //training
//                        for(Token t: tokens_original){
//                            sb1.append(doc.replace(" ", "_") + "\t" + t.originalText() + "\t" + t.posTag() + "\t" + "O" + "\n");
//                        }
//                        sb1.append("\n");
//                    }else{ //testing
//                        for(Token t: tokens_original){
//                        sb2.append(doc.replace(" ", "_") + "\t" + t.originalText() + "\t" + t.posTag() + "\t" + "O" + "\n");
//                    }
//                        sb2.append("\n");
//                    }
//                     
//                }
//            }// add table sentences which do not have any annotations
        }
        //add augenentated annotations in the training file
//         for(String attribute: augmentAttri){
//augement strategy 1: upsampling
            for(String attribute: targetedAttri){
                if(annotationPool.containsKey(attribute)){
                    List<List<NERToken>> instances = annotationPool.get(attribute);
                    List<List<NERToken>> entities = getAnnotatedEntities(instances, attribute);
                    int added = 0;
                    if(instances.size()<200){
                        int repeat = 200/instances.size();
                        for(int i = 0; i<repeat; i++){
                            for(List<NERToken> instance: instances){
                                for (NERToken token : instance) {
                                sb1.append("augment" + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                            }
                            sb1.append("\n");
                            added  = added + 1;
                            if(instances.size() + added>200) break;
                            }
                        }
                    }
                   System.err.println(attribute + ":" + instances.size() + ":" + added);
                }
        }  

            
        for(String attribute: augmentAttri_namevalue){
                if(annotationPool_nameValue.containsKey(attribute)){
                    List<List<NERToken>> instances = annotationPool_nameValue.get(attribute);
                    int added = 0;
                    if(instances.size()<100){
                        int repeat = 200/instances.size();
                        for(int i = 0; i<repeat+1; i++){
                            for(List<NERToken> instance: instances){
                                for (NERToken token : instance) {
                                sb1.append("augment" + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                            }
                            sb1.append("\n");
                            added  = added + 1;
                            }
                        }
                    }
                   System.err.println(attribute + ":" + instances.size() + ":" + added);
                }            
        } 

//augement strategy 2: change context and annotated entity randomly
//            Random rand = new Random();
//            for(String attribute: targetedAttri){
//                if(annotationPool.containsKey(attribute)){
//                    List<List<NERToken>> instances = annotationPool.get(attribute);
//                    List<List<NERToken>> entities = getAnnotatedEntities(instances, attribute);
//                    int added = 0;
//                    int max = entities.size()-1;
//                    int min = 0;
//                    if(instances.size()<200){
//                        int repeat = 200/instances.size();
//                        for(int i = 0; i<repeat; i++){
//                            for(List<NERToken> instance: instances){
//                                for (NERToken token : instance) {
//                                    if(token.nertag.equalsIgnoreCase("O")){
//                                       sb1.append("augment" + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
//                                    }else if(token.nertag.contains("B-")&&token.nertag.contains(attribute)){
//                                        //random insert an annotated ner from other sentences
//                                        int randomNum = rand.nextInt((max - min) + 1) + min;      
//                                        List<NERToken> insert = entities.get(randomNum);
//                                        for(NERToken inserttoken: insert){
//                                             sb1.append("augment" + "\t" + inserttoken.word + "\t" + inserttoken.postag + "\t" + inserttoken.nertag.trim().replace(" ", "_") + "\n");
//                                        }
//                                    }else if(token.nertag.contains("I-")&&token.nertag.contains(attribute)){
//                                        continue; //neglect I-tag tokens
//                                    }else{//other entities
//                                       sb1.append("augment" + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
//                                    }
//                                }
//                            sb1.append("\n");
//                            added  = added + 1;
//                            if(instances.size() + added>200) break;
//                            }
//                        }
//                    }
//                   System.err.println(attribute + ":" + instances.size() + ":" + added);
//                }
//        }  
//
//            
//        for(String attribute: augmentAttri_namevalue){
//                if(annotationPool_nameValue.containsKey(attribute)){
//                    List<List<NERToken>> instances = annotationPool_nameValue.get(attribute);
//                    List<List<NERToken>> entities = getAnnotatedEntities(instances, attribute);
//                    int added = 0;
//                    int max = entities.size()-1;
//                    int min = 0;
//                   if(instances.size()<100){
//                        int repeat = 200/instances.size();
//                        for(int i = 0; i<repeat; i++){
//                                for(List<NERToken> instance: instances){
//                                for (NERToken token : instance) {
//                                    if(token.nertag.equalsIgnoreCase("O")){
//                                       sb1.append("augment" + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
//                                    }else if(token.nertag.contains("B-")&&token.nertag.contains(attribute)){
//                                        //random insert an annotated ner from other sentences
//                                        int randomNum = rand.nextInt((max - min) + 1) + min;      
//                                        List<NERToken> insert = entities.get(randomNum);
//                                        for(NERToken inserttoken: insert){
//                                             sb1.append("augment" + "\t" + inserttoken.word + "\t" + inserttoken.postag + "\t" + inserttoken.nertag.trim().replace(" ", "_") + "\n");
//                                        }
//                                    }else if(token.nertag.contains("I-")&&token.nertag.contains(attribute)){
//                                        continue; //neglect I-tag tokens
//                                    }else{//other entities
//                                       sb1.append("augment" + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
//                                    }
//                                }
//                            sb1.append("\n");
//                            added  = added + 1;
//                            if(instances.size() + added>200) break;
//                       }  
//                    }
//                }
//                   System.err.println(attribute + ":" + instances.size() + ":" + added);
//                }            
//        } //augment2
        
        
        writer1.write(sb1.toString());
        writer1.close();
        writer2.write(sb2.toString());
        writer2.close();
        writer4.write(sb4.toString());
        writer4.close();
        System.err.println("num of doc:" + count + ":" + allDocs.size());
        System.err.println("num of train doc:" + traintest.get("train").size());
        System.err.println("num of test doc:" + traintest.get("test").size());
        System.err.println("num of training+testing instances:" + instanceNum);
        System.err.println("num of HBCP annotated instances:" + instanceNum_annotate);
        System.err.println("num of problematic annotated instances:" + problematicAnnotationCount);
//generate real testing data
        for (String doc : traintest.get("test")) {
            List<String> sents = generateTestingSentence(doc, testdir);
            if(sents.isEmpty()){
                System.err.println(doc + ": no corresponding xml file");
                continue;
            }
//            FileWriter writer3 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/testfile/" + doc + ".txt"));
            FileWriter writer3 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/testfile_rank123/" + doc + ".txt"));
            StringBuffer sb3 = new StringBuffer();
            for (String sent : sents) {
                sb3.append(sent).append("\n");
            }
            writer3.write(sb3.toString());
            writer3.close();
        }
        for(String attri: stat.keySet()){
            System.err.println(attri + "\t" + stat.get(attri).get(0) + "\t" + stat.get(attri).get(1) + "\t" + stat.get(attri).get(2) + "\t" + stat.get(attri).get(3));
        }
        
        System.err.println("training data distribution");
        for(String attri: stat_train.keySet()){
            System.err.println(attri + "\t" + stat_train.get(attri).get(3));
        }
        
//        for(int i: instanceLength){
//            System.err.println(i);
//        }
        Collections.sort(instanceLength, Collections.reverseOrder()); 
//        System.err.println("longest instance:" + instanceLength.get(0));
//        System.err.println("isntanceLength:" + instanceLength);
//        for(int i=0; i<instanceLength.size(); i++){
//            if(instanceLength.get(i)>100){
//                System.err.println(instanceLengthDebug.get(instanceLength.get(i)));
//            }
//        }
//        System.err.println(instanceLengthDebug.get(instanceLength.get(0)));

    }
    
    public List<List<NERToken>> getAnnotatedEntities(List<List<NERToken>> instances, String attribute){
        List<List<NERToken>> entities = new ArrayList<>();
        for(List<NERToken> instance: instances){
            List<NERToken> ner = new ArrayList<>();
            for(NERToken token: instance){
                if(token.nertag.contains("B-")&&token.nertag.contains(attribute)){
                    if(ner.isEmpty()){
                        ner.add(token);
                    }else{
                        entities.add(ner);
                        ner = new ArrayList<>();
                        ner.add(token);
                    }
                }
                if(token.nertag.contains("I-")&&token.nertag.contains(attribute)){
                    ner.add(token);
                }
            }
            entities.add(ner);
        }
        return entities;
    }

    private List<NERToken> getAnnotationOnSent(Sentence sent, Set<String> annotationsWithType) throws IOException, Exception {
        List<Token> tokens_original = sent.tokens();
        List<NERToken> tokens = new ArrayList<>();
        for (Token tok : tokens_original) {
//            NERToken nertoken = new NERToken(tok.word(), tok.posTag(), "O");
            NERToken nertoken = new NERToken(tok.originalText(), tok.posTag(), "O");
            tokens.add(nertoken);
        }
        List<String> tokenStr = new ArrayList<>();
        for (NERToken token : tokens) {
            tokenStr.add(token.word);
        }
        //derive annotation
        Map<Integer, String> annotation = new TreeMap<>();
        for (String str : annotationsWithType) {
            String highlightedText = str.split("##")[0];
            String type = str.split("##")[1];
            annotateSentFromKB(tokenStr, Sets.newHashSet(highlightedText), type, annotation);
        }
        if (annotation.isEmpty()) {
            return null;
        }
        for (Integer index : annotation.keySet()) {
            String type = annotation.get(index).split("#")[1];
            int length = Integer.valueOf(annotation.get(index).split("#")[0]);
            tokens.get(index).setNER("B-" + type);
            for (int i = 1; i < length; i++) {
                tokens.get(index + i).setNER("I-" + type);
            }
        }
        return tokens;
    }

    private void annotateSentFromKB(List<String> tokens, Set<String> kb, String type, Map<Integer, String> annotation) {
        for (String entryStr : kb) {
            Sentence sent = new Sentence(entryStr);
            List<String> entry = new ArrayList<>();
            for (Token t : sent.tokens()) {
                entry.add(t.originalText());
            }
//            for (int i = 0; i < entryStr.split(" ").length; i++) {
//                entry.add(entryStr.split(" ")[i].toLowerCase());
//            }
            int index = Collections.indexOfSubList(tokens, entry);
            if (index > -1) {
                annotation.put(index, entry.size() + "#" + type);
            }
        }
    }

    private Boolean isContextFromTable(String context) {
        boolean res = false;
        Pattern p = Pattern.compile(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%).*?");
        Matcher m = p.matcher(context);
        int wordCount = 0;
        List<String> numbers = new ArrayList<>();
        for (String s : context.split(" ")) {
            if (s.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%).*?")) {
                numbers.add(s);
            }
        }
        if (numbers.size() > 0 && numbers.size() / (0.0 + context.split(" ").length - numbers.size()) > 0.5) {
            res = true;
        }
        if (context.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%) (\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%) (\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%).*?")) {
            res = true;
        }
        return res;
    }

    /**
     * Returns a list of documents paired with their annotations from a doc
     * index and a JSON annotation parser. Useful to quickly evaluate the
     * extractor with a default index/JSON.
     */
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
    
    public void count() throws IOException, Exception{
          Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        IndexManager index = getDefaultIndexManager(props);

        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser);
        int instanceNum_annotate = 0;
        for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> pairsPerDoc : groundTruth) {
            IndexedDocument doc = pairsPerDoc.getKey();
            Map<String, Set<String>> annotationPerContext = new HashMap<>();
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String context = cap.getContext();
                if (cap.getAttribute().getId().equalsIgnoreCase("5140146")) {
                    instanceNum_annotate++;
                }
            }
        }
        System.err.println(instanceNum_annotate);
    }
    
    
    public void tmp() throws IOException, Exception{
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        refParser.getAttributeValuePairs().byDoc();   
        for(String docname: refParser.getAttributeValuePairs().byDoc().keySet()){
            Collection<? extends ArmifiedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);
        }
    }
    
 
    /**
     * Returns a list of documents paired with their annotations from a doc
     * index and a JSON annotation parser. Useful to quickly evaluate the
     * extractor with a default index/JSON.
     */
    List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getGroundTruthForEvaluation_fromJson(
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

    List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getGroundTruthForEvaluation_fromJson_w_MinedOutcomevalues(
        JSONRefParser refParser
    ) throws IOException {
        AnnotationOutcomesMiner outcomeMiner = new AnnotationOutcomesMiner(Props.loadProperties());
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
            AttributeValueCollection<AnnotatedAttributeValuePair> annotations1 = outcomeMiner.withOtherOutcomeAndFollowupSeparate(new AttributeValueCollection<>(annotations));            
            AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(new AttributeValueCollection<>(annotations1));
//            res.add(Pair.of(docname, new ArrayList<>(annotations)));
            res.add(Pair.of(docname, new ArrayList<>(cleaned)));
        }
        return res;
    }

    
     
    
    
    /**
     * Returns a list of documents paired with their annotations from a doc
     * index and a JSON annotation parser. Useful to quickly evaluate the
     * extractor with a default index/JSON.
     */
    List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getGroundTruthForEvaluation_fromJson_LrecOpenAccess(
            IndexManager index,
            JSONRefParser refParser
    ) throws IOException {
        String openAccessJsonsPath = "data/lrec2020/openaccesspapers_extracted_humanreadable";
        Set<String> shortTitles = Arrays.stream(new File(openAccessJsonsPath).listFiles())
                .map(File::getName)
                .map(filename -> filename.replaceAll("\\.json", ""))
                .collect(Collectors.toSet());
         Cleaners cleaners = new Cleaners(Props.loadProperties());
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> res = new ArrayList<>();
        for (String docname : refParser.getAttributeValuePairs().byDoc().keySet()) {
            Collection<? extends AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);
            // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
            if (annotations == null) {
                System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                continue;
            }
            Optional<JSONRefParser.PdfInfo> pdfInfo = refParser.getDocInfo(docname);
            if (pdfInfo.isPresent()) {
                String shortTitle = pdfInfo.get().getShortTitle();
                if(!shortTitles.contains(shortTitle)) continue;
            }
            AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(new AttributeValueCollection<>(annotations));
            res.add(Pair.of(docname, new ArrayList<>(cleaned)));
        }
        return res;
    }
    
    //30th, 1st, 2nd, 3rd
    public String splitNumberth(String originalSent){
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*? (\\d+th) .*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList();
        while(m.find()){
            candidates.add(m.group(1));
         }
        for(String cand: candidates){
            String newStr  = cand.replace("th", "") + " " + "th";
            newSent = newSent.replace(cand, newStr);
        }
        newSent = newSent.replace("1st", "1 st");
        newSent  = newSent.replace("2nd", "2 nd");
        newSent  = newSent.replace("3rd", "3 rd");
        return newSent;
    }
    
    //six-week, five-session, one-time, xxx-based, twice-weekly, two-month
    public String splitDashBetweenTokens(String originalSent){
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*?(one|two|three|four|five|six|seven|eight|nine|twice)-(month|months|monthly|week|weeks|weekly|day|days|session|sessions|time).*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList();
        while(m.find()){
            candidates.add(m.group(1) + "-" + m.group(2));
         }
        for(String cand: candidates){
            String newStr  = cand.split("-")[0] + " " + "-" + " "+ cand.split("-")[1];
            newSent = newSent.replace(cand, newStr);
        }
        return newSent;
    }
    


    //3-months, 2.5-4.5
    public String splitDashBetweenNumbers(String originalSent){
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)-(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList();
        while(m.find()){
            candidates.add(m.group(1) + "-" + m.group(2));
         }
        for(String cand: candidates){
            String newStr  = cand.split("-")[0] + " " + "-" + " "+ cand.split("-")[1];
            newSent = newSent.replace(cand, newStr);
        }
        return newSent;
    }

    //34/56
    public String splitSlashBetweenNumbers(String originalSent){
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)/(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList<>();
        while(m.find()){
            candidates.add(m.group(1) + "/" + m.group(2));
         }
        for(String cand: candidates){
            String newStr  = cand.split("/")[0] + " " + "/" + " "+ cand.split("/")[1];
            newSent = newSent.replace(cand, newStr);
        }
        return newSent;
    }
    
    public void generateTestingFileForPhysicalActivity(String parsedPDFDir) throws IOException, Exception{
        File dir = new File(parsedPDFDir);
        for (File jsonfile : dir.listFiles()) {
            List<String> sents = generateTestingSentence(jsonfile);
            if(sents.isEmpty()){
                System.err.println(jsonfile.getName() + ": no corresponding xml file");
                continue;
            }
            FileWriter writer3 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp/rank123Exp/testfile_physicalActivity/" + jsonfile.getName() + ".txt"));
            StringBuffer sb3 = new StringBuffer();
            for (String sent : sents) {
                sb3.append(sent).append("\n");
            }
            writer3.write(sb3.toString());
            writer3.close();
        }
        
    }
        



    public static void main(String[] args) throws IOException, Exception {
        GenerateTrainingData_NameAsCategory generatorTrainTest = new GenerateTrainingData_NameAsCategory();
//        String s1 = "6 months Web 27.4 PTC 30.6 1.17 (0.86, 1.58)";
//        String s2 = "Model 1: Quit at 12 weeks 11 (28.2%) 18 (43.9%) 2.0 (0.8, 5.1) 2.2 b (0.8, 6.2) Model 2: Quit at 26 weeks 7 (18.0%) 15 (36.6%) 2.6 (0.9, 7.4) 3.6 c (1.1, 11.4) NRT vs. NRT plus hypnotherapy";
//        System.err.println(generatorTrainTest.isContextFromTable(s1));
//        System.err.println(generatorTrainTest.isContextFromTable(s2));

         generatorTrainTest.generateTrainTestData_BIO_Tagging();

//mention classification exp
//        generate candidate mentions
//          generatorTrainTest.generateTestingData_mention_classification();
 //generate mention classification data
//          generatorTrainTest.generateTrainingData_mention_classification();


//        generatorTrainTest.generateTestingFileForPhysicalActivity("/Users/yhou/git/hbcp/data/Physical_Activity_extracted");
        
//        generatorTrainTest.count();

//          generatorTrainTest.generateTestingSentence("Abroms 2008.pdf");
//        String text = "We briefly reviewed some of the practicalities of stopping; and we said that although it might be hard, if anyone really wanted to stop, he could.";
////        String text = "Figure 2 illustrates the 24-hr point-prevalence outcome data for the two treatment conditions at baseline and at 12-month and 24-month follow-ups.";
//        Sentence sent = new Sentence(generatorTrainTest.splitDashBetweenNumbers(text));
//          
//          Sentence sent = new Sentence("Conﬂict of interest statement None declared.");
//          Sentence sent = new Sentence("cognitive behaviour therapy (MI/CBT)");
//          
//          for(Token t: sent.tokens()){
//              System.err.println(t.originalText());
//          }
          
//          System.err.println(generatorTrainTest.splitNumberth("was the 30th day after the quit day, the 3rd was at the 90th day (1st follow-up test), and the 4th was"));
//          System.err.println(generatorTrainTest.splitDashBetweenTokens("was 7-days twice-weekly seven-week one-time the 30th day after the quit day, the 3rd was at the 90th day (1st follow-up test), and the 4th was"));

//          generatorTrainTest.generateTestingSentence("Abroms 2008.pdf");
//        String text = "We briefly reviewed some of the practicalities of stopping; and we said that although it might be hard, if anyone really wanted to stop, he could.";
//        String text = "Of the brochure and tailored information group 85% (546/642) and 84% (538/642)";
//        Sentence sent1 = new Sentence(generatorTrainTest.splitSlashBetweenNumbers(text));
//        String text = "Analysed (N=451)";
//        Sentence sent1 = new Sentence(text);
//          for(Token t: sent1.tokens()){
//              System.err.println(t.originalText());
//          }
//            String annotation = "one-session.";
//               if(annotation.matches("[a-zA-Z] .*?")){
//                    annotation = annotation.substring(2);
//                }
//                if(annotation.matches(".*?\\.")){
//                    annotation = annotation.substring(0, annotation.length()-1).trim();
//                }
//            System.err.println(annotation);
            
//fix partial annotation errors
//                String context = "The brand fading program provided 5-month all treatment bytelephone contact.";
////                String annotation = "ent bytelep";
//                String annotation = "5";
//                if(context.replace("\n", " ").contains(annotation)){
//                    int start = context.indexOf(annotation);
//                    int end = context.indexOf(annotation) + annotation.length()-1;
//                    System.err.println(context.charAt(start));
//                    System.err.println(context.charAt(end));
//                    while(!Character.isWhitespace(context.charAt(start))){
//                        if(start==0) break;
//                        start = start - 1;
//                    }
//                    while(!Character.isWhitespace(context.charAt(end))&&context.charAt(end)!='-'){
//                        end = end + 1;
//                        if(end==context.length()) break;
//                    }
//                    String newAnnotation = context.substring(start, end);
//                    System.err.println(newAnnotation.trim());
//                }
          
//          generatorTrainTest.generateTrainTestData_ArmAssociation();

//          generatorTrainTest.generateTrainingTestingFiles();
    }

}

