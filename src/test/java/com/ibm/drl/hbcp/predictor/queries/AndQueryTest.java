package com.ibm.drl.hbcp.predictor.queries;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import org.junit.BeforeClass;
import org.junit.Test;
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
public class AndQueryTest {

    public static final String smallGraphPath = "data/test/graphs/andQueryTestVectors.vec";

    private static NodeVecs smallGraph;

    @BeforeClass
    public static void loadSmallGraph() throws IOException {
        Properties prop = new Properties();
        prop.load(new FileReader(BaseDirInfo.getPath("init.properties")));
        smallGraph = new NodeVecs(new FileInputStream(smallGraphPath), prop);
    }

    @Test
    public void testExactAndQuery() {
        assertEquals(6, smallGraph.getSize());
        // queries 2 nodes we know are in the graph
        Query query = new AndQuery(Lists.newArrayList(
                new NodeQuery(AttributeValueNode.parse("C:10:1")),
                new NodeQuery(AttributeValueNode.parse("I:11:1"))
        )).filter(sr -> sr.node.getAttribute().getType() == AttributeType.OUTCOME);
        // request the top 10 results (there are only 4 possible results)
        List<SearchResult> res = query.searchTopK(smallGraph, 10);
        assertEquals(4, res.size());
        // discard the score (the order should be sufficient to test)
        List<AttributeValueNode> attributeValueNodes = res.stream().map(sr -> sr.node).collect(Collectors.toList());
        List<String> rawExpected = Lists.newArrayList("O:1:1", "O:2:1", "O:3:1", "O:4:1");
        assertEquals(rawExpected.stream().map(AttributeValueNode::parse).collect(Collectors.toList()), attributeValueNodes);
    }
}
