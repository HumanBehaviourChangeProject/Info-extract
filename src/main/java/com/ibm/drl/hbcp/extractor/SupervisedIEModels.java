/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.nb.DPAttribNBClassifier;

import java.io.FileInputStream;
import java.io.ObjectInputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Properties;

import static com.ibm.drl.hbcp.extractor.InformationExtractor.logger;

/**
 * Use this class to:
 * 1. Batch run supervised IE considering the whole collection
 * as a training set.
 * 2. Save the model as serialized file on disk
 * 3. Provide methods to load the models to be called from the API flow.
 * 
 * @author dganguly
 */
public class SupervisedIEModels {
    InformationExtractor extractor;
    List<InformationUnit> bcts;
    HashMap<String, SupervisedIEModel> models;
    Properties prop;
    //String basePath;
    
    public SupervisedIEModels(String propFile) throws Exception {
        extractor = new InformationExtractor(propFile);
        this.prop = extractor.prop;
        
        InformationExtractorFactory ieFactory = new InformationExtractorFactory(extractor);        
        bcts = ieFactory.createSupervisedIUnits();
        
        //basePath = prop.getProperty("models.dir");
    }
    
    public SupervisedIEModels(InformationExtractor extractor, List<InformationUnit> bcts) {
        this.extractor = extractor;
        this.bcts = bcts;
        
        this.prop = extractor.prop;
        
        //basePath = this.getClass().getClassLoader().getResource("models").getPath();
        //logger.debug("Basepath = " + basePath);
    }
    
    public void trainModels() throws Exception {
        
        models = new HashMap<>();
        
        int nTopTerms = Integer.parseInt(prop.getProperty("attributes.typedetect.supervised.ntopterms", "10"));
        float lambda = Float.parseFloat(prop.getProperty("attributes.typedetect.supervised.lambda", "0.5"));
        
        DPAttribNBClassifier classifier = null;
        SupervisedIEModel model = null;
        
        for (InformationUnit iu: bcts) {
            CVSplit dataset = new CVSplit(iu);
            
            logger.debug("Training with " + dataset.trainDocs.size() + ", testing with " + dataset.testDocs.size());
            
            iu.updateFromTrainingData(dataset, nTopTerms, lambda);
            
            ((AbstractDetectPresenceAttribute)iu).setThreshold();
        
            logger.debug("Training NB classifier...");
            // prepare for training by inputting the current fold
            classifier = new DPAttribNBClassifier(prop, dataset, iu);
            classifier.train();

            // Model to save
            logger.debug("Saving model for attribute " + iu.getName());
            model = new SupervisedIEModel((AbstractDetectPresenceAttribute)iu, classifier!=null?classifier.getClassifier(): null);
            
            //+++ Serialized saving of models
            //model.save(basePath);
            //
            
            models.put(iu.attribId, model);
            logger.debug("key: |" + iu.attribId + "|");
        }
        
        logger.debug("Total number of NB models = " + models.size());
    }
    
    public SupervisedIEModel getModel(String attribId) {
        return models.get(attribId);
    }
    
    public SupervisedIEModel load(String basePath, String attribId) {
        SupervisedIEModel obj = null;
        
        try (FileInputStream fileIn = new FileInputStream(SupervisedIEModel.getFileName(basePath, attribId))) {
            ObjectInputStream in = new ObjectInputStream(fileIn);
            obj = (SupervisedIEModel)in.readObject();
            in.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }
        
        return obj;
    }
    
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.err.println("Usage: java SupervisedIEModels <prop-file>");
            args[0] = "init.properties";
        }
        
        try {
            SupervisedIEModels handler = new SupervisedIEModels(args[0]);
            handler.trainModels();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }        
    }
}
