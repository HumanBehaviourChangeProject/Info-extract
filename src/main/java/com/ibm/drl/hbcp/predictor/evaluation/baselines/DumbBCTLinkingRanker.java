package com.ibm.drl.hbcp.predictor.evaluation.baselines;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.util.ParsingUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class DumbBCTLinkingRanker extends RealisticIdealTop1Ranker {

    private final Random rand = new Random(82736438);
    protected final Map<AttributeValueNode, List<AttributeValueNode>> outcomeValuesAndLinkedValues;

    public DumbBCTLinkingRanker(Properties prop, AttribNodeRelations test, AttribNodeRelations train) throws IOException {
        super(prop, test, train);
        outcomeValuesAndLinkedValues = getOutcomeValuesAndLinkedValues(train, avn -> true);
    }

    @Override
    protected List<AttributeValueNode> getClosestOutcomeValueInTrain(NormalizedAttributeValuePair referenceValue, Multiset<NormalizedAttributeValuePair> otherValuesInDoc) {
        double numericValue = ParsingUtils.parseFirstDouble(referenceValue.getValue());
        Set<String> allBCTsInDoc = getBCTIds(otherValuesInDoc);
        List<AttributeValueNode> closestNumericValuesInTrain = outcomeValuesAndLinkedValues.entrySet().stream()
                .filter(entry -> entry.getKey().isNumeric())
                .map(entry -> Pair.of(entry.getKey(), entry.getValue().stream().filter(avn -> contains(otherValuesInDoc, avn)).count()))
                .sorted(Comparator.comparing(avnAndSharedCount -> - avnAndSharedCount.getRight()))
                .map(Map.Entry::getKey)
                .collect(Collectors.toList());
        //Collections.shuffle(closestNumericValuesInTrain, rand);
        if (closestNumericValuesInTrain.isEmpty()) {
            return Lists.newArrayList(new AttributeValueNode(new AttributeValuePair(referenceValue.getAttribute(), meanTrainOutcomeValue)));
        }
        return closestNumericValuesInTrain;
    }
}
