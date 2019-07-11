package com.ibm.drl.hbcp.predictor.graph;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import com.ibm.drl.hbcp.util.Props;
import org.junit.BeforeClass;

public class RelationGraphBuilderTest {

    private static RelationGraphBuilder rgb;

    @BeforeClass
    public static void setUpBeforeClass() throws Exception {
        rgb = new RelationGraphBuilder(Props.loadProperties("test.properties"));
        rgb.getGraph();
    }

//    @Test
//    public void testGetNodeIdInstanceMap() {
//        Map<AttributeValueNode, Collection<AttributeInstInfo>> nodeIdInstanceMap = rgb.getNodeIdInstanceMap();
//        assertNotNull(nodeIdInstanceMap);
//        Collection<AttributeInstInfo> collection = nodeIdInstanceMap.get(AttributeValueNode.parse("C:4507561:6.7"));
//        assertNotNull(collection);
//        collection = nodeIdInstanceMap.get(AttributeValueNode.parse("O:4087172:smoking"));
//        assertNotNull(collection);
//        AttributeInstInfo dummy = new AttributeInstInfo("smoking", "Brown 1992.pdf", "After 3 and 6 months, all participants were telephoned to ascertain whether they had stopped smoking, how many times they had attempted to stop smoking, how many cigarettes they smoked, and if they were contemplating making further cessation attempts.", 3);
//        assertTrue(collection.contains(dummy));
//    }

}
