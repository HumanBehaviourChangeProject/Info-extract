package com.ibm.drl.hbcp.predictor.evaluation.baselines;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValueCollection;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.evaluation.Evaluator;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.util.ParsingUtils;

import java.io.IOException;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class IdealTop1Ranker extends Evaluator {

    protected final String targetOutcomeValueAttributeId;
    private final List<AttributeValueNode> trainOutcomeValues;
    protected final String meanTrainOutcomeValue;
    protected final AttributeValueCollection<NormalizedAttributeValuePair> annotations;

    public IdealTop1Ranker(Properties prop, AttribNodeRelations test, AttribNodeRelations train) throws IOException {
        super(prop, test, train);
        targetOutcomeValueAttributeId = getTargetOutcomeValueAttributeId();
        trainOutcomeValues = train.getEdges().stream()
                .flatMap(edge -> Lists.newArrayList(edge.source, edge.target).stream())
                .filter(avp -> avp.getAttribute().getId().equals(targetOutcomeValueAttributeId))
                .collect(Collectors.toList());
        meanTrainOutcomeValue = getMeanValue(trainOutcomeValues);
        Normalizers normalizers = new Normalizers(prop);
        AttributeValueCollection<AnnotatedAttributeValuePair> raw = new JSONRefParser(prop).getAttributeValuePairs();
        annotations = new NormalizedAttributeValueCollection<>(normalizers, raw).distributeEmptyArm();
    }

    protected List<SearchResult> getResults(AndQuery query, NodeVecs vectors, int topK) {
        // get all the AVPs used in the query
        List<AttributeValueNode> constraints = getAllNodeConstraints(query);
        // find the document
        Multiset<NormalizedAttributeValuePair> annotationsInDoc = getAllValuesInDoc(constraints);
        // get the outcome value (this is the ground truth)
        Optional<NormalizedAttributeValuePair> outcomeValueTry = annotationsInDoc.stream()
                .filter(navp -> navp.getAttribute().getId().equals(targetOutcomeValueAttributeId))
                .findFirst();
        if (!outcomeValueTry.isPresent())
            throw new RuntimeException("The query matched a doc in the annotation but the outcome value couldn't be found.");
        NormalizedAttributeValuePair referenceOutcomeValue = outcomeValueTry.get();
        // This indeed yields an RMSE of 0.0, as expected (phew :D)
        // return Collections.singletonList(new SearchResult(new AttributeValueNode(referenceOutcomeValue), 1.0));
        // find the closest outcome value in the train set
        List<AttributeValueNode> closestInTrain = getClosestOutcomeValueInTrain(referenceOutcomeValue, annotationsInDoc);
        return closestInTrain.stream()
                .limit(topK)
                .map(avn -> new SearchResult(avn, 1.0))
                .collect(Collectors.toList());
    }

    protected List<AttributeValueNode> getClosestOutcomeValueInTrain(NormalizedAttributeValuePair referenceValue, Multiset<NormalizedAttributeValuePair> otherValuesInDoc) {
        double numericValue = ParsingUtils.parseFirstDouble(referenceValue.getValue());
        List<AttributeValueNode> closestNumericValuesInTrain = trainOutcomeValues.stream()
                .filter(AttributeValueNode::isNumeric)
                .sorted(Comparator.comparing(avp -> Math.abs(numericValue - avp.getNumericValue())))
                .collect(Collectors.toList());
        return closestNumericValuesInTrain.subList(0, 1);
    }

    protected List<AttributeValueNode> getAllNodeConstraints(AndQuery query) {
        // try to flatten the query first
        try {
            query = AndQuery.flatten(query);
        } catch (Exception e) {

        }
        // get all the NodeQueries in argument
        return query.queries.stream()
                .map(q -> (NodeQuery)q)
                .map(nq -> nq.node)
                .collect(Collectors.toList());
    }

    private Multiset<NormalizedAttributeValuePair> getAllValuesInDoc(List<AttributeValueNode> nodes) {
        for (String doc : annotations.byDoc().keySet()) {
            Multiset<NormalizedAttributeValuePair> valuesPerDoc = annotations.byDoc().get(doc);
            if (nodes.stream().allMatch(avn -> contains(valuesPerDoc, avn)))
                return valuesPerDoc;
        }
        throw new RuntimeException("This won't work. Couldn't match the constraints with the annotation: " + nodes);
    }

    protected boolean contains(Multiset<NormalizedAttributeValuePair> annotations, AttributeValueNode node) {
        return annotations.stream()
                .anyMatch(navp -> navp.getAttribute().equals(node.getAttribute()) && AttributeValueNode.normalizeValue(navp.getValue()).equals(node.getValue()));
    }

    private String getMeanValue(List<AttributeValueNode> trainOutcomeValues) {
        List<AttributeValueNode> numericOV = trainOutcomeValues.stream()
                .filter(avn -> avn.isNumeric())
                .filter(avn -> avn.getNumericValue() <= 100.0)
                .collect(Collectors.toList());
        double sum = numericOV.stream()
                .map(avn -> avn.getNumericValue())
                .reduce(0.0, Double::sum);
        return String.valueOf(sum / numericOV.size());
    }

    private String getTargetOutcomeValueAttributeId() {
        return prop.getProperty("prediction.testquery.outcomevalues");
    }
}
