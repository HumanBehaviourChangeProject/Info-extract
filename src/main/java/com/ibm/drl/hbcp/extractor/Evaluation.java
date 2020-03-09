/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 *
 * @author francesca
 */
class Evaluation {

    static AttributeValueCollection <AnnotatedAttributeValuePair> annotatedAVPs;
    
    static void loadGroudTruth() throws IOException{
        String fileToParse = "data/jsons/Smoking_AllAnnotations_01Apr19.json";
        JSONRefParser refParser = new JSONRefParser(new File(fileToParse));
        annotatedAVPs= refParser.getAttributeValuePairs();
        
         
    }
    
    private static void runArmifiedEvaluation(List<ArmifiedAttributeValuePair> extractedArmifiedAVPs) {
    // take the extracted attribute value pairs with arm associated and compare
    RefComparison scores = new RefComparison();
    // check each extracted armified AVP:
        for (ArmifiedAttributeValuePair armifiedAVP : extractedArmifiedAVPs){
            Arm extractedArm= armifiedAVP.getArm();
            Attribute extractedA= armifiedAVP.getAttribute();
            String extractedValue= armifiedAVP.getValue();
            System.out.println("Now checking: "+extractedArm+" "+extractedA.getName()+" "+ extractedValue+" "+ armifiedAVP.getDocName());
            for (ArmifiedAttributeValuePair annotatedAVP : annotatedAVPs){
               //TODO check why armifiedAVP.equals(annotatedAVP) did not work!!
                if //(armifiedAVP.equals(annotatedAVP) ||extractedArm.equals(annotatedAVP.getArm()))
                        (extractedValue.equalsIgnoreCase(annotatedAVP.getValue()) &&
                        annotatedAVP.getArm().equals(extractedArm) &&
                        extractedA.equals(annotatedAVP.getAttribute()))
                        {
                        System.out.println(">>>>"+annotatedAVP.getAttribute().getId()+" "+annotatedAVP.getArm().toString()+" "+annotatedAVP.getValue()+" "+annotatedAVP.getDocName());
                        System.out.println("Got it!!!!");
                        scores.tp++;
                        break;
                }
                else
                    if (    annotatedAVP.getArm().getId().equalsIgnoreCase("0") && !(extractedArm.getId().equalsIgnoreCase("0")) &&
                            extractedValue.equalsIgnoreCase(annotatedAVP.getValue()) &&
                            extractedA.equals(annotatedAVP.getAttribute()))
                            scores.fp++;   
                    //TODO check if you need to break... related to the fact that Attribute + value is unique. If it is I can break
                    
                else 
                    if (    !(annotatedAVP.getArm().getId().equalsIgnoreCase("0")) && extractedArm.getId().equalsIgnoreCase("0") &&
                            extractedValue.equalsIgnoreCase(annotatedAVP.getValue()) &&
                            extractedA.equals(annotatedAVP.getAttribute()))
                            scores.fn++;   
                     //TODO check if you need to break... related to the fact that Attribute + value is unique. If it is I can break
                else
                        scores.tn++;
                
            }
        }
        
        float precision= scores.computePrecisionVal();
        float recall= scores.computeRecallVal();
        float fscore= scores.computeFscoreVal();
        
        System.out.println("TP: "+ scores.tp+"\n"
                 +"FP: "+ scores.fp+"\n"
                 +"FN "+ scores.fn +"\n"     
                 +"TN "+ scores.tn  
                 );
        
        System.out.println("Precision: "+ precision+"\n"
                 +"Recall: "+ recall+"\n"
                 +"Fscore: "+ fscore     
                 );

    }
    
    
    public static void main(String[] args) throws Exception {
       loadGroudTruth();
     
       
       Attribute[] attributes = {
            new Attribute("5140146", AttributeType.OUTCOME_VALUE, "pippo"),
            new Attribute("3675685", AttributeType.INTERVENTION, "pippo"),
            new Attribute("5579728", AttributeType.POPULATION, "pippo"),
           
           
           
    };
       Arm arm1 = new Arm("242", "Revised contingent vouchers");
       Arm arm2 = new Arm("423", "Intervention group (IG)");
       Arm arm3= new Arm("517", "Motivationally enhanced phone counselling");
     
      
       List<ArmifiedAttributeValuePair> extractedArmifiedAVP = Lists.newArrayList(
           
            new ArmifiedAttributeValuePair(attributes[0], "17.9%", "Higgins 2014 (c) primary paper.pdf", arm1),
            new ArmifiedAttributeValuePair(attributes[1], "relaxation", "Froelicher 2004b.pdf", arm2),
            new ArmifiedAttributeValuePair(attributes[2], "Married/living with partner43.5", "McClure 2005.pdf", arm3)
            
            );
   
      runArmifiedEvaluation(extractedArmifiedAVP);
    }
    
}
