/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.graph;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

import static com.ibm.drl.hbcp.core.attributes.normalization.Normalizers.getAttributeIdsFromProperty;

import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations.AttribNodeRelation;
import java.io.FileReader;
import java.util.*;

/**
 * Coarsens a graph by creating nodes for intervals of values instead of
 * a single node for a particular value.
 * 
 * @author dganguly
 */

// Applicable for numeric-type nodes only.. Useful for coarsening
class MinMaxStats {
    String id;
    float min;
    float max;

    public MinMaxStats(String id) {
        this.id = id;
        max = 0;
        min = Float.MAX_VALUE;
    }
    
    void update(AttributeValueNode node) {
        float val = 0;
        Double valObj = node.getNumericValue();
        if (valObj != null)
            val = valObj.floatValue();
        if (valObj!=null && val < min)
            min = val;
        if (valObj!=null && val > max)
            max = val;
    }
    
    String getAttribId() { return id; }
    
    int computeOffset(AttributeValueNode node, int numIntervals) {
        float val = 0;
        Double valObj = node.getNumericValue();
        if (valObj == null)
            return -1;  // invalid offset
        val = valObj.floatValue();
        
        float delta = (max - min)/(float)numIntervals;
        return (int)((val - min)/delta);  // the bin id at which this value falls
    }
}

public class GraphCoarsener {
    private Set<String> numericalAttributes;
    private int numIntervals;
    private AttribNodeRelations graph;
    AttribNodeRelations modifiedGraph;
    
    private Map<String, MinMaxStats> minMaxStatsMap;  // keyed by attribute id

    public GraphCoarsener(AttribNodeRelations graph, Set<String> numericalAttributes, int numIntervals) {
        this.graph = graph;
        this.numericalAttributes = numericalAttributes;
        this.numIntervals = numIntervals;

        minMaxStatsMap = new HashMap<>();
    }
    
    // the main driver function
    public List<AttribNodeRelation> collapseNodes() {
        List<AttribNodeRelation> edges = graph.getEdges();

        // Construct equivalence classes (min/max interval binning)
        for (AttribNodeRelation edge: edges) {
            updateEqClass(edge.source);
            updateEqClass(edge.target);
        }

        // coarsen values in edge nodes
        List<AttribNodeRelation> coarsenedEdges = new ArrayList<>(edges.size());
        for (AttribNodeRelation edge: edges) {
            AttributeValueNode source = edge.source;
            AttributeValueNode target = edge.target;
            if (numericalAttributes.contains(edge.source.getId())) {
                source = collapseNode(edge.source);
            }
            if (numericalAttributes.contains(edge.target.getId())) {
                target = collapseNode(edge.target);
            }
            coarsenedEdges.add(new AttribNodeRelation(edge.getDocName(), source, target, (float)edge.weight));
        }

        // after coarsening, we can have multiple edges with the same source and target
        Collections.sort(coarsenedEdges);
        final ListIterator<AttribNodeRelation> iterator = coarsenedEdges.listIterator();
        if (iterator.hasNext()) {
            AttribNodeRelation currEdge = iterator.next();
            while (iterator.hasNext()) {
                AttribNodeRelation nextEdge = iterator.next();
                if (currEdge.equals(nextEdge)) {
                    currEdge.weight += nextEdge.weight;  // update weight
                    iterator.remove();
                } else {
                    currEdge = nextEdge;
                }
            }
        }

        modifiedGraph = new AttribNodeRelations();
        for (AttribNodeRelation edge : coarsenedEdges) {
            modifiedGraph.add(edge, 0.0f);  // edges have correct weight, so do not accumulate weight making graph from edges
        }
        return coarsenedEdges;
    }
    
    private void updateEqClass(AttributeValueNode node) {
        String nodeId;
        nodeId = node.getId();
        if (numericalAttributes.contains(nodeId)) {
            MinMaxStats inode = minMaxStatsMap.get(nodeId);
            if (inode == null) {
                inode = new MinMaxStats(nodeId);
                minMaxStatsMap.put(nodeId, inode);
            }
            inode.update(node); // update min-max
        }
    }
    
    private AttributeValueNode collapseNode(AttributeValueNode node) {
        String nodeId = node.getId();

        // Get the interval node corresponding to this node
        MinMaxStats interval = minMaxStatsMap.get(nodeId);
        if (interval == null) {
            return node;
        }
        int offset = interval.computeOffset(node, numIntervals);
        AttributeValuePair avp = new AttributeValuePair(node.getAttribute(), ""+offset);
        return new AttributeValueNode(avp);
    }

    public static void main(String[] args) {
        
        try {
            Properties prop = new Properties();
            prop.load(new FileReader("init.properties"));
            Set<String> numericalAttributes = numericalAttributes = getAttributeIdsFromProperty(prop, "prediction.attribtype.numerical");
            
            RelationGraphBuilder gb = new RelationGraphBuilder(prop);
            AttribNodeRelations graph = gb.getGraph(false);
            
            int nIntervals = Integer.parseInt(prop.getProperty("prediction.graph.coarsening.normalization.numintervals", "50"));
            GraphCoarsener gc = new GraphCoarsener(graph, numericalAttributes, nIntervals);
            gc.collapseNodes();
            
            gb.saveGraph(gc.modifiedGraph, "coarsened", 0);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
