/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

/**
 *
 * @author yhou
 */
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */


import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswer;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswers;
import com.ibm.drl.hbcp.parser.CodeSetTree;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import net.bytebuddy.asm.Advice;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;


/**
 *
 * @author yhou
 */
public class ArmsIdentifier extends InformationUnit{
    //outome_value attributeID
    public static final String ATTRIB_ID = "5140146";
    String currentRetrievedText;
    List<String> armCandidatesInCurrentRetrievedText;
    List<Pattern> armPattern;

    @Override
    public int numWanted() {
        return 50; //To change body of generated methods, choose Tools | Templates.
    }
    
    
    public ArmsIdentifier(InformationExtractor extractor, String contentFieldName, AttributeType type) {
        super(extractor, contentFieldName, type);
        //query = "(participant OR smoker OR people OR person) AND (age OR year OR old)";
        query = "group OR groups";
        keepUnAnalyzedToken = false;
        currentRetrievedText = "";
        armPattern = new ArrayList();
        armPattern.add(Pattern.compile(" (in) the (.*?) group"));
        armPattern.add(Pattern.compile("\\.(In) the (.*?) group"));
        armPattern.add(Pattern.compile(" (the) (\\w+) group"));
        armPattern.add(Pattern.compile("\\.(The) (.*?) group"));
//        armPattern.add(Pattern.compile(" (a|an) (.*?) group"));
    }

    @Override
    String preProcess(String window) {
        currentRetrievedText = window;
        armCandidatesInCurrentRetrievedText = new ArrayList();
        for (Pattern p : armPattern) {
            Matcher m = p.matcher(currentRetrievedText);
            while (m.find()) {
                String predictionStr = m.group(2);
                if(window.contains(predictionStr + " groups")) continue;
                if(predictionStr.split(" ").length > 5) continue;
                if(predictionStr.contains(", ")) continue;
                if(predictionStr.contains(". ")) continue;
                predictionStr = predictionStr.replace("- ", "");
                armCandidatesInCurrentRetrievedText.add(predictionStr.toLowerCase());
                System.err.println(window + ":" + predictionStr);
            }
        }
        return window;
    }    

    @Override
    public void setProperties() {

    }
    
        /* Note: The purpose of this function is to make sure that the correct answer
    is set correctly (18) when the token is the case of "18 25"
     */
    @Override
    public void construct(String window, Query q, float sim) {
        this.queryTerms = extractQueryTerms(q);

        String pp_window = preProcess(window);

        CandidateAnswers keys = new CandidateAnswers();
        

        if(armCandidatesInCurrentRetrievedText.size()>0){
            String arms = "";
            String firstArm = armCandidatesInCurrentRetrievedText.get(0);
            for(String s: armCandidatesInCurrentRetrievedText){
                arms = arms + ":::" + s;
            }
            mostLikelyAnswer = new CandidateAnswer(arms, window.indexOf(firstArm), window);
        }else{
            mostLikelyAnswer = null;
        }
    }

    @Override
    public String getName() {
        return "Arms";
    }

    @Override
    public void appendFields(Document doc) {
        doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
                String.valueOf(ATTRIB_ID),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }
    
    private boolean comparedPredicted2GoldAnnotation(String predict, String goldAnnotation){
        boolean val = false;
        goldAnnotation = goldAnnotation.toLowerCase().replace("group", "").trim();
        if(predict.equalsIgnoreCase(goldAnnotation)) return true;
//        if(predict.equalsIgnoreCase(goldAnnotation.replace("-", " "))) return true;
        if(goldAnnotation.matches(".*?\\(.*?\\).*?")){
            String goldAnnotation1 = goldAnnotation.split("\\(")[1];
            goldAnnotation1 = goldAnnotation1.split("\\)")[0].trim();
            if(predict.equalsIgnoreCase(goldAnnotation1)) return true;
            String goldAnnotation2 = goldAnnotation.split("\\(")[0].trim();
            if(predict.equalsIgnoreCase(goldAnnotation2)) return true;
        }
        return val;
    }
    
    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc, boolean armification) {
        Set<String> annotatedArms = null;
        if (gt.getNode(ATTRIB_ID)!=null) {
            annotatedArms = gt.getNode(ATTRIB_ID).getAnnotatedArms(docName);
        }
        Set<String> prediction = new HashSet();
        if(predicted!=null){
            for(String s: predicted.mostLikelyAnswer.getKey().split(":::")){
                if(!s.equalsIgnoreCase(":::")&&s.split(" ").length<=5)
                    prediction.add(s.toLowerCase());
            }
        }
        logger.info("arm annotation:" + annotatedArms);
        if (predicted == null) {
            if (annotatedArms!=null) {
                rc.fn = rc.fn + annotatedArms.size(); // annotated but we predict null
            }
            return;  // no point of going further.. sine there's nothing we predicted
        } else if (annotatedArms==null) {
            rc.fp = rc.fp + prediction.size(); // we predicted null when there was a value
            return;
        }

        for(String arm: annotatedArms){
            boolean armIsAmongPrediction = false;
            for(String s1: prediction){
                if(comparedPredicted2GoldAnnotation(s1, arm)){
                    rc.tp++;
                    armIsAmongPrediction = true;
                    break;
                }
            }
            if(armIsAmongPrediction){
                rc.tp++;
            }else{
                rc.fn++;
            }
        }
        
        for(String predictArm: prediction){
            boolean predictArmIsAmongAnnotation = false;
            for(String s: annotatedArms){ 
                if(comparedPredicted2GoldAnnotation(predictArm, s)){ 
                   predictArmIsAmongAnnotation = true;
                   break;
                }
            }
            if(!predictArmIsAmongAnnotation)
                rc.fp++;
        }

    }

    public void compareWithRef1(CodeSetTree gt, InformationUnit predicted, RefComparison rc, boolean armification, Map<String, Set<String>> armInfo) {
        Set<String> annotatedArms = null;
        if (armInfo.containsKey(docName)) {
            annotatedArms = armInfo.get(docName);
        }
        Set<String> prediction = new HashSet();
        if(predicted!=null){
            for(String s: predicted.mostLikelyAnswer.getKey().split(":::")){
                if(!s.equalsIgnoreCase(":::")&&s.split(" ").length<=5)
                    prediction.add(s.toLowerCase());
            }
        }
        logger.info("arm annotation:" + annotatedArms);
        if (predicted == null) {
            if (annotatedArms!=null) {
                rc.fn = rc.fn + annotatedArms.size(); // annotated but we predict null
            }
            return;  // no point of going further.. sine there's nothing we predicted
        } else if (annotatedArms==null) {
            rc.fp = rc.fp + prediction.size(); // we predicted null when there was a value
            return;
        }

        for(String arm: annotatedArms){
            boolean armIsAmongPrediction = false;
            for(String s1: prediction){
                if(comparedPredicted2GoldAnnotation(s1, arm)){
                    rc.tp++;
                    armIsAmongPrediction = true;
                    break;
                }
            }
            if(armIsAmongPrediction){
                rc.tp++;
            }else{
                rc.fn++;
            }
        }
        
        for(String predictArm: prediction){
            boolean predictArmIsAmongAnnotation = false;
            for(String s: annotatedArms){ 
                if(comparedPredicted2GoldAnnotation(predictArm, s)){ 
                   predictArmIsAmongAnnotation = true;
                   break;
                }
            }
            if(!predictArmIsAmongAnnotation)
                rc.fp++;
        }

    }


    
    
}

