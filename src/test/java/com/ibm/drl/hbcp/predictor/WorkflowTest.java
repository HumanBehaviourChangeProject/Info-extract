package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.Test;

import java.util.List;
import java.util.Properties;

/**
 * Checks that the basic Java-only Workflow run correctly.
 */
public class WorkflowTest {

    @Test
    public void testWekaRegressionFlow() throws Exception {
        WekaRegressionFlow.main(new String[0]);
    }

    // this test is too expensive memory-wise to run
    //@Test
    public void testNearestNeighborQueryFlow() throws Exception {
        // replace the usual edge set with a very simple one (graph building should be faster with a small graph)
        NearestNeighborQueryFlow flow = new NearestNeighborQueryFlow() {
            @Override
            protected RelationGraphBuilder getRelationGraphBuilder(Properties props, AttributeValueCollection<ArmifiedAttributeValuePair> values) {
                return new RelationGraphBuilder(props, values) {
                    @Override
                    protected List<Pair<AttributeType, AttributeType>> getValidEdgeTypes() {
                        return EdgeTypeSets.SIMPLE;
                    }
                };
            }
        };
        flow.run();
    }
}
