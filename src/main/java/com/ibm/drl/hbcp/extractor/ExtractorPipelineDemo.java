/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.core.attributes.Arm;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author francesca
 */
public class ExtractorPipelineDemo {
    
    
    
    private static List<Arm> identifyArms() {
        //does something to return all the arms in the dataset
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }
    
   // private static void assignArmToEntity(Arm a, AttributeValuePair someEntity) {
   //     throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
   // }

     private static void evaluate() {
         //does something to evaluate : comparing a Arm-ExtractedAttributeValuePair with gold Arm -AttributeValuepair
        
        Evaluation ev = new Evaluation();
      //  ev.run();
        
    }
     
     
    public static void main(String[] args) throws Exception {
        //String propFile="init.properties";
        if (args.length == 0) {
            args = new String[1];
            System.err.println("Usage: java PredictionPipelineDemo <prop-file>");
            args[0] = "init.properties";
        }
        
       // extract entities 
       // do something
       
       //extract arms with Arm identifier
       
       List <Arm> allArms= new ArrayList();
       allArms= identifyArms();
       
       for (Arm a : allArms){
        //assignArmToEntity(a,someEntity);
       }
       
       
       //evaluate
       evaluate();
        
        
}

    
   
    
    
    
}
