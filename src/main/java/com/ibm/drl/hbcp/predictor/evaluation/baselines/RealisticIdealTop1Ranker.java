package com.ibm.drl.hbcp.predictor.evaluation.baselines;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.util.ParsingUtils;

import java.io.IOException;
import java.util.*;
import java.util.function.Predicate;
import java.util.stream.Collectors;

public class RealisticIdealTop1Ranker extends IdealTop1Ranker {

    protected final Map<AttributeValueNode, List<AttributeValueNode>> outcomeValuesAndTheirBCTs;

    public RealisticIdealTop1Ranker(Properties prop, AttribNodeRelations test, AttribNodeRelations train) throws IOException {
        super(prop, test, train);
        outcomeValuesAndTheirBCTs = getOutcomeValuesAndTheirBCTs(train);
    }

    @Override
    protected List<AttributeValueNode> getClosestOutcomeValueInTrain(NormalizedAttributeValuePair referenceValue, Multiset<NormalizedAttributeValuePair> otherValuesInDoc) {
        double numericValue = ParsingUtils.parseFirstDouble(referenceValue.getValue());
        Set<String> allBCTsInDoc = getBCTIds(otherValuesInDoc);
        List<AttributeValueNode> closestNumericValuesInTrain = outcomeValuesAndTheirBCTs.entrySet().stream()
                .filter(entry -> entry.getKey().isNumeric())
                .filter(entry -> entry.getValue().stream().anyMatch(avp -> allBCTsInDoc.contains(avp.getAttribute().getId())))
                .map(Map.Entry::getKey)
                .sorted(Comparator.comparing(avp -> Math.abs(numericValue - avp.getNumericValue())))
                .collect(Collectors.toList());
        if (closestNumericValuesInTrain.isEmpty()) {
            return super.getClosestOutcomeValueInTrain(referenceValue, otherValuesInDoc);
        }
        return closestNumericValuesInTrain.subList(0, 1);
    }

    private Map<AttributeValueNode, List<AttributeValueNode>> getOutcomeValuesAndTheirBCTs(AttribNodeRelations train) {
        return getOutcomeValuesAndLinkedValues(train, avn -> avn.getAttribute().getType() == AttributeType.INTERVENTION);
    }

    protected Map<AttributeValueNode, List<AttributeValueNode>> getOutcomeValuesAndLinkedValues(AttribNodeRelations train, Predicate<AttributeValueNode> sourceFilter) {
        Map<AttributeValueNode, List<AttributeValueNode>> res = new HashMap<>();
        for (AttribNodeRelations.AttribNodeRelation edge : train.getEdges()) {
            if (edge.target.getAttribute().getId().equals(targetOutcomeValueAttributeId)) {
                // we found a valid outcome value, we get the BCTs associated to it
                if (sourceFilter.test(edge.source)) {
                    res.putIfAbsent(edge.target, new ArrayList<>());
                    res.get(edge.target).add(edge.source);
                }
            }
        }
        return res;
    }

    protected Set<String> getBCTIds(Multiset<NormalizedAttributeValuePair> docValues) {
        return docValues.stream()
                .filter(avp -> avp.getAttribute().getType() == AttributeType.INTERVENTION)
                .map(avp -> avp.getAttribute().getId())
                .collect(Collectors.toSet());
    }
}
