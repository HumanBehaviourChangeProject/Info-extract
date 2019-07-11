package com.ibm.drl.hbcp.predictor.queries;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import org.junit.BeforeClass;
import org.junit.Test;
import com.ibm.drl.hbcp.predictor.graph.Graph;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;

import java.io.*;
import java.util.List;
import java.util.Properties;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Unit tests for specifically the "NodeQuery", which returns the closest nodes (by name) already in the graph
 *
 * @author marting
 */
public class NodeQueryTest {

    public static final String smallNumericGraphPath = "data/test/graphs/numericSmallGraph.graph";

    private static NodeVecs smallNumericGraph;

    public static final String[] QUERIES = {
            "C:1:0", "C:1:1000", "C:2:6", "I:1:1", "C:3:1"
    };

    public static final String[] EXPECTED = {
            "C:1:1", "C:1:100", "C:2:5", null, null
    };

    public static final String FULL_QUERY = "C:2:9";
    public static final List<String> FULL_EXPECTED = Lists.newArrayList(
            "C:2:10", "C:2:5", "C:2:4", "C:2:3", "C:2:2", "C:2:1", "C:2:20", "C:2:50", "C:2:100"
    );

    @BeforeClass
    public static void loadSmallGraph() throws IOException {
        Properties prop = new Properties();
        prop.load(new FileReader(BaseDirInfo.getPath("init.properties")));
        Node2Vec node2vec = new Node2Vec(AttribNodeRelations.fromFile(new File(smallNumericGraphPath)), prop);
        smallNumericGraph = node2vec.getNodeVectors();
    }

    @Test
    public void testTopResult() {
        assertNotEquals(0, smallNumericGraph.getSize());
        for (int i = 0; i < QUERIES.length; i++) {
            assertEquals(EXPECTED[i], getSingleResult(QUERIES[i]));
        }
    }

    @Test
    public void testFullResult() {
        assertEquals(FULL_EXPECTED,
                new NodeQuery(AttributeValueNode.parse(FULL_QUERY))
                        .search(smallNumericGraph)
                        .stream()
                        .map(res -> res.node.toString())
                        .collect(Collectors.toList()));
    }

    @Test
    public void testResultScoreConsistency() {
        List<SearchResult> results = new NodeQuery(AttributeValueNode.parse(FULL_QUERY)).search(smallNumericGraph);
        assertNotEquals(0, results.size());
        for (int i = 0; i < results.size() - 1; i++) {
            assertTrue(results.get(i).score >= results.get(i + 1).score);
        }
    }

    private String getSingleResult(String queryNode) {
        NodeQuery query = new NodeQuery(AttributeValueNode.parse(queryNode));
        List<SearchResult> res = query.search(smallNumericGraph);
        if (res.isEmpty()) return null;
        else return res.get(0).node.toString();
    }

}
