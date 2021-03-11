package com.ibm.drl.hbcp.predictor.graph;

import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Wrapper class for collection of attribute node relations, which form the edges of the graph.
 * The collection is a mapping from a relation/edge id (i.e., nodeId1:nodeId2) to the relation/edge
 * object {@link AttribNodeRelation}.
 *
 * Nodes are AttributeValueNode objects and edges are weighted with a double.
 * 
 * @author dganguly
 *
 */
public class AttribNodeRelations implements Graph<AttributeValueNode> {
    private Map<String, AttribNodeRelation> nodeRelations;  // the key is an edge encoding of the form "a:b", a and b being the vertices

    public AttribNodeRelations() {
        nodeRelations = new HashMap<>();
    }

    public AttribNodeRelation getEdge(AttributeValueNode a, AttributeValueNode b) {
        AttribNodeRelation key = new AttribNodeRelation(a, b, 0); // weight of 0 is a placeholder
        return nodeRelations.get(key.genKey());
    }

    @Override
    public List<AttribNodeRelation> getEdges() {
        List<AttribNodeRelation> res = new ArrayList<>(nodeRelations.values());
        Collections.sort(res);
        return res;
    }

    /**
     * Add node relation to collection.
     * 
     * @param nodeRelation
     */
    public void add(AttribNodeRelation nodeRelation) {
        add(nodeRelation, 1);
    }

    /**
     * Add node relation to collection.
     * 
     * @param nodeRelation
     * @param wt
     */
    public void add(AttribNodeRelation nodeRelation, float wt) {
        String key = nodeRelation.genKey();
        AttribNodeRelation seen = nodeRelations.get(key);
        if (seen == null) {
            nodeRelations.put(key, nodeRelation);
            seen = nodeRelation;
        }
        seen.accumulateWeight(wt);
    }

    public String getAllOutcomeValues(String id) {
        return StringUtils.join(getEdges().stream()
                .flatMap(edge -> Lists.newArrayList(edge.source, edge.target).stream())
                .filter(avn -> avn.getAttribute().getId().equals(id))
                .collect(Collectors.toSet()).stream()
                .sorted(Comparator.comparing(avn -> avn.getNumericValue())), " | ");
    }
    
    /**
     * Write graph to {@link BufferedWriter} with nodes and edge weight
     * 
     * @param bw
     * @throws Exception
     */
    void saveGraph(BufferedWriter bw) throws Exception {
        for (AttribNodeRelation r: nodeRelations.values()) {
            bw.write(r.toString());
            bw.newLine();
        }
    }
    
    /**
     * Write graph to {@link BufferedWriter} with nodes edge weight and a document name
     * 
     * @param bw
     * @throws Exception
     */
    void saveGraphWithDocTitle(BufferedWriter bw) throws Exception {
        for (AttribNodeRelation r: nodeRelations.values()) {
            bw.write(r.toString()+"\t"+r.getDocName());
            bw.newLine();
        }
    }

    /**
     * Build graph from file.
     * <br>
     * Graph should have one edge per line. Each line has two nodes and a weight (i.e., 3 columns) separated by tab.
     *
     * @param file
     * @return a new graph
     * @throws IOException
     */
    public static Graph fromFile(File file) throws IOException {
        List<Edge> res = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new FileReader(file))) {
            String line;
            while ((line = br.readLine()) != null) {
                String[] splits = line.split("\\t");
                Edge<AttributeValueNode> edge = new Edge<AttributeValueNode>(
                        AttributeValueNode.parse(splits[0]),
                        AttributeValueNode.parse(splits[1]),
                        Double.parseDouble(splits[2])
                );
                res.add(edge);
            }
        }
        return () -> res;
    }
    
    /**
     * @deprecated this is doing the same as {@link #saveGraphWithDocTitle(BufferedWriter)}
     * 
     * Write graph to {@link BufferedWriter} with nodes edge weight and a document name
     * 
     * @param bw
     * @throws Exception
     */
    @Deprecated
    void saveAnnotatedGraph(BufferedWriter bw) throws Exception {
        for (AttribNodeRelation r: nodeRelations.values()) {
            bw.write(r.toAnnotatedString());
            bw.newLine();
        }
    }

    @Override
    public String toString() {
        return toText();
    }

    public Map<String, AttribNodeRelation> getNodeRelations() { return nodeRelations; }

    /**
     * Edge in the graph, with two nodes, a weight, and optionally a docname where this relation occurs.
     *
     */
    public static class AttribNodeRelation extends Edge<AttributeValueNode> implements Comparable<AttribNodeRelation> {
        public final String docName;

        public AttribNodeRelation(String docName, AttributeValueNode a, AttributeValueNode b, float weight) {
            super(a, b, weight);
            this.docName = docName;
        }

        public AttribNodeRelation(String docName, AttributeValueNode a, AttributeValueNode b) {
            this(docName, a, b, 0.0f);
        }
        
        public AttribNodeRelation(AttributeValueNode a, AttributeValueNode b, float weight) {
            this(null, a, b, weight);
        }
        
        public String getDocName() { return this.docName; }
        
        /**
         * Increment weight by 1
         */
        void accumulateWeight() {
            this.weight++;
        }
        
        /**
         * Increment weight by delw
         */
        void accumulateWeight(float delw) {
            this.weight += delw;
        }
        
        /**
         * Generates a key for the relation; concatenating nodeIds with colon (:)
         * 
         * @return
         */
        String genKey() {
            assert(source.getValue() != null && target.getValue() != null);
            return source.toString() + "-->" + target.toString();
//            return source.getValue() + ":" + target.getValue();
        }

        // +++DG: Added the doc name in case it's reqd. in the flow...
        // this should be needed any longer as calling method is deprecated
        @Deprecated
        public String toAnnotatedString() { // print names instead of ids
            return toString() + "\t" + docName;
        }

        @Override
        public int compareTo(@NotNull AttribNodeRelation o) {
            return genKey().compareTo(o.genKey());
        }
        
        @Override
        public String toString() {
            return source + "\t" + target + "\t" + weight + "\t" + docName;
        }
        
    }
}