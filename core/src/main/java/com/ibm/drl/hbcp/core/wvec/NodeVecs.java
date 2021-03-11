/**
 * 
 */
package com.ibm.drl.hbcp.core.wvec;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.util.ParsingUtils;

import lombok.Getter;

/**
 * A collection of vectorized attribute instances. The result of the node2vec algorithm applied on a graph
 * of these attribute instances.
 * @author charlesj
 */
public class NodeVecs extends WordVecs {

    public Map<AttributeType, Map<String, WordVec>> attTypeVecMap;
    NodeMetric metric;

    // TODO add the min values too at some point
    private final Map<Attribute, Double> maxValuesPerAttribute;
    @Getter
    private final Set<String> attributeIds;

    public NodeVecs(InputStream inputFile, Properties prop) throws IOException {
        super(inputFile, prop);
        metric = new NodeMetric(prop);
        groupVectorsByAttributeType();
        maxValuesPerAttribute = getMaxValuesPerAttribute();
        attributeIds = buildAttributeIds();
    }

    // TODO: remove every dependency to Properties, just replace this with the actual typed arguments you need
    public NodeVecs(Properties prop) throws IOException {
        this(new FileInputStream(new File(prop.getProperty("nodevecs.vecfile"))), prop);
    }
    
    /* DG: loadFromTextFile is called from the constructor... so this is an instance
        where a derived class is initialized by calling a constructor of base class
        which again delegates the control back to the derived class (which is
        still not created yet!)
    */
    @Override
    void loadFromTextFile(InputStream wordvecFile) {
        loadFromInputStream(wordvecFile);
    }

    protected void loadFromInputStream(InputStream input) {
        wordvecmap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            String line;
            // have to skip first line
            line = br.readLine();

            while ((line = br.readLine()) != null) {
                WordVec wv = new WordVec(line);
                wordvecmap.put(wv.word, wv);
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public int getDimension() {
        return Integer.parseInt(prop.getProperty("node2vec.layer1_size", "128"));
    }
    
    private void groupVectorsByAttributeType() {
        attTypeVecMap = new HashMap<>();
        // for POPULATION, INTERVENTION, OUTCOME, and OUTCOME VALUES
        // but it doesn't hurt to put EFFECT too
        for (AttributeType t : AttributeType.values()) {
            attTypeVecMap.put(t, new HashMap<String, WordVec>());
        }
        
        for (Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            AttributeType attributeType = AttributeValueNode.parse(entry.getKey()).getAttribute().getType();
            Map<String, WordVec> map = attTypeVecMap.get(attributeType);
            if (map != null) {
                map.put(entry.getKey(), entry.getValue());
            }
        }
        
    }

    private Map<Attribute, Double> getMaxValuesPerAttribute() {
        Map<Attribute, Double> res = new HashMap<>();
        for (String nodeId : attTypeVecMap.values().stream().flatMap(map -> map.keySet().stream()).collect(Collectors.toSet())) {
            AttributeValuePair avp = AttributeValueNode.parse(nodeId);
            Double numericValue = getNumericValue(nodeId);
            if (numericValue != null) {
                res.putIfAbsent(avp.getAttribute(), Double.NEGATIVE_INFINITY);
                res.put(avp.getAttribute(), Double.max(res.get(avp.getAttribute()), numericValue));
            }
        }
        return res;
    }

    private Set<String> buildAttributeIds() {
        Set<String> res = new HashSet<>();
        for (String nodeId : attTypeVecMap.values().stream().flatMap(map -> map.keySet().stream()).collect(Collectors.toSet())) {
            AttributeValuePair avp = AttributeValueNode.parse(nodeId);
            res.add(avp.getAttribute().getId());
        }
        return res;
    }

    /** return the numeric value if numeric node, null otherwise
     * assumes nodeInstName is always of the form type:id:value */
    public static Double getNumericValue(String nodeInstName) {
        try {
            String[] tokens = nodeInstName.split(AttributeValueNode.DELIMITER);
            if (tokens.length == 2)
                return null;
            
            return ParsingUtils.parseFirstDouble(tokens[2]);
        }
        catch (NumberFormatException e) {
            return null;
        }
    }

    /**
     * Returns node instances of specified attribute type (e.g., context, intervention, outcome),
     * indexed by AttributeValueNode string representation
     * */
    public Map<String, WordVec> getNodeInstancesByType(AttributeType attType) {
        Map<String, WordVec> vecMap = attTypeVecMap.get(attType);
        if (vecMap != null)
            return vecMap;
        else
            return new HashMap<>();
    }

    /**
     * Returns node instances of specified attribute type (e.g., context, intervention, outcome) and attribute ID,
     * indexed by AttributeValueNode string representation
     * */
    public Map<String, WordVec> getNodeInstancesByTypeAndAttributeId(AttributeType attType, String attId) {
        Map<String, WordVec> res = new HashMap<>();
        for (Map.Entry<String, WordVec> entry : getNodeInstancesByType(attType).entrySet()) {
            if (attId.equals(AttributeValueNode.parse(entry.getKey()).getAttribute().getId()))
                res.put(entry.getKey(), entry.getValue());
        }
        return res;
    }

    public boolean hasNodeInstance(String attributeInstance) {
        return wordvecmap.containsKey(attributeInstance);
    }
    
    
    public WordVec getNodeInstanceVector(String attributeInstance) {
        return wordvecmap.get(attributeInstance);
    }

    /** Returns a list of closest node attributes based on some metric --
    1. For categorical type attributes -- exact match with one of the allowed category types --- MUST be defined
       in the properties file...
    2. For some types of attributes -- rely on specifically defined ways on the normal forms to get the closest, e.g.
       for mixed gender, the normal form is "M (%ge) F (%ge)"... in which case get the closest as a two dimesnional vector distance */
    public List<String> getClosestAttributeInstancesForNonNumericNodes(AttributeValueNode node) {
        String attributeInstance = node.toString();
        AttributeValueNode parsed = AttributeValueNode.parse(attributeInstance);
        AttributeType type = parsed.getAttribute().getType();
        String attributeId = parsed.getAttribute().getId();
        
        Map<String, WordVec> seenNodesMap = getNodeInstancesByTypeAndAttributeId(type, attributeId);  // seen nodes from graph
        List<WordVec> closestNodes = new ArrayList<>();
        Map<WordVec, Double> sims = new HashMap<>();
        List<String> closestNodeIds = new ArrayList<>();
        
        for (WordVec seenNode : seenNodesMap.values()) {
            double querySim = metric.getDistance(node, seenNode.word);
            sims.put(seenNode, querySim);
            closestNodes.add(seenNode);
        }
        Collections.sort(closestNodes, new Comparator<WordVec> () {
            @Override
            public int compare(WordVec a, WordVec b) { // reverse order... sort by distance
                return - Double.compare(sims.get(a), sims.get(b));
            }
        });
        
        for (WordVec w: closestNodes) {
            closestNodeIds.add(w.word);
        }
        
        return closestNodeIds;
    }

    /** Return the closest nodes (by numerical value) of the same attribute ID
     * or null if the attribute is not numeric or if no node of such ID has been found */
    public List<String> getClosestAttributeInstances(String attributeInstance) {
        // if non-numeric, we can't find anything "close"
        Double value = getNumericValue(attributeInstance);
        if (value == null) {
            return Lists.newArrayList();
        }
        
        // then look for closest of same type and attribute ID
        AttributeValueNode parsed = AttributeValueNode.parse(attributeInstance);
        AttributeType type = parsed.getAttribute().getType();
        String attributeId = parsed.getAttribute().getId();
        Map<String, WordVec> nodes = getNodeInstancesByTypeAndAttributeId(type, attributeId);
        return nodes.entrySet().stream()
                // filter out non-numeric nodes
                .filter(nodeIdAndVector -> getNumericValue(nodeIdAndVector.getKey()) != null)
                .map(Map.Entry::getKey)
                // sort by closest numeric values
                .sorted(new Comparator<String>() {
                    @Override
                    public int compare(String o1, String o2) {
                        // no NPE possible here, we already filtered this above
                        double dist1 = Math.abs(value - getNumericValue(o1));
                        double dist2 = Math.abs(value - getNumericValue(o2));
                        return Double.compare(dist1, dist2);
                    }
                })
                .collect(Collectors.toList());
    }
    
    /** Return the closest nodes of the same attribute ID
     * or null if the attribute is not numeric or if no node of such ID has been found */
    public List<String> getClosestAttributeInstances(AttributeValueNode node) {
        List<String> nearestNodesIdentifiers = getClosestAttributeInstances(node.toString());
        if (nearestNodesIdentifiers.size() > 0)
            return nearestNodesIdentifiers;
        
        // Handle non numerical types
        return getClosestAttributeInstancesForNonNumericNodes(node);
    }

//    private <K, V> Collector<? super Entry<K, V>, ?, Map<K, V>> toMap() {
//        return Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue);
//    }

    public long getSize() { return attTypeVecMap.values().stream().flatMap(map -> map.values().stream()).count(); }

    public Double getMaxValue(Attribute attribute) {
        return maxValuesPerAttribute.get(attribute);
    }
    
    /**
     * @param args
     */
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            args[0] = "init.properties";
        }
    
        // create the JSON com.ibm.drl.hbcp.parser
        Properties prop = new Properties();
        try {
        	prop.load(NodeVecs.class.getClassLoader().getResourceAsStream(args[0]));
    		// prop.load(new FileReader(args[0]));
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            // Prepare testing by loading the graph from the edge-list file
            /*
            Graph graph = Graph.fromFile(new File("prediction/graphs/relations.graph"));
            recommender.graph.Node2Vec node2Vec = new recommender.graph.Node2Vec(graph, prop);
            // rewrite the node2vec file
            node2Vec.trainAndSaveNodeVecs("prediction/graphs/nodevecs/refVecs.vec");
            */
            
            NodeVecs nodeVecs = new NodeVecs(prop);
            String nodeInstance = "C:12345:18";
            System.out.println("Graph has instance " + nodeInstance + ": " + nodeVecs.hasNodeInstance(nodeInstance));
            nodeInstance = "4087178";
            System.out.println("Graph has instance " + nodeInstance + ": " + nodeVecs.hasNodeInstance(nodeInstance));
            nodeInstance = "C:4507428:37.95";
            System.out.println("Graph has instance " + nodeInstance + ": " + nodeVecs.hasNodeInstance(nodeInstance));
            nodeInstance = "C:4507428:M";
            
            /*
            System.out.println("Closest instance to " + nodeInstance + " is " + nodeVecs.getClosestAttributeInstances(nodeInstance).get(0));
            nodeInstance = "C:4507428:37";
            System.out.println("Closest instance to " + nodeInstance + " is " + nodeVecs.getClosestAttributeInstances(nodeInstance).get(0));
            System.out.println("Graph has " + nodeVecs.wordvecmap.size() + " instances.");
            System.out.println("  number of Context instances: " + nodeVecs.getNodeInstancesByType(JSONRefParser.POPULATION).size());
            System.out.println("  number of Intervention instances: " + nodeVecs.getNodeInstancesByType(JSONRefParser.INTERVENTION).size());
            System.out.println("  number of Outcome instances: " + nodeVecs.getNodeInstancesByType(JSONRefParser.OUTCOME).size());
            */
            
            // Testing the Mixed Gender types
            
            //nodeInstance = "C:4507427:M_(75.8)_F_(24.2)";
            nodeInstance = "C:5579088:22.8";
            
            AttributeValueNode qnodeAttributeValue = AttributeValueNode.parse(nodeInstance);
            List<String> nnList = nodeVecs.getClosestAttributeInstances(qnodeAttributeValue);
            System.out.println(nnList.size());
            
            for (int i=0; i < Math.min(5, nnList.size()); i++) {
                System.out.println("Closest instance to " + nodeInstance + " is " + nnList.get(i));
            }
            
        }
        catch (Exception e) {
            e.printStackTrace();
        }
    
    }
}