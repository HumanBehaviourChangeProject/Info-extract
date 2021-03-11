package com.ibm.drl.hbcp.predictor.graph;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.predictor.EdgeTypeSets;

public class RelationGraphBuilderTest {

    private static final String smallJsonPath = "data/test/jsons/armified_testfile.json";
    private static final List<String> VALID_ARMIFIED_EDGES = Arrays.asList(
            "C:113:37_years_old-->I:3673272:1",
            "C:113:37_years_old-->V:5140146:15.3",
            "C:113:37_years_old-->V:5140146:7.4",
            "C:122:all_female_indeed-->I:3673272:1",
            "C:122:all_female_indeed-->V:5140146:15.3",
            "C:122:all_female_indeed-->V:5140146:7.4",
            "I:3673272:1-->V:5140146:15.3",
            "C:111:47.3_years-->I:3673271:1",
            "C:111:47.3_years-->I:3673272:1",
            "C:111:47.3_years-->V:5140146:26.7",
            "C:111:47.3_years-->V:5140146:12.9",
            "C:121:but_still_mostly_male-->I:3673271:1",
            "C:121:but_still_mostly_male-->I:3673272:1",
            "C:121:but_still_mostly_male-->V:5140146:26.7",
            "C:121:And_way_too_manly-->I:3673271:1",
            "C:121:And_way_too_manly-->I:3673272:1",
            "C:121:And_way_too_manly-->V:5140146:12.9",
            "I:3673271:1-->V:5140146:26.7",
            "I:3673271:1-->V:5140146:12.9",
            "I:3673272:1-->V:5140146:12.9",
            "I:3673272:1-->V:5140146:26.7"
            );
    private static RelationGraphBuilder rgb;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        final Properties properties = new Properties();
        properties.setProperty("ref.json", smallJsonPath);
        properties.setProperty("prediction.source", "gt");
        properties.setProperty("prediction.graph.numeric.edges", "false");
        rgb = new RelationGraphBuilder(properties) {
            @Override
            protected List<Pair<AttributeType, AttributeType>> getValidEdgeTypes() {
                return EdgeTypeSets.OLD_VALID;
            }
        };
    }

    @Test
    public void testGetGraph() {
        // test ignoring armification
        final AttribNodeRelations graph = rgb.getGraph(false);
        final List<AttribNodeRelations.AttribNodeRelation> edges = graph.getEdges();
        assertEquals(24, edges.size());
        // test with armification
        final AttribNodeRelations armifiedGraph = rgb.getGraph(true);
        final List<AttribNodeRelations.AttribNodeRelation> armifiedEdges = armifiedGraph.getEdges();
        assertEquals(21, armifiedEdges.size());
        for (AttribNodeRelations.AttribNodeRelation armifiedEdge : armifiedEdges) {
            assertTrue(VALID_ARMIFIED_EDGES.contains(armifiedEdge.genKey()));
        }
    }

    @Test
    public void addEdgesBetweenSameTypes() throws IOException {
        // need a new RGB with edges between same type
        final Properties properties = new Properties();
        properties.setProperty("ref.json", smallJsonPath);
        properties.setProperty("prediction.source", "gt");
        properties.setProperty("prediction.graph.numeric.edges", "true");
        RelationGraphBuilder rgbSame = new RelationGraphBuilder(properties) {
            @Override
            protected List<Pair<AttributeType, AttributeType>> getValidEdgeTypes() {
                return EdgeTypeSets.OLD_VALID;
            }
        };
        // test ignoring armification
        final AttribNodeRelations graph = rgbSame.getGraph(false);
        final List<AttribNodeRelations.AttribNodeRelation> edges = graph.getEdges();
        // six new edges are added between outcome value nodes (both with and without armification)
        assertEquals(30, edges.size());
        final List<String> edgeIds = edges.stream().map(e -> e.genKey()).collect(Collectors.toList());
        assertTrue(edgeIds.contains("V:5140146:12.9-->V:5140146:26.7") ||
                edgeIds.contains("V:5140146:26.7-->V:5140146:12.9"));  // not sure of direction
        // test with armification
        final AttribNodeRelations armifiedGraph = rgbSame.getGraph(true);
        final List<AttribNodeRelations.AttribNodeRelation> armifiedEdges = armifiedGraph.getEdges();
        assertEquals(27, armifiedEdges.size());
    }
}
