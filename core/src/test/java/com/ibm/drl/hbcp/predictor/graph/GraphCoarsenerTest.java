package com.ibm.drl.hbcp.predictor.graph;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.predictor.EdgeTypeSets;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;

public class GraphCoarsenerTest {

    private static final String smallJsonPath = "data/test/jsons/armified_testfile.json";

    private static RelationGraphBuilder rgb;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty("ref.json", smallJsonPath);
        properties.setProperty("prediction.source", "gt");
        properties.setProperty("prediction.graph.numeric.edges", "false");
        properties.setProperty("prediction.applyCleaning", "true");
        rgb = new RelationGraphBuilder(properties) {
            @Override
            protected List<Pair<AttributeType, AttributeType>> getValidEdgeTypes() {
                return EdgeTypeSets.OLD_VALID;
            }
        };
    }

    @Test
    public void collapseNodes() {
        // test ignoring armification
        final AttribNodeRelations graph = rgb.getGraph(false);
        final List<Attribute> numericAttributes = rgb.getAttributeValueCollection().getNumericAttributes();
        final Set<String> numericAttributeIds = numericAttributes.stream().map(e -> e.getId()).collect(Collectors.toSet());
        int nIntervals = 1;
        GraphCoarsener gc = new GraphCoarsener(graph, numericAttributeIds, nIntervals);
        final List<AttribNodeRelations.AttribNodeRelation> edges = graph.getEdges();
        assertEquals(24, edges.size());  // full graph is 24
        final List<AttribNodeRelations.AttribNodeRelation> coarsenedEdges = gc.collapseNodes();
        assertEquals(20, coarsenedEdges.size());
        final List<AttribNodeRelations.AttribNodeRelation> collectSpecificEdge = coarsenedEdges.stream().
                filter(e -> e.genKey().equals("I:3673272:1-->V:5140146:0")).collect(Collectors.toList());
        assertEquals(1, collectSpecificEdge.size());  // should only be 1 edge with that source and target
        assertEquals(4.0, collectSpecificEdge.get(0).weight, 0.0000001);  // after coarsening it should have weight of 6
        final double originalEdgeWeightSum = edges.stream().mapToDouble(e -> e.weight).sum();
        final double coarsenedEdgeWeightSum = coarsenedEdges.stream().mapToDouble(e -> e.weight).sum();
        assertEquals(originalEdgeWeightSum, coarsenedEdgeWeightSum, 0.000001);
    }
}