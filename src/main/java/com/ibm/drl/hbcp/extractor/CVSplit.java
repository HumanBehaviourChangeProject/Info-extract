/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.lucene.index.IndexReader;

import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.CodeSetTree;
import com.ibm.drl.hbcp.parser.CodeSetTreeNode;
import com.ibm.drl.hbcp.parser.PerDocRefs;

/**
 *
 * @author dganguly
 */

class DocIdNamePair {
    int id;
    String name;

    public DocIdNamePair(InformationUnit iu, int id) throws Exception {
        this.id = id;
        name = iu.extractor.reader.document(id).get(ResearchDoc.FIELD_NAME);
    }
}

/**
 * This class splits the data into training and test sets for each attribute.
 *
 * @author dganguly
 */

public class CVSplit {
    InformationUnit iu;
    IndexReader unannotatedDocsInMemIndex;
    
    List<DocIdNamePair> trainDocs;
    List<DocIdNamePair> testDocs;
    
    List<AnnotatedAttributeValuePair> attribsToTrain;
    List<AnnotatedAttributeValuePair> attribsToTest;
    
    HashSet<String> trainingDocNames;
    PerDocRefs pdmap;
    
    /**
     * This constructor is used for a cross-validation split and not a held-out one.
     * @param iu Information unit object encoding the attribute to extract
     * @param numDocs  Number of documents
     * @param numDocsInOneFold Number of documents in one fold.
     * @param k The value of K in k-fold cross validation.
     * @throws Exception
     */    
    public CVSplit(InformationUnit iu, int numDocs, int numDocsInOneFold, int k) throws Exception {
        trainDocs = new ArrayList<>();
        testDocs = new ArrayList<>();
        this.iu = iu;
        
        int start = k*numDocsInOneFold;
        int end = (k+1)*numDocsInOneFold;
                
        // form the train/test split for this fold number
        for (int j=0; j<numDocs; j++) {            
            if (start <= j && j < end)
                testDocs.add(new DocIdNamePair(iu, j));  // k-th fold as test
            else
                trainDocs.add(new DocIdNamePair(iu, j)); // rest is training                
        }        
        loadTrainingAttribs();
    }

   /**
     * A dummy CVSplit when the entire collection is to be used for training.
     * To be called during initiating the REST APIs.
     * @param iu Information unit object encoding the attribute to extract
     */    
    public CVSplit(InformationUnit iu) throws Exception {
        trainDocs = new ArrayList<>();
        testDocs = new ArrayList<>();
        
        this.iu = iu;
        
        int numDocs = iu.extractor.reader.numDocs();
        for (int i=0; i < numDocs; i++) {
            trainDocs.add(new DocIdNamePair(iu, i)); // rest is training                
        }
        loadTrainingAttribs();
    }
    
    public CVSplit(InformationUnit iu, CVFoldGenerator fg, int k) throws Exception {
        trainDocs = new ArrayList<>();
        testDocs = new ArrayList<>();
        this.iu = iu;
        
        for (Integer docNum : fg.getBuckets().keySet()) {            
            if (fg.getBuckets().get(docNum)==k)
                testDocs.add(new DocIdNamePair(iu, docNum));  // k-th fold as test
            else
                trainDocs.add(new DocIdNamePair(iu, docNum)); // rest is training                
        }
        
        loadTrainingAttribs();
    }
    
   /**
     * Allows specifying sprint names, i.e. separate held out datasets for train/test.
     * @param iu Information unit object encoding the attribute to extract
     * @param trainSprints Name of the sprints to be used for training, e.g. "12" or "123" etc.
     * @param testSprints Name of the sprints to be used for testing, e.g. "12" or "123" etc.
     */
    public CVSplit(InformationUnit iu, String trainSprints, String testSprints) throws Exception {
        trainDocs = new ArrayList<>();
        testDocs = new ArrayList<>();
        this.iu = iu;
        
        //Create list of docNames included in Train and Test
        Set<String> trainingDocs = new HashSet<String>();
        for (int i = 0; i < trainSprints.length(); i++){
            Set<String> docsOfThisSprint = iu.extractor.docBySprint.get("Sprint" + String.valueOf(trainSprints.charAt(i)));
            trainingDocs.addAll(docsOfThisSprint);
        }
        
        Set<String> testingDocs = new HashSet<String>();
        for (int i = 0; i < testSprints.length(); i++){
            Set<String> docsOfThisSprint = iu.extractor.docBySprint.get("Sprint" + String.valueOf(testSprints.charAt(i)));
            testingDocs.addAll(docsOfThisSprint);
        }
        
        //Create the training and testing sets
        for (String docName : trainingDocs) {
            int num = iu.extractor.getDocIdFromName(docName);
            System.out.println(docName);
            if (num > -1) {
                trainDocs.add(new DocIdNamePair(iu, num));
            }
        }
        
        for (String docName : testingDocs) {
            int num = iu.extractor.getDocIdFromName(docName);
            if (num > -1) {
                testDocs.add(new DocIdNamePair(iu, num));
            }
        }       
        loadTrainingAttribs();
        
    }
    
    public void getTrainingDocNames() throws Exception {
        trainingDocNames = new HashSet<>();        
        for (DocIdNamePair docIdNamePair: trainDocs) {
            trainingDocNames.add(docIdNamePair.name);
        }
    }
    
    /**
     * Used to get the documents that are not annotated in order to construct
     * the set of negative samples.
     * @return A list of document ids that weren't annotated for the given attribute
     * encoded in the class member 'iu'.
     */    
    public List<Integer> getUnannotatedDocIds() throws Exception {
        List<Integer> unannotatedDocs = new ArrayList<>();
        
        // Collect all unannotated docs
        for (DocIdNamePair dnp: trainDocs) {
            String annotatedText = pdmap.getAnnotatedText(dnp.name);
            if (annotatedText == null) {
                unannotatedDocs.add(dnp.id);
            }
        }
        
        return unannotatedDocs;
    }
    
    void loadTrainingAttribs() throws Exception {
        
        getTrainingDocNames();
        
        // Get a list of attributes to train from that belong to this training set
        CodeSetTree refNodeTree = iu.extractor.refBuilder.getGroundTruths(iu.type.code());
        CodeSetTreeNode refNode = refNodeTree.getNode(iu.attribId);
        pdmap = refNode.getDocRecordMap();
                
        List<AnnotatedAttributeValuePair> attribs = new ArrayList<>(pdmap.getRefs().values());
        attribsToTrain = new ArrayList<>();
        attribsToTest = new ArrayList<>();
        
        for (AnnotatedAttributeValuePair a : attribs) {
            String docName = a.getDocName();
            if (trainingDocNames.contains(docName))
                attribsToTrain.add(a); // filter only those that appear in the training set
            else
                attribsToTest.add(a);
        }
    }
    
    public List<AnnotatedAttributeValuePair> getAttribsToTrain() { return attribsToTrain; }
    public List<AnnotatedAttributeValuePair> getAttribsToTest() { return attribsToTest; }
}


