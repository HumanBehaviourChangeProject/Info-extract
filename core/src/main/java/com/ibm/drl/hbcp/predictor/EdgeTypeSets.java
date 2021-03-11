package com.ibm.drl.hbcp.predictor;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;

/**
 * Sets of valid edges used in the construction of graphs.
 *
 * @author mgleize
 */
public class EdgeTypeSets {

    // Old validity set used in the pass to build the graph
    public static final List<Pair<AttributeType, AttributeType>> OLD_VALID = Lists.newArrayList(
            Pair.of(AttributeType.POPULATION, AttributeType.INTERVENTION),
            Pair.of(AttributeType.POPULATION, AttributeType.OUTCOME),
            Pair.of(AttributeType.POPULATION, AttributeType.OUTCOME_VALUE), // ?? this was allowed by the original code
            Pair.of(AttributeType.INTERVENTION, AttributeType.OUTCOME),
            Pair.of(AttributeType.INTERVENTION, AttributeType.OUTCOME_VALUE),
            Pair.of(AttributeType.OUTCOME, AttributeType.OUTCOME_VALUE)
    );

    // Simple set, still gives better results in practice
    public static final List<Pair<AttributeType, AttributeType>> SIMPLE = Lists.newArrayList(
            Pair.of(AttributeType.POPULATION, AttributeType.INTERVENTION),
            Pair.of(AttributeType.INTERVENTION, AttributeType.OUTCOME_VALUE)
    );

    public static List<Pair<AttributeType, AttributeType>> getAllEdgeTypes() {
        List<Pair<AttributeType, AttributeType>> res = new ArrayList<>();
        AttributeType[] types = AttributeType.values();
        for (int i = 0; i < types.length; i++) {
            for (int j = 0; j < i; j++) {
                res.add(Pair.of(types[i], types[j]));
            }
        }
        return res;
    }
}
