package com.ibm.drl.hbcp.predictor.graph;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.api.IUnitPOJO;
import com.ibm.drl.hbcp.core.attributes.*;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoRetriever;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaner;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.parser.cleaning.NumericValueCleaner;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static com.ibm.drl.hbcp.core.attributes.normalization.Normalizers.getAttributeIdsFromProperty;
import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;

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
    
    Set<String> numericalAttributes;
    
    //private final boolean modelBCTAbsence;
    private final AttributeValueCollection<ArmifiedAttributeValuePair> attributeValueCollection;

    private static Logger logger = LoggerFactory.getLogger(RelationGraphBuilder.class);

    private static final double DEFAULT_P_VALUE_WEIGHT = Math.exp(-0.1);

    private static final List<Pair<AttributeType, AttributeType>> VALID_EDGE_TYPES = Lists.newArrayList(
            Pair.of(AttributeType.POPULATION, AttributeType.SETTING),
            Pair.of(AttributeType.POPULATION, AttributeType.INTERVENTION),
            Pair.of(AttributeType.INTERVENTION, AttributeType.REACH),
            Pair.of(AttributeType.INTERVENTION, AttributeType.DOSE),
            Pair.of(AttributeType.INTERVENTION, AttributeType.SOURCE),
            Pair.of(AttributeType.INTERVENTION, AttributeType.MODES_OF_DELIVERY),
            Pair.of(AttributeType.INTERVENTION, AttributeType.OUTCOME_VALUE),
            Pair.of(AttributeType.OUTCOME_VALUE, AttributeType.OUTCOME),
            Pair.of(AttributeType.OUTCOME_VALUE, AttributeType.EFFECT)
    );

    public RelationGraphBuilder(Properties prop, AttributeValueCollection<ArmifiedAttributeValuePair> values) {
        this.prop = prop;

        //String graphStorePath = prop.getProperty("prediction.graph.path", "prediction/graphs/");
        numericalAttributes = getAttributeIdsFromProperty(prop, "prediction.attribtype.numerical");

        includeContextToOutcomeEdges = Boolean.parseBoolean(prop.getProperty("prediction.graph.c2o", "true"));
        //modelBCTAbsence = Boolean.parseBoolean(prop.getProperty("prediction.interventions.absence.nodes", "false"));
        // distribute the empty arm values
        AttributeValueCollection<ArmifiedAttributeValuePair> unnormalizedAttributeValueCollection = values;
        // normalize the values
        Normalizers normalizers = new Normalizers(prop);
        AttributeValueCollection<NormalizedAttributeValuePair> normalizedAttributeValueCollection =
                new NormalizedAttributeValueCollection<>(normalizers, unnormalizedAttributeValueCollection);
        // TODO this might not actually help? predictions seem way too high with it
        // distribute the empty arm values to the other arms
        AttributeValueCollection<NormalizedAttributeValuePair> nAVPWithoutEmptyArm = normalizedAttributeValueCollection.distributeEmptyArm();
        // use the normalized values to build the graph
        attributeValueCollection = AttributeValueCollection.cast(nAVPWithoutEmptyArm);
    }

    public RelationGraphBuilder(Properties prop, boolean predGT) throws IOException {
        this(Props.overrideProps(prop, Lists.newArrayList(Pair.of("prediction.source", predGT ? "gt" : "ie"))));
    }
    
    public RelationGraphBuilder(Properties prop) throws IOException {
        this(prop, AttributeValueCollection.cast(CrossValidationSplitter.obtainAttribVals(prop)));
    }

    /**
     * Returns a graph whose nodes are attribute-value pairs (AVP) and edges are built between AVPs in the same arm
     * @return A collection of weighted edges between 2 attribute-value pairs.
     */
    public AttribNodeRelations getGraph(boolean useArms) {
        Map<AttributeType, Multiset<ArmifiedAttributeValuePair>> avPairsPerDocPerArm;
        AttribNodeRelations res = new AttribNodeRelations();
        Set<String> docNames = attributeValueCollection.getDocNames();
        
        for (String docName: docNames) {
            Map<Arm, Map<AttributeType, Multiset<ArmifiedAttributeValuePair>>> avPairsPerDoc =
                    attributeValueCollection.getArmifiedPairsInDocSplitByType(docName); // key is 'arm'
            
            if (useArms) {
                for (Arm arm: avPairsPerDoc.keySet()) {
                    avPairsPerDocPerArm = avPairsPerDoc.get(arm);
                    String armName = arm.getStandardName();
                    updateGraphWithAttributeValuePairs(res,
                        docName, armName, avPairsPerDocPerArm);  // for this doc for this arm
                }                                
            }
            else {
                // flatten by stripping off the arm names
                avPairsPerDocPerArm = new HashMap<>();
                
                for (Arm armName: avPairsPerDoc.keySet()) {
                    Map<AttributeType, Multiset<ArmifiedAttributeValuePair>> armRcds = 
                        avPairsPerDoc.get(armName);
                    
                    for (Map.Entry<AttributeType, Multiset<ArmifiedAttributeValuePair>> e: armRcds.entrySet()) {
                        AttributeType aTypeKey = e.getKey();
                        avPairsPerDocPerArm.putIfAbsent(aTypeKey, HashMultiset.create());
                        avPairsPerDocPerArm.get(aTypeKey).addAll(e.getValue());
                    }
                }
                
                updateGraphWithAttributeValuePairs(res,
                    docName, Arm.EMPTY.getStandardName(), avPairsPerDocPerArm);  // for this doc for this arm
            }
        }
        
        // Add edges between same types
        addEdgesBetweenSameTypes(res);
        
        return res;
    }

    private void updateGraphWithAttributeValuePairs(
            AttribNodeRelations graph,
            String docName, String arm,
            Map<AttributeType, Multiset<ArmifiedAttributeValuePair>> avPairsPerDocPerArm) {

        // Get the set of effect sizes
        boolean useEffectSizes = Boolean.parseBoolean(prop.getProperty("prediction.effects.priors", "false"));
        double pvalue = useEffectSizes ? getPValue(avPairsPerDocPerArm) : 1;

        // Done building the list... now add edges...
        for (Pair<AttributeType, AttributeType> validEdgeType : getValidEdgeTypes()) {
            AttributeType src = validEdgeType.getLeft();
            AttributeType dest = validEdgeType.getRight();
            if (!includeContextToOutcomeEdges &&
                    src == AttributeType.POPULATION &&
                    dest == AttributeType.OUTCOME)
                continue;  // skip reference C->O when using transitive C->O
            
            /*
            //+++TODO: Revisit later and decide ---DG
            if (src==AttributeType.NEW_PRIORITISED_CODESET || dest==AttributeType.NEW_PRIORITISED_CODESET)
                continue; // +++DG: For the time being, ignore the new codeset for prediction ---DG
            */
            
            addEdgesBetweenTypes(graph, docName,
                    avPairsPerDocPerArm.get(src),
                    avPairsPerDocPerArm.get(dest), pvalue);
        }
    }

    protected List<Pair<AttributeType, AttributeType>> getValidEdgeTypes() {
        return VALID_EDGE_TYPES;
    }

    private double getPValue(Map<AttributeType, Multiset<ArmifiedAttributeValuePair>> avPairsPerDocPerArm) {
        Multiset<ArmifiedAttributeValuePair> effectSizes = avPairsPerDocPerArm.get(AttributeType.EFFECT);
        if (effectSizes == null)
            return DEFAULT_P_VALUE_WEIGHT;

        // find the avp of the valid effect size (e.g. effect size p value)
        String pValueAttrib = prop.getProperty("prediction.effects.attrib");
        Optional<ArmifiedAttributeValuePair> pvalAttrib = effectSizes.stream().filter(avp -> avp.getAttribute().getId().equals(pValueAttrib)).findAny();
        if (!pvalAttrib.isPresent())
            return DEFAULT_P_VALUE_WEIGHT;

        try {
            double res = ParsingUtils.parseFirstDouble(pvalAttrib.get().getValue());
            return Math.exp(-res);
        }
        catch (NumberFormatException nfex) {
            return DEFAULT_P_VALUE_WEIGHT;
        }
    }

    private void addEdgesBetweenSameTypes(AttribNodeRelations graph) {
        final String PSEUDO_DOC_NAME = "INTRA-ATTRIB-EDGE";
        boolean addEdgesBetweenSameType = Boolean.parseBoolean(prop.getProperty("prediction.graph.numeric.edges", "true"));

        if (!addEdgesBetweenSameType)
            return;

        // Find the minimum weight of a co-occurrence edge. We make sure that the weight
        // of a numeric edge is ALWAYS LESS than the minimum weight of a co-occurrence type edge.
        // This is to make it less likely for the random walk to get stuck within visits to
        // the same edge types.
        List<AttribNodeRelations.AttribNodeRelation> edges = graph.getEdges();
        float minEdgeWt = Float.MAX_VALUE;
        for (AttribNodeRelations.AttribNodeRelation e: edges) {
            if (e.weight < minEdgeWt) {
                minEdgeWt = (float)e.weight;
            }
        }
        
        // Intra-attribute edge types
        Multiset<ArmifiedAttributeValuePair> allAVPairs = attributeValueCollection.getAllPairs();
        int numpairs = allAVPairs.size();
        ArmifiedAttributeValuePair[] avPairArray = new ArmifiedAttributeValuePair[numpairs];
        avPairArray = allAVPairs.toArray(avPairArray);

        for (int i=0; i < numpairs; i++) {
            ArmifiedAttributeValuePair avp_a = avPairArray[i];
            final AttributeValueNode aNode = new AttributeValueNode(avp_a);
            if (aNode.getAttribute().getType() == AttributeType.EFFECT)
                continue; // skip Effect-Effect same type edges

            for (int j=i+1; j < numpairs; j++) {
                ArmifiedAttributeValuePair avp_b = avPairArray[j];
                if (!avp_a.getAttribute().getId().equals(avp_b.getAttribute().getId()))
                    continue;
                final AttributeValueNode bNode = new AttributeValueNode(avp_b);

                final float weight = intervalInducedWeight(aNode, bNode, minEdgeWt);

                AttribNodeRelations.AttribNodeRelation numericEdge =
                    new AttribNodeRelations.AttribNodeRelation(PSEUDO_DOC_NAME,
                        aNode, bNode, weight);
                if (numericEdge.weight > 0)
                    graph.add(numericEdge);
            }
        }
    }

    /* DG:
    This weight applies for nodes of the same type..
    and measures a prior of the similarity between the 'values'.
    If node type is numerical, the weight is exp-(diff between values)

    TODO: current implementation returns 0
    [If node type is categorical (with a text representation), then
    the weight is the edit distance.]
    */
    private float intervalInducedWeight(AttributeValueNode a, AttributeValueNode b, float interTypeEdgeWtMinVal) {
        // if attributes are different, return 0
        if (!a.getId().equals(b.getId()))
            return 0;

        Double aval = a.getNumericValue();
        if (aval == null)
            return 0;
        Double bval = b.getNumericValue();
        if (bval == null)
            return 0;
        if (aval.equals(bval))
            return 0;

        // a value between interTypeEdgeWtMinVal (if aval=bval) and 0 (in the limit if they're infinite distance apart)
        return interTypeEdgeWtMinVal * (float)Math.exp(-Math.abs(aval-bval));
    }

    private void addEdgesBetweenTypes(AttribNodeRelations graph, String docName,
                                      Multiset<ArmifiedAttributeValuePair> srcAVPairs,  // avpairs for one type
                                      Multiset<ArmifiedAttributeValuePair> destAVPairs,
                                      double pvalue) {
        if (srcAVPairs == null || destAVPairs == null)
            return;  // multisets can be null

        for (ArmifiedAttributeValuePair srcavp: srcAVPairs.elementSet()) {
            AttributeValueNode srcNode = new AttributeValueNode(srcavp);
            for (ArmifiedAttributeValuePair destavp: destAVPairs.elementSet()) {
                AttributeValueNode dstNode = new AttributeValueNode(destavp);
                // TODO: why is this a float?
                graph.add(new AttribNodeRelations.AttribNodeRelation(docName, srcNode, dstNode), (float)pvalue);
            }
        }
    }

    private static String[] graphTypes = {
            "",
            "ann",
            "withDocTitle"
    };

    public void saveGraph(AttribNodeRelations graph, String graphName, int code) throws Exception {
        String basePath = BaseDirInfo.getPath("prediction/graphs/");
        String source = "." + prop.getProperty("prediction.source", "gt");
        String graphNameFull = basePath + graphName + source + "." + graphTypes[code] + ".graph";
        BufferedWriter bw = new BufferedWriter(new FileWriter(graphNameFull));

        switch (code) {
            case 0: graph.saveGraph(bw); break;
            case 1: graph.saveAnnotatedGraph(bw); break;
            case 2: graph.saveGraphWithDocTitle(bw); break;
        }
        bw.close();
    }
    
    public void saveGraph(AttribNodeRelations graph, int code) throws Exception {
        saveGraph(graph, "relations", code);
    }

    public AttributeValueCollection<ArmifiedAttributeValuePair> getAttributeValueCollection() {
        return attributeValueCollection;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.err.println("Usage: java RelationGraphBuilder <prop-file>");
            args[0] = "init.properties";
        }

        try {
            Properties props = Props.loadProperties(args[0]);
            props.setProperty("prediction.source", "ie");
            RelationGraphBuilder gb = new RelationGraphBuilder(props);
            AttribNodeRelations graph = gb.getGraph(true);
            for (int i=0; i < RelationGraphBuilder.graphTypes.length; i++)
                gb.saveGraph(graph, i);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}