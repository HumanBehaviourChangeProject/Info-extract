package com.ibm.drl.hbcp.predictor.evaluation.baselines;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.stream.Collectors;

public class TranslatingRanker extends IdealTop1Ranker {

    private final Normalizers normalizers;

    public TranslatingRanker(Properties prop, AttribNodeRelations test, AttribNodeRelations train) throws IOException {
        super(prop, test, train);
        normalizers = new Normalizers(prop);
        System.out.println("Train OV:");
        System.out.println(train.getAllOutcomeValues(targetOutcomeValueAttributeId));
        System.out.println("Test OV");
        System.out.println(test.getAllOutcomeValues(targetOutcomeValueAttributeId));

    }

    @Override
    public List<SearchResult> getResults(AndQuery query, NodeVecs vectors, int topK) {
        // get all the AVPs used in the query
        List<AttributeValueNode> constraints = getAllNodeConstraints(query).stream()
                .map(avn -> new ArmifiedAttributeValuePair(avn.getAttribute(), avn.getValue(), "", "", avn.getValue()))
                .map(normalizers::normalize)
                .map(navp -> new AttributeValueNode(navp))
                .collect(Collectors.toList());
        System.out.println("Constraints in the query: " + constraints.size());
        System.out.println(constraints);
        // build node queries out of all those
        List<NodeQuery> queries = constraints.stream().map(NodeQuery::new).collect(Collectors.toList());
        // execute the node queries, we get the closest constraints in the training graph
        List<AttributeValueNode> top1Results = queries.stream()
                .map(q -> q.searchTopK(vectors, 1))
                .filter(l -> !l.isEmpty())
                .map(list -> list.get(0))
                .map(SearchResult::getNode)
                .collect(Collectors.toList());
        System.out.println("Top 1 results in the actual training graph: " + top1Results.size());
        System.out.println(top1Results);
        // we're going to find their annotation here
        List<AttributeValueNode> queryOutcomeValues = top1Results.stream()
                .map(avn -> getTrainOutcomeValue(avn))
                .filter(ov -> ov.isPresent())
                .map(ov -> ov.get())
                .map(ov -> new AttributeValueNode(ov))
                .collect(Collectors.toList());
        System.out.println("Outcome values: " + queryOutcomeValues.size());
        System.out.println(queryOutcomeValues);
        // we build an and query out of them
        AndQuery andQuery = new AndQuery(queryOutcomeValues.stream().map(NodeQuery::new).collect(Collectors.toList()),
                avn -> avn.getAttribute().isOutcomeValue(), topK);
        return andQuery.searchTopK(vectors, topK);
    }

    protected Optional<NormalizedAttributeValuePair> getTrainOutcomeValue(AttributeValueNode avn) {
        for (String doc : annotations.byDoc().keySet()) {
            if (annotations.byDoc().get(doc).stream().anyMatch(aavp ->
                    aavp.getAttribute().equals(avn.getAttribute()) && aavp.getValue().equals(avn.getValue()))) {
                return annotations.byDoc().get(doc).stream()
                        .filter(avp -> avp.getAttribute().isOutcomeValue())
                        .findFirst();
            }
        }
        return Optional.empty();
    }
}
