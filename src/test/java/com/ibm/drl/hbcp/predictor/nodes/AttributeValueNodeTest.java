package com.ibm.drl.hbcp.predictor.nodes;

import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import org.junit.Test;

import static org.junit.Assert.*;

public class AttributeValueNodeTest {

    @Test
    public void testParse() {
        String[] validParses = {"C:1234:OK", "O:1:1", "I:42:spaces_replaced", "O:23:Newline_?", "C:1:"};
        for (String nodeStr : validParses) {
            AttributeValueNode attributeValueNode = AttributeValueNode.parse(nodeStr);
            assertNotNull(attributeValueNode);
            assertEquals(nodeStr, attributeValueNode.toString());
        }
        String[] invalidParses = {"", " ", "T:1234:OK", "c:1:", "::", "CO:1:1", "O::Not ok"};
        for (String nodeStr : invalidParses) {
            try {
                AttributeValueNode attributeValueNode = AttributeValueNode.parse(nodeStr);
                fail("This Node identifier should crash AttributeValueNode.parse: " + nodeStr);
            } catch (RuntimeException e) { }
        }
        String[] withWhitespace = {"C:11111111111111:Here is space", "O:34:Newline\n?"};
        for (String nodeStr : withWhitespace) {
            AttributeValueNode attributeValueNode = AttributeValueNode.parse(nodeStr);
            assertNotNull(attributeValueNode);
            assertEquals(nodeStr.replaceAll("\\s+", "_"), attributeValueNode.toString());
        }
    }

}
