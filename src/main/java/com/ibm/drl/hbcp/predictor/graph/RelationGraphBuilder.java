package com.ibm.drl.hbcp.predictor.graph;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.api.IUnitPOJO;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.ContextualizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoRetriever;
import com.ibm.drl.hbcp.parser.AttributeCache;
import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Given a GT file, build a graph representing node types and edges across
 * different node types. An edge exists from node i to node j (i and j corresponding
 * to two different attributes) if i and j occur in the same paper.
 * 
 * @author dganguly
 */
public class RelationGraphBuilder {
    private final Properties prop;
    private final boolean includeContextToOutcomeEdges;
    private final boolean modelBCTAbsence;
    private final AttributeValueCollection<ArmifiedAttributeValuePair> unnormalizedAttributeValueCollection;
    private final AttributeValueCollection<ArmifiedAttributeValuePair> attributeValueCollection;

    private static Logger logger = LoggerFactory.getLogger(RelationGraphBuilder.class);

    // Should be equivalent to:
    //for (int src = AttributeType.POPULATION.ordinal(); src <= AttributeType.OUTCOME.ordinal(); src++) {
    //            for (int dest = src + 1; dest <= AttributeType.OUTCOME_VALUE.ordinal(); dest++) {
    // but without relying on AttributeType enum order
    private static final List<Pair<AttributeType, AttributeType>> VALID_EDGE_TYPES = Lists.newArrayList(
            Pair.of(AttributeType.POPULATION, AttributeType.INTERVENTION),
            Pair.of(AttributeType.POPULATION, AttributeType.OUTCOME),
            Pair.of(AttributeType.POPULATION, AttributeType.OUTCOME_VALUE), // ?? this was allowed by the original code
            Pair.of(AttributeType.INTERVENTION, AttributeType.OUTCOME),
            Pair.of(AttributeType.INTERVENTION, AttributeType.OUTCOME_VALUE),
            Pair.of(AttributeType.OUTCOME, AttributeType.OUTCOME_VALUE)
    );

    public RelationGraphBuilder(Properties prop) throws IOException {
        this.prop = prop;

        String graphStorePath = prop.getProperty("prediction.graph.path", "prediction/graphs/");
        includeContextToOutcomeEdges = Boolean.parseBoolean(prop.getProperty("prediction.graph.c2o", "true"));
        modelBCTAbsence = Boolean.parseBoolean(prop.getProperty("prediction.interventions.absence.nodes", "false"));
        unnormalizedAttributeValueCollection = AttributeValueCollection.cast(obtainAttribVals());
        // normalize the values
        Normalizers normalizers = new Normalizers(prop);
        AttributeValueCollection<NormalizedAttributeValuePair> normalizedAttributeValueCollection =
                new NormalizedAttributeValueCollection<ArmifiedAttributeValuePair>(normalizers, unnormalizedAttributeValueCollection);
        // use the normalized values to build the graph
        attributeValueCollection = AttributeValueCollection.cast(normalizedAttributeValueCollection);
    }

    /**
     * Returns a graph whose nodes are attribute-value pairs (AVP) and edges are built between AVPs in the same arm
     * @return A collection of weighted edges between 2 attribute-value pairs.
     */
    public AttribNodeRelations getGraph() {
        AttribNodeRelations res = new AttribNodeRelations();
        Set<String> docNames = attributeValueCollection.getDocNames();

        for (String docName: docNames) {
            Map<String, Map<AttributeType, Multiset<ArmifiedAttributeValuePair>>> avPairsPerDoc = attributeValueCollection.getArmifiedPairsInDocSplitByType(docName);// key is 'arm'
            for (String arm: avPairsPerDoc.keySet()) {
                Map<AttributeType, Multiset<ArmifiedAttributeValuePair>> avPairsPerDocPerArm = avPairsPerDoc.get(arm);
                updateGraphWithAttributeValuePairs(res, docName, arm, avPairsPerDocPerArm);  // for this doc for this arm
            }
        }
        return res;
    }

    private void updateGraphWithAttributeValuePairs(AttribNodeRelations graph, String docName, String arm, Map<AttributeType, Multiset<ArmifiedAttributeValuePair>> avPairsPerDocPerArm) {
        // Done building the list... now add edges...
        for (Pair<AttributeType, AttributeType> validEdgeType : VALID_EDGE_TYPES) {
            AttributeType src = validEdgeType.getLeft();
            AttributeType dest = validEdgeType.getRight();
            if (!includeContextToOutcomeEdges && src == AttributeType.POPULATION && dest == AttributeType.OUTCOME)
                continue;  // skip reference C->O when using transitive C->O
            addEdgesBetweenTypes(graph, docName, avPairsPerDocPerArm.get(src), avPairsPerDocPerArm.get(dest));
        }
    }

    private void addEdgesBetweenTypes(AttribNodeRelations graph, String docName,
                                      Multiset<ArmifiedAttributeValuePair> srcAVPairs,  // avpairs for one type
                                      Multiset<ArmifiedAttributeValuePair> destAVPairs) {
        if (srcAVPairs == null || destAVPairs == null)
            return;  // multisets can be null

        for (ArmifiedAttributeValuePair srcavp: srcAVPairs.elementSet()) {
            AttributeValueNode srcNode = new AttributeValueNode(srcavp);
            for (ArmifiedAttributeValuePair destavp: destAVPairs.elementSet()) {
                AttributeValueNode dstNode = new AttributeValueNode(destavp);
                graph.add(new AttribNodeRelations.AttribNodeRelation(docName, srcNode, dstNode));
            }
        }
    }


    // Depending on the type of the source - [gt/ie] obtain these attribute value
    // pairs from either extracted information or from the ground truth (json)
    private AttributeValueCollection<? extends ArmifiedAttributeValuePair> obtainAttribVals() throws IOException {
        String source = prop.getProperty("prediction.source", "gt");
        JSONRefParser parser = new JSONRefParser(prop);
        parser.buildAll();
        
        if (source.equals("gt")) {
            return parser.getAttributeValuePairs();
        }
        else { // from ie
            return createFromExtractedIndex(parser);
        }
    }

    public static AttributeValueCollection<ContextualizedAttributeValuePair> createFromExtractedIndex(JSONRefParser parser) throws IOException {
        AttributeCache attributeCache = parser.getAttributes();

        List<ContextualizedAttributeValuePair> pairs = new ArrayList<>();
        ExtractedInfoRetriever retriever = new ExtractedInfoRetriever();
        List<IUnitPOJO> iunits = retriever.getIUnitPOJOs();

        for (IUnitPOJO iunit: iunits) {
            // iunit.getCode() contains the name which must be converted back to the id
            String attribName = iunit.getCode();
            Attribute attribute = attributeCache.getFromName(attribName);
            if (attribute == null) {
                logger.error("Unable to get id of the attribute named '" + attribName + "'");
                System.out.println("Unable to get id of the attribute named '" + attribName + "'");
                continue;
            }

            pairs.add(new ContextualizedAttributeValuePair(
                    attribute,
                    iunit.getExtractedValue(),
                    iunit.getDocName(),
                    // TODO: for now IUnitPOJO is non-armified (the entire extraction is actually)
                    ArmifiedAttributeValuePair.DUMMY_ARM,
                    iunit.getContext()));
        }
        return new AttributeValueCollection<>(pairs);
    }

    private static String graphTypes[] = {
            "",
            "ann",
            "withDocTitle"
    };
    
    private void saveGraph(AttribNodeRelations graph, int code) throws Exception {
        String basePath = BaseDirInfo.getPath("prediction/graphs/");
        String source = "." + prop.getProperty("prediction.source", "gt");
        String graphName = basePath + "relations" + source + "." + graphTypes[code] + ".graph";
        BufferedWriter bw = new BufferedWriter(new FileWriter(graphName));

        switch (code) {
            case 0: graph.saveGraph(bw); break;
            case 1: graph.saveAnnotatedGraph(bw); break;
            case 2: graph.saveGraphWithDocTitle(bw); break;
        }
        bw.close();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.err.println("Usage: java RelationGraphBuilder <prop-file>");
            args[0] = "init.properties";
        }

        try {
            Properties props = Props.loadProperties(args[0]);
            //props.setProperty("prediction.source", "ie");
            RelationGraphBuilder gb = new RelationGraphBuilder(props);
            AttribNodeRelations graph = gb.getGraph();
            for (int i=0; i < RelationGraphBuilder.graphTypes.length; i++)
                gb.saveGraph(graph, i);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
