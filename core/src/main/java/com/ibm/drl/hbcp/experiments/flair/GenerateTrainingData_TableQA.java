/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.experiments.tapas.FlairTableInstancesGenerator;
import com.ibm.drl.hbcp.experiments.tapas.FlairTableInstancesGenerator.AnnotatedTable;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 *
 * @author yhou
 */
public class GenerateTrainingData_TableQA {

    //3-months, 2.5-4.5
    public String splitDashBetweenNumbers(String originalSent) {
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)-(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList<>();
        while (m.find()) {
            candidates.add(m.group(1) + "-" + m.group(2));
        }
        for (String cand : candidates) {
            String newStr = cand.split("-")[0] + " " + "-" + " " + cand.split("-")[1];
            newSent = newSent.replace(cand, newStr);
        }
        return newSent;
    }

    //34/56
    public String splitSlashBetweenNumbers(String originalSent) {
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)/(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList<>();
        while (m.find()) {
            candidates.add(m.group(1) + "/" + m.group(2));
        }
        for (String cand : candidates) {
            String newStr = cand.split("/")[0] + " " + "/" + " " + cand.split("/")[1];
            newSent = newSent.replace(cand, newStr);
        }
        return newSent;
    }

    public String splitNumberth(String originalSent) {
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*? (\\d+th) .*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList<>();
        while (m.find()) {
            candidates.add(m.group(1));
        }
        for (String cand : candidates) {
            String newStr = cand.replace("th", "") + " " + "th";
            newSent = newSent.replace(cand, newStr);
        }
        newSent = newSent.replace("1st", "1 st");
        newSent = newSent.replace("2nd", "2 nd");
        newSent = newSent.replace("3rd", "3 rd");
        return newSent;
    }

    //six-week, five-session, one-time, xxx-based, twice-weekly, two-month
    public String splitDashBetweenTokens(String originalSent) {
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*?(one|two|three|four|five|six|seven|eight|nine|twice)-(month|months|monthly|week|weeks|weekly|day|days|session|sessions|time).*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList<>();
        while (m.find()) {
            candidates.add(m.group(1) + "-" + m.group(2));
        }
        for (String cand : candidates) {
            String newStr = cand.split("-")[0] + " " + "-" + " " + cand.split("-")[1];
            newSent = newSent.replace(cand, newStr);
        }
        return newSent;
    }

    private List<NERToken> getAnnotationOnSent(Sentence sent, Set<String> annotationsWithType) throws IOException, Exception {
        List<Token> tokens_original = sent.tokens();
        List<NERToken> tokens = new ArrayList();
        for (Token tok : tokens_original) {
//            NERToken nertoken = new NERToken(tok.word(), tok.posTag(), "O");
            NERToken nertoken = new NERToken(tok.originalText(), tok.posTag(), "O");
            tokens.add(nertoken);
        }
        List<String> tokenStr = new ArrayList();
        for (NERToken token : tokens) {
            tokenStr.add(token.word);
        }
        //derive annotation
        Map<Integer, String> annotation = new TreeMap();
//        System.err.println(sent + ":" + annotationsWithType);
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
            List<String> entry = new ArrayList();
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
    
    public void generateTestingData(Map<String, List<AnnotatedTable>> tablesDoc) throws IOException, Exception {
        for(String doc: tablesDoc.keySet()){
            int tableid = 0;
            for(AnnotatedTable table: tablesDoc.get(doc)){
                System.err.println(doc + "--" + tableid);
                FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp_tableqa/test/" + doc + "_" + "table" + tableid));
                StringBuffer sb1 = new StringBuffer();
                for(TableValue cell: table.getCells()){
                    sb1.append(cell.toText()).append("\n");
                }
                writer1.write(sb1.toString());
                writer1.close();
                tableid ++;
            }
        }
    }

    public void generateConllBIOData(String filename, Map<String, List<AnnotatedTable>> tablesDoc) throws IOException, Exception {
        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "../flairExp_tableqa/" + filename));
        StringBuffer sb1 = new StringBuffer();
        Pattern longfollowup = Pattern.compile(".*? (\\d+)(-month | mo |-Month| Mo | week| Week| months| Months| wk |-wk | week |-week ).*?");
        for (String doc : tablesDoc.keySet()) {
            int tableid = 0;
            for (AnnotatedTable table : tablesDoc.get(doc)) {
                Map<String, Set<String>> annotationPerContext = new HashMap<>();
                Map<String, Set<String>> negative_annotationPerContext = new HashMap<>();
                String longestfollowup = "";
                Set<String> table_armnames = new HashSet();
                for (FlairTableInstancesGenerator.CellWithAttribute positive : table.getPositives()) {
                    String attributeName = positive.getAttribute().getName();
                    String attributeValue = positive.getCell().getValue();
                    //arm name and longest follow up are not accurate
                    if(attributeName.equalsIgnoreCase("Arm name")) continue;
                    if(attributeName.equalsIgnoreCase("Longest follow up")) continue;
                    String context = positive.getCell().toText();
                    Set<String> anno = Sets.newHashSet(attributeValue + "##" + attributeName);
                    annotationPerContext.put(context, anno);
                    if (attributeName.equalsIgnoreCase("Longest follow up")) {
                        longestfollowup = attributeValue;
                    }
                    //context should contain attribute value and armname
                    if (positive.getAnnotation() != null) {
                        List<String> armnames = positive.getAnnotation().getArm().getAllNames();
                        table_armnames.addAll(armnames);
                        for (String armname : armnames) {
                            if(armname.isEmpty() || armname.length()<2) continue;
                            if (context.contains(armname)) {
                                annotationPerContext.get(context).add(armname + "##" + "Arm name");
                                break;
                            }
                        }
                    }
                }
                //for outcome context, we also try to find whether it contains longest follow up
                for (String context : annotationPerContext.keySet()) {
                    String anno = "";
                    for(String s: annotationPerContext.get(context)){
                        anno = anno + " " + s;
                    }
                    if (anno.contains("Outcome value")) {
//                        if (!longestfollowup.isEmpty() && context.contains(longestfollowup)) {
//                            annotationPerContext.get(context).add(longestfollowup + "##" + "Longest follow up");
//                        }
                    //current longest follow-up is not accurate, automatically derived from the sentence itself
                        Matcher m = longfollowup.matcher(context);
                        if (m.find()) {
                            String longfollowupannotation = m.group(1) + m.group(2);
                            annotationPerContext.get(context).add(longfollowupannotation + "##" + "Longest follow up");
                        }
                    }
                    
                }
                //generate all annotations for the current table
                for (String context : annotationPerContext.keySet()) {
                    if(annotationPerContext.get(context).isEmpty() || annotationPerContext.get(context)==null) 
                        continue;
                    Set<String> annotations = annotationPerContext.get(context);
                    String context1 = context;
                    context1 = splitDashBetweenNumbers(context1);
                    context1 = splitSlashBetweenNumbers(context1);
                    context1 = splitNumberth(context1);
                    context1 = splitDashBetweenTokens(context1);
                    if(context1.isEmpty()) continue;
                    Sentence sent = new Sentence(context1);
                    List<NERToken> annotation = getAnnotationOnSent(sent, annotations);
                    if (annotation != null) {
                        for (NERToken token : annotation) {
                            sb1.append(doc.replace(" ", "_") + "##" + "table_" + tableid + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                        }
                        sb1.append("\n");
                    }
                }
                //add negative training instances, but these negative training instances can contain the arm name annotations 
                int count = 0;
                int count1 = 0;
                for(TableValue cell: table.getCells()){
                    String cellsent = cell.toText();
                    if(cellsent.isEmpty() || cellsent.split(" ").length < 5) continue;
                    if(!annotationPerContext.keySet().contains(cellsent)){
                        boolean armnameMatch = false;
                        for (String armname : table_armnames) {
                            if(armname.isEmpty() || armname.length()<2) continue;
                            if (cellsent.contains(armname)) {
                                Set<String> anno = Sets.newHashSet(armname + "##" + "Arm name");
                                negative_annotationPerContext.put(cellsent, anno);
                                armnameMatch = true;
                                break;
                            }
                        }
                        if(!armnameMatch){
                            Sentence tableSent = new Sentence(cellsent);
                            List<Token> tokens_original = tableSent.tokens();
                            for(Token t: tokens_original){
                                sb1.append(doc.replace(" ", "_") + "##" + "table_" + tableid+ "\t" + t.originalText() + "\t" + t.posTag() + "\t" + "O" + "\n");
                            }
                            sb1.append("\n");
                        }
                        count++;
                        if(count>=5) break;
                        
                    }
                }
                //generate all negative annotations for the current table winch contain arm names
                for (String context : negative_annotationPerContext.keySet()) {
                    if(negative_annotationPerContext.get(context).isEmpty() || negative_annotationPerContext.get(context)==null) 
                        continue;
                    Set<String> annotations = negative_annotationPerContext.get(context);
                    String context1 = context;
                    context1 = splitDashBetweenNumbers(context1);
                    context1 = splitSlashBetweenNumbers(context1);
                    context1 = splitNumberth(context1);
                    context1 = splitDashBetweenTokens(context1);
                    if(context1.isEmpty()) continue;
                    Sentence sent = new Sentence(context1);
                    List<NERToken> annotation = getAnnotationOnSent(sent, annotations);
                    if (annotation != null) {
                        for (NERToken token : annotation) {
                            sb1.append(doc.replace(" ", "_") + "##" + "table_" + tableid + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                        }
                        sb1.append("\n");
                        count1++;
                        if(count1>=5)break;
                    }
                }
                
            tableid ++;
            }
        }
        writer1.write(sb1.toString());
        writer1.close();
    }

    public static void main(String[] args) throws Exception {
        GenerateTrainingData_TableQA tableQA = new GenerateTrainingData_TableQA();
        FlairTableInstancesGenerator gen = new FlairTableInstancesGenerator();
        Map<String, Map<String, List<AnnotatedTable>>> splits = gen.getAnnotatedTablesSplits();
        Map<String, List<AnnotatedTable>> train = splits.get("train");
        Map<String, List<AnnotatedTable>> dev = splits.get("dev");
        Map<String, List<AnnotatedTable>> test = splits.get("test");
        tableQA.generateConllBIOData("train_plusnegative.csv", train); 
        tableQA.generateConllBIOData("test_plusnegative.csv", test);
//        tableQA.generateTestingData(test);
        
//        for (String doc : train.keySet()) {
//            List<AnnotatedTable> tables = train.get(doc);
//            for (AnnotatedTable table : tables) {
//                for (FlairTableInstancesGenerator.CellWithAttribute positive : table.getPositives()) {
//                    System.out.println(positive.getAttribute() + " -> " + positive.getAnnotation());
//                    if (positive.getAnnotation() != null) {
//                        System.out.println(positive.getAnnotation().getArm().getAllNames());
//                    }
//                    System.out.println(positive.getCell().toText());
//                    System.out.println(positive.getCell().getValue());
//                }
//            }
//        }
    }

}
