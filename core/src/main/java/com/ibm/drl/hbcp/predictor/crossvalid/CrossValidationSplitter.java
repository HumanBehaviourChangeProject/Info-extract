/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.crossvalid;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Random;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.api.IUnitPOJO;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoRetriever;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaner;
import com.ibm.drl.hbcp.parser.cleaning.NumericValueCleaner;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations.AttribNodeRelation;
import com.ibm.drl.hbcp.util.Props;

/**
 * Splits the graph produced from JSON annotations into a train graph and a test graph.
 * The train graph will be used to run node2vec and learn the vectors.
 * The test graph will use these vectors in combination with queries to find out how accurate the vectors for unseen edges.
 * @author dganguly, marting
 */
public class CrossValidationSplitter {
    private final double trainPercentage;  // Easier to implement than folding... select a percentage for training and the rest for testing    
    private final AttribNodeRelations graph;
    private final AttributeValueCollection<ArmifiedAttributeValuePair> attributeValueCollection;
    
    private final AttribNodeRelations train;
    private final AttribNodeRelations test;
    
    final List<String> trainingDocs;
    final List<String> testDocs;
    
    private static final int SEED = 123456;
    private final Random random = new Random(SEED);
    private static Logger logger = LoggerFactory.getLogger(CrossValidationSplitter.class);

    public CrossValidationSplitter(double trainPercentage, AttribNodeRelations graph) {
        this.trainPercentage = trainPercentage;
        this.graph = graph;
        attributeValueCollection = null;
        
        Pair<AttribNodeRelations, AttribNodeRelations> split = split(graph);
        train = split.getLeft();
        test = split.getRight();
        
        trainingDocs = null;
        testDocs = null;
    }

    /*+++REFACTOR-DG:
        We intend to avoid passing the 'AttribNodeRelations' object. The
        splitter shouldn't depend on the graph
    ---REFACTOR-DG */
    public CrossValidationSplitter(double trainPercentage,
            String jsonFile, boolean isGT,
            boolean applyCleaning) throws Exception {
        
        attributeValueCollection = AttributeValueCollection.cast(
                obtainAttribVals(jsonFile, isGT, applyCleaning, Props.loadProperties()));
        
        this.trainPercentage = trainPercentage;
        Pair<List<String>, List<String>> split = split(trainPercentage);
        trainingDocs = split.getLeft();
        testDocs = split.getRight();
        
        // Used in the older flow
        graph = null;
        train = null;
        test = null;
    }
    
    /**
     * Refactoring: Yet another constructor... this time to ensure that we pass the AVPs to the splitter
     */ 
    public CrossValidationSplitter(AttributeValueCollection<ArmifiedAttributeValuePair> attributeValueCollection, double trainPercentage) {
        this.attributeValueCollection = attributeValueCollection;
        
        this.trainPercentage = trainPercentage;
    
        Pair<List<String>, List<String>> splits = split(trainPercentage);
        trainingDocs = splits.getLeft();
        testDocs = splits.getRight();
        
        // Used in the older flow
        graph = null;
        train = null;
        test = null;
    }
    
    // Depending on the type of the source - [gt/ie] obtain these attribute value
    // pairs from either extracted information or from the ground truth (json)
    public static AttributeValueCollection<? extends ArmifiedAttributeValuePair> obtainAttribVals(Properties props) throws IOException {
        return obtainAttribVals(
                props.getProperty("ref.json"),
                "gt".equalsIgnoreCase(props.getProperty("prediction.source")),
                Boolean.parseBoolean(props.getProperty("prediction.applyCleaning", "false")),
                props // I need to pass it anyway so this is kind of useless
        );
    }
    
    public static AttributeValueCollection<? extends ArmifiedAttributeValuePair> obtainAttribVals(
            String jsonFile, boolean isGT, boolean applyCleaning, Properties props) throws IOException {
        JSONRefParser parser = new JSONRefParser(new File(jsonFile));
        parser.buildAll();
        
        if (isGT) {
            final AttributeValueCollection<AnnotatedAttributeValuePair> attributeValuePairs = parser.getAttributeValuePairs();
            // apply cleaners (if property set)
            if (applyCleaning) {
//                Cleaners cleaners = new Cleaners(props);
                final List<Attribute> numericAttributes = attributeValuePairs.getNumericAttributes();
                final List<String> numericAttributeIds = numericAttributes.stream().map(e -> e.getId()).collect(Collectors.toList());
                final NumericValueCleaner cleaners = new NumericValueCleaner(numericAttributeIds);
                AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.getCleaned(attributeValuePairs);
                logger.info("Amount cleaned: " + Cleaner.delta(cleaned, attributeValuePairs).size());
                return cleaned;
            } else
                return attributeValuePairs;
        }
        else { // from ie
            return createFromExtractedIndex(parser, props);
        }
    }

    public static AttributeValueCollection<ArmifiedAttributeValuePair> createFromExtractedIndex(JSONRefParser parser, Properties props) throws IOException {
        Attributes attributes = parser.getAttributes();

        List<ArmifiedAttributeValuePair> pairs = new ArrayList<>();
        ExtractedInfoRetriever retriever = new ExtractedInfoRetriever(props);
        List<IUnitPOJO> iunits = retriever.getIUnitPOJOs();

        for (IUnitPOJO iunit: iunits) {
            // iunit.getCode() contains the name which must be converted back to the id
            String attribName = iunit.getCode();
            Attribute attribute = attributes.getFromName(attribName);
            if (attribute == null) {
                System.out.println("Unable to get id of the attribute named '" + attribName + "'");
                continue;
            }

            pairs.add(new ArmifiedAttributeValuePair(
                    attribute,
                    iunit.getExtractedValue(),
                    iunit.getDocName(),
                    new Arm(iunit.getArmId(), iunit.getArmName()),
                    iunit.getContext()));
        }
        return new AttributeValueCollection<>(pairs);
    }
    
    /**
     * Split the graph into a training graph and a testing graph
     * @param graph the original complete graph
     * @return a pair of the training graph (left) and the testing graph (right)
     */
    private Pair<AttribNodeRelations, AttribNodeRelations> split(AttribNodeRelations graph) {
        // get all the docnames in the graph
        List<String> docNames = graph.getEdges().stream()
                .flatMap(rel -> Lists.newArrayList((ArmifiedAttributeValuePair)rel.source.getOriginal(), (ArmifiedAttributeValuePair)rel.target.getOriginal())
                        .stream())
                .map(ArmifiedAttributeValuePair::getDocName)
                .distinct()
                .collect(Collectors.toList());
        // shuffle them for a random split
        Collections.shuffle(docNames, random);
        // split the docnames into train/test
        int trainIndexEnd = (int)(trainPercentage * docNames.size());
        List<String> trainDocNames = docNames.subList(0, trainIndexEnd);
        List<String> testDocNames = docNames.subList(trainIndexEnd, docNames.size());
        // build a train and test subgraph
        return Pair.of(
                getSubgraphForDocs(graph, new HashSet<>(trainDocNames)),
                getSubgraphForDocs(graph, new HashSet<>(testDocNames))
        );
    }

    /*+++REFACTOR-DG:
        No graph object in split() 
        Function now supposed to return a list of doc names obtained from the
        AVP collection.
    ---REFACTOR-DG */
    public final Pair<List<String>, List<String>> split(double trainPercentage) {
        // Obtain the doc names from the AVP collection
        List<String> docNames = new ArrayList(attributeValueCollection.getDocNames());
        
        // shuffle them for a random split
        Collections.shuffle(docNames, random);
        
        // split the docnames into train/test
        int trainIndexEnd = (int)(trainPercentage * docNames.size());
        List<String> trainDocNames = docNames.subList(0, trainIndexEnd);
        List<String> testDocNames = docNames.subList(trainIndexEnd, docNames.size());
        
        // build a train and test subgraph
        return Pair.of(trainDocNames, testDocNames);
    }
    
    private AttribNodeRelations getSubgraphForDocs(AttribNodeRelations graph, Set<String> docnames) {
        AttribNodeRelations res = new AttribNodeRelations();
        for (AttribNodeRelation edge : graph.getEdges()) {
            List<ArmifiedAttributeValuePair> navps = Lists.newArrayList(
                    (ArmifiedAttributeValuePair)edge.source.getOriginal(),
                    (ArmifiedAttributeValuePair)edge.target.getOriginal()
            );
            if (navps.stream().allMatch(navp -> docnames.contains(navp.getDocName()))) {
                res.add(edge);
            }
        }
        return res;
    }

    public void dumpEdgeList(AttribNodeRelations relations, String prefixForFilename) throws IOException {
        String fileName = "prediction/graphs/" + prefixForFilename + ".edges.txt";
        FileWriter fw = new FileWriter(fileName);
        BufferedWriter bw = new BufferedWriter(fw);
        
        for (AttribNodeRelation ar: relations.getEdges()) {
            bw.write(ar.toString());
            bw.newLine();
        }
    }

    public AttribNodeRelations getTrainingSet() { return train; }
    public AttribNodeRelations getTestSet() { return test; }
    
    public List<String> getTrainingDocs() { return trainingDocs; }
    public List<String> getTestDocs() { return testDocs; }
    
}
