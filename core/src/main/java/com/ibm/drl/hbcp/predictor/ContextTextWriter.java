/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.data.DataSplitter;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.util.Props;

/**
 * This writes out the context text (word sequence) for each attribute occurrence;
 * which we later use to get the vectors from Bio-BERT (using HuggingFace).
 * 
 * @author debforit
 */
public class ContextTextWriter extends PredictionWorkflow {

    public static void main(String[] args) throws IOException {
        
        Properties props = Props.loadProperties();
//        final Analyzer analyzer = new StandardAnalyzer();
        
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new JSONRefParser(props).getAttributeValuePairs();
        DataSplitter splitter = new TrainTestSplitter(1);
        ContextTextWriter cw = new ContextTextWriter(annotations, annotations, splitter, props);
        
        List<String> docNames = cw.docnamesTrainTest.getLeft();
        docNames.addAll(cw.docnamesTrainTest.getRight());
        
        String contextDumpFile = "prediction/biobert/context.txt";
        FileWriter fw = new FileWriter(contextDumpFile);
        BufferedWriter bw = new BufferedWriter(fw);
        
        AttributeValueCollection<ArmifiedAttributeValuePair> allAVPs = cw.getTrainAVPs(); // set ratio=1
        
        for (String docName: docNames) {
            List<DataInstance> perDocList = DataInstance.getInstancesForDoc(allAVPs, docName);
            
            for (DataInstance dataInstance: perDocList) {
                AttributeValueCollection<ArmifiedAttributeValuePair> x = dataInstance.getX();
                
                for (ArmifiedAttributeValuePair avp: x.getAllPairs()) {                
                    boolean isIntervention = avp.getAttribute().getType()== AttributeType.INTERVENTION;
                    /*
                    if (!isIntervention)
                        continue;
                    */
                    String textValue = isIntervention?
                        ((NormalizedAttributeValuePair)((NormalizedAttributeValuePair)avp).getOriginal()).getOriginal().getValue():
                        avp.getValue();
                    
                    String contextText = DataInstanceManager.selectWindowOfWords(avp.getContext(), textValue);
                    if (contextText.length()==0)
                        continue;
                    
                    bw.write(contextText);
                    bw.newLine();
                    
                    /*
                    if (!isIntervention) {
                        textValue = avp.getContext();                    
                        analyzedContext = PaperIndexer.analyze(analyzer, textValue);
                        bw.write(analyzedContext);
                        bw.newLine();
                    }
                    */
                }
            }
        }
        
        bw.close();
        fw.close();
    }

    public ContextTextWriter(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values, AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations, DataSplitter splitter, Properties props) throws IOException {
        super(values, annotations, splitter, props);
    }
}
