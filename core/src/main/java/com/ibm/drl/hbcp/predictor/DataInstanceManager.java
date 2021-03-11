/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.api.ExtractorController;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.core.wvec.WordVec;
import com.ibm.drl.hbcp.core.wvec.WordVecs;
import com.ibm.drl.hbcp.inforetrieval.apr.ParagraphAnalyzer;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.regression.WekaDataset;
import com.ibm.drl.hbcp.util.FileUtils;
import com.opencsv.CSVWriter;
import org.apache.commons.math3.distribution.NormalDistribution;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.slf4j.LoggerFactory;

import weka.core.Instances;
import weka.core.converters.ArffSaver;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

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

    static private org.slf4j.Logger logger = LoggerFactory.getLogger(ExtractorController.class);

    WordVecs wvecs; // for the PubMed vectors
    int wvec_dimension;
    int ndvec_dimension;
    
    boolean quantized;
    boolean useLargerContext;
    
    private String avgVecOutFile;
    
    HashMap<String, WordVec> nodeAndWordVecMap;
    int totalWordsSeen;
    int oov;
    
    Set<String> newDictNodeKeys;
    final int NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC = 5;   // Allow at most 5 values to be appended to a vector
    final Analyzer analyzer = new StandardAnalyzer();
        
    public DataInstanceManager(AttributeValueCollection<ArmifiedAttributeValuePair> trainValues,
                               AttributeValueCollection<ArmifiedAttributeValuePair> testValues) {
        this(trainValues, testValues, false, false);
    }
    
    public DataInstanceManager(AttributeValueCollection<ArmifiedAttributeValuePair> trainValues,
                               AttributeValueCollection<ArmifiedAttributeValuePair> testValues,
                               boolean quantized, boolean useLargerContext) {
        List<ArmifiedAttributeValuePair> combined = new ArrayList<>();
        combined.addAll(trainValues.getAllPairs());
        combined.addAll(testValues.getAllPairs());
        attributeValueCollection = new AttributeValueCollection<>(combined);
        this.trainInstances = createDataInstances(new ArrayList<>(trainValues.getDocNames()));
        this.testInstances = createDataInstances(new ArrayList<>(testValues.getDocNames()));
        this.quantized = quantized;
        this.useLargerContext = useLargerContext;
        totalWordsSeen = 0; oov = 0;
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
        try {
        	this.avgVecOutFile = avgVecOutFile;
        	wvecs = new WordVecs(new FileInputStream(FileUtils.potentiallyGetAsResource(new File(pubMedFile))));
        	wvec_dimension = wvecs.getVec("the").getDimension();        	
        } catch (FileNotFoundException fnfe) {
        	logger.warn("File " + pubMedFile + " not found, test will be skipped.");
        }
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

    private static String cleanTextContent(String text) {
        // strips off all non-ASCII characters
        text = text.replaceAll("[^\\x00-\\x7F]", "");

        // erases all the ASCII control characters
        text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

        // removes non-printable characters from Unicode
        text = text.replaceAll("\\p{C}", "");
        
        text = text.replaceAll("[\\r\\n]+", " ");

        return text.trim();
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
            String value = cleanTextContent(avp.getValue());
            
            StringBuffer nodeInstanceNameBuff = new StringBuffer();
            nodeInstanceNameBuff
                .append(avp.getAttribute().getType().getShortString())
                .append(":")
                .append(avp.getAttribute().getId())
                .append(":")
                .append(value)
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
                /*
                if (!nnNodeInstanceText.equals(nodeInstanceText))
                    System.out.println(String.format("Replacing missing node '%s' in the test set with the nearest '%s' from the embedding file",
                            nodeInstanceText, nnNodeInstanceText));
                */
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
    WordVec constructConcatenatedVec(ArmifiedAttributeValuePair avp, NodeVecs ndvecs, WordVec sumvec) {
        
        WordVec ndvec = ndvecs!=null? getNodeVec(avp, ndvecs): null;
        WordVec ndAndWordVec = WordVec.concat(ndvec, sumvec);
        ndAndWordVec.setWord(sumvec.getWord());

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

        /* TODO: Replace with log messages
        if (!nnNodeInstanceText.equals(nodeInstanceText))
            System.out.println(String.format("Replacing missing node '%s' in the test set with the nearest '%s' from the embedding file",
                    nodeInstanceText, nnNodeInstanceText));
        */

        return nnNodeInstanceText;
    }
    
    static boolean isWord(String token) {
        int len = token.length();
        for (int i=0; i < len; i++) {
            char ch = token.charAt(i);
            if (Character.isDigit(ch))
                return false;
            if (ch == '.')
                return false;
        }
        return true;
    }
    
    static public String selectWindowOfWords(String context, String annotation) {
        final int k=5;
        final int max = 10; // max 10 tokens
        
        List<String> analyzedContent = Arrays.asList(PaperIndexer.analyze(new StandardAnalyzer(), context).split(" "));
        List<String> analyzedMatch = Arrays.asList(PaperIndexer.analyze(new StandardAnalyzer(), annotation).split(" "));
        
        int s = analyzedContent.indexOf(analyzedMatch.get(0));
        int e = analyzedContent.indexOf(analyzedMatch.get(analyzedMatch.size()-1));
        
        if (s<0 || e<0 || s>=e)
            return PaperIndexer.analyze(new StandardAnalyzer(), annotation);
        
        StringBuffer buff = new StringBuffer();        
        int n = analyzedContent.size();
        int j=0;
        for (int i=Math.max(0, s-k); i < Math.min(e+k, n); i++) {
            buff.append(analyzedContent.get(i)).append(' ');
            if (j++ == max)
                break;
        }
        
        String contextText = PaperIndexer.analyze(new StandardAnalyzer(), buff.toString());
        return contextText;
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
            String docArmId, NodeVecs ndvecs) throws IOException {
    
        boolean isIntervention = avp.getAttribute().getType()== AttributeType.INTERVENTION;
        
        /*
        if (avp.getAttribute().getId().equals("3673298") && docArmId.startsWith("Ellerbeck"))
            avp = avp;
        */
        
        // Write out the words following the node... which are to
        // be loaded from the PubMed pretrained embeddings        
        String textValue =
            isIntervention?
                quantized?
                    ((NormalizedAttributeValuePair)((NormalizedAttributeValuePair)avp).getOriginal()).getOriginal().getValue()
                    :
                    ((NormalizedAttributeValuePair)avp).getOriginal().getValue()
            :
                avp.getValue();
        
        textValue = DataInstanceManager.selectWindowOfWords(avp.getContext(), textValue);
        
        
        // If just a single token, then get a longer context (if the flag to use it is true by the caller)
        String analyzedContext = PaperIndexer.analyze(analyzer, textValue);
        if (useLargerContext && !isIntervention && analyzedContext.split(" ").length == 1)
            textValue = avp.getContext();
        
        analyzedContext = PaperIndexer.analyze(analyzer, textValue);
        String pseudoWord = avp.getAttribute().getType().getShortString() +
                            ":" + avp.getAttribute().getId() +
                            ":" + docArmId;
        
        WordVec sumvec = new WordVec(wvec_dimension);
        int numWordMatches = 0;
        float[] extractedNumbersFormAnnotations = new float[NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC];
        float val = 0;
        int numbersFound = 0;
        
        for (String token: analyzedContext.split(" ")) {
            
            // First check if this token is a number. If so, we know that its vector
            // won't exist in the PubMed vecs
            try {
                val = Float.parseFloat(token);
            }
            catch (NumberFormatException nex) { val = 0; }
            if (val > 0 && !isIntervention) {
                if (numbersFound < NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC)
                    extractedNumbersFormAnnotations[numbersFound++] = val;
                continue;
            }
            
            // Next check if the PubMed pre-trained vector for this word exists.
            WordVec wvec = this.wvecs.getVec(token);
            if (wvec == null) {                
                // Keep OOV count here...
                oov++;
                continue;
            }
            
            sumvec = WordVec.sum(sumvec, wvec);
            numWordMatches++;
            totalWordsSeen++;
        }
        if (numWordMatches > 0)
            sumvec.scalarMutiply(1/(float)numWordMatches);  // take the average vector
        
        WordVec sumvecWithNumbers = appendNumbersToContext(sumvec, pseudoWord, extractedNumbersFormAnnotations);
        WordVec concatenatedVec = constructConcatenatedVec(avp, ndvecs, sumvecWithNumbers);
        
        nodeAndWordVecMap.put(concatenatedVec.getWord(), concatenatedVec);
        
        return pseudoWord; // the key is written in the node sequence i/p file
    }

    /*
        Append a sequence of pseudo-words of the form '<docname>_<armid>:word' (e.g. DOC1_1:goal)
        The vectors for the words are appended to the node vector file so that
        they form a concatenated vocabulary. The Keras code is going to use the tokens
        that we write out here as keys to get the vectors from the embedding file.
        If it's a OOV word or no words are found write the token ZERO_VEC which is
        to be concatenated as a 0 vector.
    */
    String getNodeAndContextVec(String nodeId, ArmifiedAttributeValuePair avp,
            String docArmId, NodeVecs ndvecs, WordVecs cvec) throws IOException {
    
        boolean isIntervention = avp.getAttribute().getType()== AttributeType.INTERVENTION;
        
        // Write out the words following the node... which are to
        // be loaded from the PubMed pretrained embeddings        
        String textValue = isIntervention?
            ((NormalizedAttributeValuePair)((NormalizedAttributeValuePair)avp).getOriginal()).getOriginal().getValue():
            avp.getValue();

        textValue = DataInstanceManager.selectWindowOfWords(avp.getContext(), textValue);
        
        String pseudoWord = avp.getAttribute().getType().getShortString() +
                            ":" + avp.getAttribute().getId() +
                            ":" + docArmId;
        
        float[] extractedNumbersFormAnnotations = new float[NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC];
        WordVec sumvec = cvec.getVec(textValue);
        if (sumvec == null) {
            sumvec = cvec.zeroVec(pseudoWord);
        }
        else {
            System.err.println("Loaded BERT vec |" + textValue + "|...");            
        }
        
        WordVec sumvecWithNumbers = appendNumbersToContext(sumvec, pseudoWord, extractedNumbersFormAnnotations);
        WordVec concatenatedVec = constructConcatenatedVec(avp, ndvecs, sumvecWithNumbers);
        
        nodeAndWordVecMap.put(concatenatedVec.getWord(), concatenatedVec);
        
        return pseudoWord; // the key is written in the node sequence i/p file
    }
    
    // Append the list of numbers to the sum vector
    WordVec appendNumbersToContext(WordVec contextvec, String pseudoword, float[] extractedNumbersFormAnnotations) {
        int cvec_dim = contextvec.getDimension();
        double[] numAnnotatedContextVec = new double[cvec_dim + NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC];
        System.arraycopy(contextvec.getPoint(), 0, numAnnotatedContextVec, 0, cvec_dim); // copy the context vec
        
        // copy the numbers
        for (int i=0; i < NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC; i++) {
            numAnnotatedContextVec[cvec_dim + i] = extractedNumbersFormAnnotations[i];
        }
        return new WordVec(pseudoword, numAnnotatedContextVec);
    }
    
    /*
        Write out the numeric vecs, e.g. MIN_AGE, simply by appending the
        zeroes for the word part...
        Enhancement: Instead of zeroes, write out the value itself 
    */
    void writeInDictConcatenatedVecForNumeric(String nodeVecKey, NodeVecs ndvecs, float numericValue) throws IOException {
        if (newDictNodeKeys.contains(nodeVecKey))
            return; // write only once for efficiency
        
        WordVec ndvec = null;
        if (ndvecs != null) {
            ndvec = ndvecs.getVec(nodeVecKey);
            if (ndvec == null) {
                // System.out.println(String.format("No key found for node (%s)", nodeVecKey));
                return;
            }
        }
        
        WordVec ndAndWordVec = WordVec.concat(ndvec, new WordVec(wvec_dimension, numericValue));
        ndAndWordVec.setWord(nodeVecKey);
        
        nodeAndWordVecMap.put(ndAndWordVec.getWord(), ndAndWordVec);
        
        newDictNodeKeys.add(nodeVecKey);
    } 
    
    /**
     * 
     * @param docArmId
     * @param headerMap
     * @param dataInstance
     * @param ndvecs  If ndvecs is NULL, then simply write out the text vector (PubMed vectors).
     * @return
     * @throws IOException 
     */
    String getSparseVecWithWords(String docArmId,
                                    Map<String, WekaDataset.AttributeHeader> headerMap,
                                    DataInstance dataInstance,
                                    NodeVecs ndvecs, WordVecs contextVec) throws IOException {
        
        ndvec_dimension = ndvecs!=null? ndvecs.getDimension(): 0;
        
        StringBuffer buff = new StringBuffer();
        String token;
        AttributeValueCollection<ArmifiedAttributeValuePair> x = dataInstance.getX();
        
        for (ArmifiedAttributeValuePair avp: x.getAllPairs()) {
            WekaDataset.AttributeHeader headerInfo = headerMap.get(avp.getAttribute().getId());
            if (headerInfo == null)
                continue;

            // if not string-type then append value as part of token and then append each word context
            // else split up the value (currently underscore separated)...
            
            /*
            This is the old flow where we added the context only for non-numeric types
            if (headerInfo.getType()!=WekaDataset.ValueType.STRING && !isIntervention) {
                token = getNearestNode(avp, ndvecs, false); 
                if (token != null) {
                    buff.append(token);
                    buff.append(" ");
                    
                    try {
                        val = Float.parseFloat(avp.getValue());
                    }
                    catch (NumberFormatException nex) { val = 0; }
                    
                    writeInDictConcatenatedVecForNumeric(token, ndvecs, val);
                }
            }            
            */
            //else { // this is of type text... also includes interventions
            
            String nodeId = getNearestNode(avp, null, true);
            token = contextVec==null? getNodeAndWordVec(nodeId, avp, docArmId, ndvecs) :
                    getNodeAndContextVec(nodeId, avp, docArmId, ndvecs, contextVec);
            
            if (token!=null)
                buff.append(token).append(" ");
            
            //}
        }
        if (buff.length() == 0)
            return null;
        
        buff.deleteCharAt(buff.length()-1);
        buff.append("\t");
        
        buff.append(dataInstance.getY().getValue());
        return buff.toString();
    }
    

    public void writeSparseVecsForSplit(String fileName, List<String> docNames, NodeVecs ndvecs,
                                        boolean withWords) {
        try {
            writeSparseVecsForSplit(fileName, docNames, false, ndvecs, withWords, null, 0);            
        }
        catch (IOException ex) { ex.printStackTrace(); }
    }
    
    static public String constructDocArmId(String docName, int armId, int startArmId) {
        docName = docName.replaceAll("\\s+", "_");
        return docName + "_" + (startArmId + armId);
    }

    public void writeTrainTestFiles(String trainFileName, List<String> trainDocs,
            String testFileName, List<String> testDocs,
            boolean includeTextAttribs, NodeVecs ndvecs,
            boolean withWords, WordVecs contextVecs) {
        writeTrainTestFiles(trainFileName, trainDocs, testFileName, testDocs, includeTextAttribs, ndvecs, withWords, contextVecs, 0);
    }
    
    public void writeTrainTestFiles(String trainFileName, List<String> trainDocs,
            String testFileName, List<String> testDocs,
            boolean includeTextAttribs, NodeVecs ndvecs,
            boolean withWords, WordVecs contextVecs, int startArmId) {
        
        /*
        for (String key: contextVecs.getWVecMap().keySet()) {
            System.out.println(String.format("|%s|", key));
        }
        System.out.println(contextVecs.getVec("advise to quit"));
        */
        if (contextVecs!=null)
            System.out.println("#BERT Vecs: " + contextVecs.getWVecMap().keySet().size());
        
        try {
            nodeAndWordVecMap = new HashMap<>();
            
            writeSparseVecsForSplit(trainFileName, trainDocs, includeTextAttribs, ndvecs, withWords, contextVecs, startArmId);
            writeSparseVecsForSplit(testFileName, testDocs, includeTextAttribs, ndvecs, withWords, contextVecs, startArmId);
            
            System.out.println("Writing concatenated text:node vecs in: " + this.avgVecOutFile);
            writeWordVecsinNewDict(this.avgVecOutFile);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void writeSparseVecsForSplit(String fileName, List<String> docNames, boolean includeTextAttribs) {
        try {
            writeSparseVecsForSplit(fileName, docNames, includeTextAttribs, null, false, null, 0);
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void writeSparseVecsForSplit(String fileName, List<String> docNames,
                                        boolean includeTextAttribs,
                                        NodeVecs ndvecs) throws IOException {
        writeSparseVecsForSplit(fileName, docNames, includeTextAttribs, ndvecs, false, null, 0);
    }
    
    /*
        This is used in the Python code, which needs to take as input tsv files.
        This is a refactoring of the sparse vectorization of the 'DataInstances' class.
        ndvecs == null indicates that we don't want to use the node vectors for the prediction task
    */
    public void writeSparseVecsForSplit(String fileName, List<String> docNames,
                                        boolean includeTextAttribs,
                                        NodeVecs ndvecs, boolean withWords,
                                        WordVecs contextVecs, int startArmId) throws IOException {
        File file = new File(fileName);
        file.getParentFile().mkdirs();
        FileWriter fw = new FileWriter(file);
        BufferedWriter bw = new BufferedWriter(fw);

        WekaDataset dataset = new WekaDataset(trainInstances, testInstances);
        List<WekaDataset.AttributeHeader> headers = dataset.getHeaders();
        Map<String, WekaDataset.AttributeHeader> headerMap = new HashMap<>();
        for (WekaDataset.AttributeHeader header: headers) {
            headerMap.put(header.getAttribute().getId(), header);
        }
        
        for (String docName: docNames) {
            List<DataInstance> perDocList = DataInstance.getInstancesForDoc(attributeValueCollection, docName);
            int armId = 0;
            for (DataInstance dataInstance: perDocList) {
                armId++;
                String docArmId = constructDocArmId(docName, armId, startArmId); // unique for a doc-arm
                String line = withWords?
                        getSparseVecWithWords(docArmId, headerMap, dataInstance, ndvecs, contextVecs):
                        getSparseVec(headerMap, dataInstance, includeTextAttribs, ndvecs)
                        ;
                if (line == null)
                    continue;
                bw.write(line);
                // Write out the doc name as well for more info... Appending as the last token
                // so that existing code isn't affected...
                bw.write("\t" + docName); // \t is the delimiter
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
        File avgvec = new File(avgVecOutFile);
        avgvec.getParentFile().mkdirs();
        
        FileWriter avgvec_fw = new FileWriter(avgvec);
        BufferedWriter avgvec_bw = new BufferedWriter(avgvec_fw);
    
        // first line - vocab-size and dimension
        // additional dimensions for word vecs and numbers
        // Get the dimension of the first vector
        int dimension = NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC; 
        if (!nodeAndWordVecMap.isEmpty()) {
            WordVec first = nodeAndWordVecMap.values().iterator().next();
            dimension = first.getDimension();
        }
        
        avgvec_bw.write(nodeAndWordVecMap.keySet().size() + " " +
                dimension
                //String.valueOf(this.wvec_dimension + ndvec_dimension + NUM_ADDITIONAL_DIMENSIONS_FOR_NUMERIC)
        );
        avgvec_bw.newLine();
        
        for (WordVec nwv: nodeAndWordVecMap.values()) {
            avgvec_bw.write(nwv.toString());
            avgvec_bw.newLine();
        }
        
        avgvec_bw.close();
        avgvec_fw.close();        
    }
    
    float oov() { return oov/(float)totalWordsSeen; }

    public List<DataInstance> augmentTrainingData(List<DataInstance> origTrainInstances) {
        // https://handbook-5-1.cochrane.org/chapter_7/7_7_3_3_obtaining_standard_deviations_from_standard_errors.htm
        // attributes we might be interested in:
        // Individual level analysed
        // Outcome value
        //

        // should be refactored later probably, but for now recollect instances by doc
        Map<String, List<DataInstance>> docInstanceMap = new HashMap<>();
        Set<Attribute> possibleAttributes = new HashSet<>();
        Attribute analyzedN = null;
        Attribute outcomeValue = null;
        for (DataInstance instance : origTrainInstances) {
            String docName = instance.getX().getDocNames().iterator().next();
            List<DataInstance> docDataInstances = docInstanceMap.computeIfAbsent(docName, k -> new ArrayList<>());
            docDataInstances.add(instance);
            if (analyzedN == null || outcomeValue == null) {
                for (ArmifiedAttributeValuePair avp : instance.getX().getAllPairs()) {
                    if (avp.getAttribute().getName().equals("Individual-level analysed")) {
                        analyzedN = avp.getAttribute();
                    }
                }
                outcomeValue = instance.getY().getAttribute();
            }
        }
        if (analyzedN == null || outcomeValue == null) throw new RuntimeException();  // we don't want this to happen
        // loop over documents
        List<DataInstance> retInstances = new ArrayList<>();
        for (Map.Entry<String, List<DataInstance>> docInstancesEntry : docInstanceMap.entrySet()) {
            // not the case always but we'll take the smallest outcome to be our 'control'
            final List<DataInstance> instances = docInstancesEntry.getValue();
            int minInd = -1;
            double minValue = 1000.0;
            for (int i = 0; i < instances.size(); i++) {
                if (instances.get(i).getYNumeric() < minValue) {
                    minInd = i;
                    minValue = instances.get(i).getYNumeric();
                }
            }
            // control values
            final DataInstance controlInstance = instances.get(minInd);
            retInstances.add(controlInstance);
            final double controlOutcomeValue = controlInstance.getYNumeric() / 100;  // convert from percentage
            int controlN = checkNumberParticipants(controlInstance, analyzedN);
            final double controlEvents = controlOutcomeValue * controlN;
            for (int i = 0; i < instances.size(); i++) {
                if (i == minInd)
                    continue;
                final DataInstance experimentalInstance = instances.get(i);
                retInstances.add(experimentalInstance);
                final double experimentalOutcomeValue = experimentalInstance.getYNumeric() / 100; // convert from percentage
                int experimentalN = checkNumberParticipants(experimentalInstance, analyzedN);
                if (controlN == -1) {
                    if (experimentalN == -1) {
                        // if we find no number of participants, skip
                        continue;
                    } else {
                        // if only missing control participants, assume same as experimental
                        controlN = experimentalN;
                    }
                } else {
                    if (experimentalN == -1) {
                        // if only missing experimental participants, assume same as contorl
                        experimentalN = controlN;
                    }
                }
                final double experimentalEvents = experimentalOutcomeValue * experimentalN;
                if (controlEvents < 1 && experimentalEvents < 1) { // probably means problem in annotation
                    continue;
                }
                final double stdError = Math.sqrt(controlEvents * (controlN - controlEvents) / Math.pow(controlN, 3) +
                        experimentalEvents * (experimentalN - experimentalEvents) / Math.pow(experimentalN, 3));
                final double stdDev = stdError / Math.sqrt(1.0 / controlN + 1.0 / experimentalN);
                final NormalDistribution experimentalDistribution = new NormalDistribution(experimentalOutcomeValue, stdDev);
                int sampleSize = (int) Math.ceil(Math.sqrt(experimentalN));  // try different functions for good sample size
                for (int j = 0; j < sampleSize; j++) {
                    final double sample = experimentalDistribution.sample() * 100;  // convert to percentage
                    if (sample < 0 || sample > 100) continue;
                    final ArmifiedAttributeValuePair y = experimentalInstance.getY();
                    final DataInstance sampleInstance = new DataInstance(experimentalInstance.getX(), new ArmifiedAttributeValuePair(y.getAttribute(), "" + sample, y.getDocName(), y.getArm(), y.getContext(), y.getPageNumber()));
                    retInstances.add(sampleInstance);
                }
            }
        }
        System.out.println("Original # insts: " + origTrainInstances.size() + " new # insts: " + retInstances.size());
        return retInstances;
    }

    private int checkNumberParticipants(DataInstance controlInstance, Attribute analyzedN) {
        final Multiset<ArmifiedAttributeValuePair> armifiedAttributeValuePairs = controlInstance.getX().byId().get(analyzedN.getId());
        if (armifiedAttributeValuePairs != null) {
            final Iterator<ArmifiedAttributeValuePair> iterator = armifiedAttributeValuePairs.iterator();
            if (iterator.hasNext())
                try {
                    return Integer.parseInt(iterator.next().getValue());
                } catch (NumberFormatException e) {
                    return -1;
                }
        }
        return -1;
    }
}
