/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.core.wvec.WordVec;
import com.ibm.drl.hbcp.core.wvec.WordVecs;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.regression.WekaDataset;
import com.opencsv.CSVWriter;
import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;

/**
 * This class implements the common functionality of handling with data instances
 * (each data instance roughly corresponds to one data row of an Weka arff/csv file).
 * 
 * The functionality is mainly a refactored version from the DataInstances class
 * The key difference is that we don't intend to use AttributeNodes any more. The values
 * and the attribute (feature) types are all obtained from the AVP collection.
 * @author dganguly
 */
public class DataInstanceManager {
    protected List<DataInstance> trainInstances;
    protected List<DataInstance> testInstances;
    protected AttributeValueCollection<ArmifiedAttributeValuePair> attributeValueCollection;

    WordVecs wvecs; // for the PubMed vectors
    int wvec_dimension;
    int ndvec_dimension;
    
    String avgVecOutFile;
    
    HashMap<String, WordVec> nodeAndWordVecMap;
    
    Set<String> newDictNodeKeys;
    
    public DataInstanceManager(AttributeValueCollection<ArmifiedAttributeValuePair> attributeValueCollection,
            List<String> trainDocNames,
            List<String> testDocNames) {
        this.attributeValueCollection = attributeValueCollection;
        this.trainInstances = createDataInstances(trainDocNames);
        this.testInstances = createDataInstances(testDocNames);
    }

    public void initWithPreTrainedWordVecs(String pubMedFile, String avgVecOutFile) {
        try {
            loadPretrainedWordVecs(pubMedFile, avgVecOutFile);
            newDictNodeKeys = new HashSet<>();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public List<DataInstance> createDataInstances(List<String> docNames) {
        List<DataInstance> dataInstances = new ArrayList<>();        
        for (String docName: docNames) {
            dataInstances.addAll(DataInstance.getInstancesForDoc(attributeValueCollection, docName));
        }
        return dataInstances;
    }

    public void writeArffFiles(String trainArffFile, String testArffFile) throws IOException {
        writeArffFiles(trainArffFile, testArffFile, 0);
    }
    
    public void writeArffFiles(String trainArffFile, String testArffFile, int numClasses) throws IOException {
        WekaDataset dataset = new WekaDataset(trainInstances, testInstances, numClasses);
        Instances train = WekaDataset.buildWekaInstances(dataset.getHeaders(), dataset.getTrainData());
        writeArffFile(trainArffFile, train);
        Instances test = WekaDataset.buildWekaInstances(dataset.getHeaders(), dataset.getTestData());
        writeArffFile(testArffFile, test);
    }

    public void loadPretrainedWordVecs(String pubMedFile, String avgVecOutFile) throws Exception {
        wvecs = new WordVecs(new FileInputStream(pubMedFile));
        wvec_dimension = wvecs.getVec("the").getDimension();
        this.avgVecOutFile = avgVecOutFile;
    }
    
    protected void writeArffFile(String fileName, Instances wekaInstances) {
        try {
            ArffSaver s= new ArffSaver();
            s.setInstances(wekaInstances);
            s.setFile(new File(fileName));
            s.writeBatch();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    String getSparseVec(Map<String, WekaDataset.AttributeHeader> headerMap, DataInstance dataInstance, NodeVecs ndvecs) {
        return getSparseVec(headerMap, dataInstance, false, ndvecs);
    }

    String getSparseVec(Map<String, WekaDataset.AttributeHeader> headerMap, DataInstance dataInstance,
            boolean includeTextAttribs, NodeVecs ndvecs) {
        
        StringBuffer buff = new StringBuffer();
        AttributeValueCollection<ArmifiedAttributeValuePair> x = dataInstance.getX();
        
        for (ArmifiedAttributeValuePair avp: x.getAllPairs()) {
            WekaDataset.AttributeHeader headerInfo = headerMap.get(avp.getAttribute().getId());
            if (headerInfo == null)
                continue;
            if (!includeTextAttribs && headerInfo.getType()==WekaDataset.ValueType.STRING)
                continue;
            
            /*
            Git-issue 568: If a node is not found in the pre-trained file of nodes, get its nearest one
            */
            StringBuffer nodeInstanceNameBuff = new StringBuffer();
            nodeInstanceNameBuff
                .append(avp.getAttribute().getType().getShortString())
                .append(":")
                .append(avp.getAttribute().getId())
                .append(":")
                .append(avp.getValue().replaceAll("[\\r\\n]+", " "))
            ;
            
            String nodeInstanceText = nodeInstanceNameBuff.toString();
            AttributeValueNode qnodeAttributeValue = AttributeValueNode.parse(nodeInstanceText);
            if (qnodeAttributeValue == null) {
                System.out.println("Skipping non-parsable node-type " + nodeInstanceText);
                continue;
            }
            
            String nnNodeInstanceText = nodeInstanceText;
            if (ndvecs != null) {
                List<String> nns = ndvecs.getClosestAttributeInstances(qnodeAttributeValue);
                nnNodeInstanceText = nns.isEmpty()? nodeInstanceText: ndvecs.getClosestAttributeInstances(qnodeAttributeValue).get(0); // returns either the exact match or the nearest match
                if (!nnNodeInstanceText.equals(nodeInstanceText))
                    System.out.println(String.format("Replacing missing node '%s' in the test set with the nearest '%s' from the embedding file",
                            nodeInstanceText, nnNodeInstanceText));
            }
            
            buff.append(nnNodeInstanceText).append(" ");
        }
        if (buff.length() == 0)
            return null;
        
        buff.deleteCharAt(buff.length()-1);
        buff.append("\t");
        
        buff.append(dataInstance.getY().getValue());
        return buff.toString();
    }
    
    /*
        Construct a line (name and vec) to be written to the modified file
    */ 
    WordVec constructConcatenatedVec(ArmifiedAttributeValuePair avp, NodeVecs ndvecs, String pseudoWord, WordVec sumvec) {
        
        WordVec ndvec = getNodeVec(avp, ndvecs);
        WordVec ndAndWordVec = WordVec.concat(ndvec, sumvec);
        ndAndWordVec.setWord(pseudoWord);

        return ndAndWordVec;
    }
    
    // TODO: Handle the string type --- currently a place-holder of '1'
    String getTextValue(ArmifiedAttributeValuePair avp, boolean addressText) {
        String value = avp.getValue();
        value = value.replaceAll("\\s+", "_");
        return !addressText? value : "1";
    }
    
    WordVec getNodeVec(ArmifiedAttributeValuePair avp, NodeVecs ndvecs) {
        boolean normalizeText = avp.getAttribute().getType()==AttributeType.INTERVENTION;
        String value = getTextValue(avp, normalizeText);  // if addressText then append '1' as the value (akin I nodes)
        
        StringBuffer nodeInstanceNameBuff = new StringBuffer();
        nodeInstanceNameBuff
            .append(avp.getAttribute().getType().getShortString())
            .append(":")
            .append(avp.getAttribute().getId())
            .append(":")
            .append(value)
        ;
        
        WordVec ndvec = ndvecs.getVec(nodeInstanceNameBuff.toString());
        if (ndvec == null)
            ndvec = new WordVec(ndvec_dimension); // a ZERO VEC
            
        return ndvec;
    }
    
    String getNearestNode(ArmifiedAttributeValuePair avp, NodeVecs ndvecs, boolean addressText) {
        String nnNodeInstanceText;
        String nodeInstanceText;
        StringBuffer nodeInstanceNameBuff = new StringBuffer();
        
        String value = getTextValue(avp, addressText);  // if addressText then append '1' as the value (akin I nodes)
        
        nodeInstanceNameBuff
            .append(avp.getAttribute().getType().getShortString())
            .append(":")
            .append(avp.getAttribute().getId())
            .append(":")
            .append(value)
        ;

        nodeInstanceText = nodeInstanceNameBuff.toString();
        AttributeValueNode qnodeAttributeValue = AttributeValueNode.parse(nodeInstanceText);
        if (qnodeAttributeValue == null) {
            System.out.println("Skipping non-parsable node-type " + nodeInstanceText);
            return null;
        }

        if (ndvecs==null || addressText)
            return nodeInstanceText;
        
        List<String> nns = ndvecs.getClosestAttributeInstances(qnodeAttributeValue);
        nnNodeInstanceText = nns.isEmpty()? nodeInstanceText: ndvecs.getClosestAttributeInstances(qnodeAttributeValue).get(0); // returns either the exact match or the nearest match
        if (!nnNodeInstanceText.equals(nodeInstanceText))
            System.out.println(String.format("Replacing missing node '%s' in the test set with the nearest '%s' from the embedding file",
                    nodeInstanceText, nnNodeInstanceText));

        return nnNodeInstanceText;
    }
    
    /*
        Append a sequence of pseudo-words of the form '<docname>_<armid>:word' (e.g. DOC1_1:goal)
        The vectors for the words are appended to the node vector file so that
        they form a concatenated vocabulary. The Keras code is going to use the tokens
        that we write out here as keys to get the vectors from the embedding file.
        If it's a OOV word or no words are found write the token ZERO_VEC which is
        to be concatenated as a 0 vector.
    */
    String getNodeAndWordVec(String nodeId, ArmifiedAttributeValuePair avp,
            String docArmId, NodeVecs ndvecs, boolean isIntervention) throws IOException {
        
        // Write out the words following the node... which are to
        // be loaded from the PubMed pretrained embeddings
        String textValue = isIntervention? ((NormalizedAttributeValuePair)avp).getOriginal().getValue(): avp.getValue();
        
        Analyzer analyzer = new StandardAnalyzer();
        String analyzedContext = PaperIndexer.analyze(analyzer, textValue);
        String pseudoWord = avp.getAttribute().getType().getShortString() +
                            ":" + avp.getAttribute().getId() +
                            ":" + docArmId;
        
        WordVec sumvec = new WordVec(wvec_dimension);
        int numWordMatches = 0;
        
        for (String token: analyzedContext.split(" ")) {
            
            // First check if the PubMed pre-trained vector for this word exists.
            WordVec wvec = this.wvecs.getVec(token);
            if (wvec == null)
                continue;
            
            sumvec = WordVec.sum(sumvec, wvec);
            numWordMatches++;
        }
        //sum OR avg?
        if (numWordMatches > 0)
            sumvec.scalarMutiply(1/(float)numWordMatches);  // take the average vector
        
        WordVec concatenatedVec = constructConcatenatedVec(avp, ndvecs, pseudoWord, sumvec);
        
        nodeAndWordVecMap.put(concatenatedVec.getWord(), concatenatedVec);
        
        return pseudoWord; // the key is written in the node sequence i/p file
    }
    
    /*
        Write out the numeric vecs, e.g. MIN_AGE, simply by appending the
        zeroes for the word part...
        Enhancement: Instead of zeroes, write out the value itself 
    */
    void writeInDictConcatenatedVecForNumeric(String nodeVecKey, NodeVecs ndvecs, float numericValue) throws IOException {
        if (newDictNodeKeys.contains(nodeVecKey))
            return; // write only once for efficiency
        
        WordVec ndvec = ndvecs.getVec(nodeVecKey);
        if (ndvec == null) {
            // System.out.println(String.format("No key found for node (%s)", nodeVecKey));
            return;
        }
        
        WordVec ndAndWordVec = WordVec.concat(ndvec, new WordVec(wvec_dimension, numericValue));
        ndAndWordVec.setWord(nodeVecKey);
        
        nodeAndWordVecMap.put(ndAndWordVec.getWord(), ndAndWordVec);
        
        newDictNodeKeys.add(nodeVecKey);
    } 
    
    String getSparseVecWithWords(String docArmId,
                                    Map<String, WekaDataset.AttributeHeader> headerMap,
                                    DataInstance dataInstance,
                                    NodeVecs ndvecs) throws IOException {
        
        ndvec_dimension = ndvecs.getDimension();
        
        StringBuffer buff = new StringBuffer();
        String token;
        AttributeValueCollection<ArmifiedAttributeValuePair> x = dataInstance.getX();
        
        for (ArmifiedAttributeValuePair avp: x.getAllPairs()) {
            WekaDataset.AttributeHeader headerInfo = headerMap.get(avp.getAttribute().getId());
            if (headerInfo == null)
                continue;

            boolean isIntervention = avp.getAttribute().getType()== AttributeType.INTERVENTION;
            // if not string-type then append value as part of token and then append each word context
            // else split up the value (current;y underscore separated)...            
            if (headerInfo.getType()!=WekaDataset.ValueType.STRING && !isIntervention) {
                token = getNearestNode(avp, ndvecs, false); 
                if (token != null) {
                    buff.append(token);
                    buff.append(" ");
                    writeInDictConcatenatedVecForNumeric(token, ndvecs, Float.parseFloat(avp.getValue()));
                }
            }
            else { // this is of type text...
                String nodeId = getNearestNode(avp, null, true);
                token = getNodeAndWordVec(nodeId, avp, docArmId, ndvecs, isIntervention);
                if (token!=null)
                    buff.append(token).append(" ");
            }
        }
        if (buff.length() == 0)
            return null;
        
        buff.deleteCharAt(buff.length()-1);
        buff.append("\t");
        
        buff.append(dataInstance.getY().getValue());
        return buff.toString();
    }
    

    public void writeSparseVecsForSplit(String fileName, List<String> docNames, NodeVecs ndvecs, boolean withWords) {
        try {
            writeSparseVecsForSplit(fileName, docNames, false, ndvecs, withWords);            
        }
        catch (IOException ex) { ex.printStackTrace(); }
    }
    
    static public String constructDocArmId(String docName, int armId) {
        docName = docName.replaceAll("\\s+", "_");
        return docName + "_" + armId;
    }

    public void writeTrainTestFiles(String trainFileName, List<String> trainDocs,
            String testFileName, List<String> testDocs,
            boolean includeTextAttribs, NodeVecs ndvecs, boolean withWords) {
        try {
            nodeAndWordVecMap = new HashMap<>();
            
            writeSparseVecsForSplit(trainFileName, trainDocs, includeTextAttribs, ndvecs, withWords);
            writeSparseVecsForSplit(testFileName, testDocs, includeTextAttribs, ndvecs, withWords);
            
            writeWordVecsinNewDict(this.avgVecOutFile);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void writeSparseVecsForSplit(String fileName, List<String> docNames, boolean includeTextAttribs) {
        try {
            writeSparseVecsForSplit(fileName, docNames, includeTextAttribs, null, false);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void writeSparseVecsForSplit(String fileName, List<String> docNames,
                                        boolean includeTextAttribs,
                                        NodeVecs ndvecs) throws IOException {
        writeSparseVecsForSplit(fileName, docNames, includeTextAttribs, ndvecs, false);
    }
    
    /*
        This is used in the Python code, which needs to take as input tsv files.
        This is a refactoring of the sparse vectorization of the 'DataInstances' class.
    */
    public void writeSparseVecsForSplit(String fileName, List<String> docNames,
                                        boolean includeTextAttribs,
                                        NodeVecs ndvecs, boolean withWords) throws IOException {
        FileWriter fw = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(fw);

        WekaDataset dataset = new WekaDataset(trainInstances, testInstances);
        List<WekaDataset.AttributeHeader> headers = dataset.getHeaders();
        Map<String, WekaDataset.AttributeHeader> headerMap = new HashMap();
        for (WekaDataset.AttributeHeader header: headers) {
            headerMap.put(header.getAttribute().getId(), header);
        }
        
        for (String docName: docNames) {
            List<DataInstance> perDocList = DataInstance.getInstancesForDoc(attributeValueCollection, docName);
            int armId = 0;
            for (DataInstance dataInstance: perDocList) {
                armId++;
                String docArmId = constructDocArmId(docName, armId); // unique for a doc-arm
                String line = withWords?
                        getSparseVecWithWords(docArmId, headerMap, dataInstance, ndvecs):
                        getSparseVec(headerMap, dataInstance, includeTextAttribs, ndvecs)
                        ;
                if (line == null)
                    continue;
                bw.write(line);
                bw.newLine();
            }
        }
        bw.close();
        fw.close();
    }

    public void writeCsvFiles(String trainCsvFile, String testCsvFile) throws IOException {
        // TODO this still collects attributes from train AND test; we might want only from train?
        WekaDataset dataset = new WekaDataset(trainInstances, testInstances);
        final List<WekaDataset.AttributeHeader> attributeHeaders = dataset.getHeaders();
        final List<String> headers = attributeHeaders.stream().map(e -> e.getAttribute().getName()).collect(Collectors.toList());
        final List<List<String>> trainData = dataset.getTrainData();
        final List<List<String>> testData = dataset.getTestData();
        writeCsvFile(trainCsvFile, headers, trainData);
        writeCsvFile(testCsvFile, headers, testData);
    }

    public void writeCsvFile(String fileName, List<String> header, List<List<String>> dataRows) {
        try (CSVWriter csvWriter = new CSVWriter(new FileWriter(fileName))) {
            csvWriter.writeNext(header.toArray(new String[0]));
            for (List<String> row : dataRows) {
                final List<String> cleanList = row.stream().map(e -> e.replaceAll("\\s+", " ")).collect(Collectors.toList());
                csvWriter.writeNext(cleanList.toArray(new String[0]));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
    
    public void writeWordVecsinNewDict(String avgVecOutFile) throws IOException {
        FileWriter avgvec_fw;
        BufferedWriter avgvec_bw;
        
        avgvec_fw = new FileWriter(avgVecOutFile);
        avgvec_bw = new BufferedWriter(avgvec_fw);
    
        // first line - vocab-size and dimension
        avgvec_bw.write(nodeAndWordVecMap.keySet().size() + " " + String.valueOf(this.wvec_dimension + ndvec_dimension));
        avgvec_bw.newLine();
        
        for (WordVec nwv: nodeAndWordVecMap.values()) {
            avgvec_bw.write(nwv.toString());
            avgvec_bw.newLine();
        }
        
        avgvec_bw.close();
        avgvec_fw.close();
        
    }
}
