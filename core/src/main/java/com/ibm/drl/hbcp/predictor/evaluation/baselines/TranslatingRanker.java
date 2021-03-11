package com.ibm.drl.hbcp.predictor.evaluation.baselines;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.normalization.NormalizedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.normalization.Normalizers;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;

public class TranslatingRanker extends IdealTop1Ranker {

    private final Normalizers normalizers;

    private static final Logger log = LoggerFactory.getLogger(TranslatingRanker.class);

    public TranslatingRanker(Properties prop, AttribNodeRelations test, AttribNodeRelations train) throws IOException {
        super(prop, test, train);
        normalizers = new Normalizers(prop);

    }

    @Override
    public List<SearchResult> getResults(AndQuery query, NodeVecs vectors, int topK) {
        // get all the AVPs used in the query
        List<AttributeValueNode> constraints = getAllNodeConstraints(query).stream()
                .map(avn -> new ArmifiedAttributeValuePair(avn.getAttribute(), avn.getValue(), "", "", avn.getValue()))
                .map(normalizers::normalize)
                .map(navp -> new AttributeValueNode(navp))
                .collect(Collectors.toList());
        log.info("Constraints in the query: " + constraints.size());
        log.debug("{}", constraints);
        // build node queries out of all those
        List<NodeQuery> queries = constraints.stream().map(NodeQuery::new).collect(Collectors.toList());
        // execute the node queries, we get the closest constraints in the training graph
        List<AttributeValueNode> top1Results = queries.stream()
                .map(q -> q.searchTopK(vectors, 1))
                .filter(l -> !l.isEmpty())
                .map(list -> list.get(0))
                .map(SearchResult::getNode)
                .collect(Collectors.toList());
        log.info("Top 1 results in the actual training graph: " + top1Results.size());
        log.debug("{}", top1Results);
        // query for the outcomes
        List<SearchResult> resOutcomeValues = getResults(top1Results, Attribute::isOutcomeValue, vectors, topK);
        // query for the follow-ups
        List<SearchResult> resFollowUps = getResults(top1Results, Attribute::isTimepoint, vectors, topK);
        //
        List<SearchResult> res = withFollowUps(resOutcomeValues, resFollowUps);
        return res;
    }

    private List<SearchResult> getResults(List<AttributeValueNode> constraintTrainResults, Predicate<Attribute> filter, NodeVecs vecs, int topK) {
        // we're going to find their annotation here
        List<AttributeValueNode> queryOutcomeValues = constraintTrainResults.stream()
                .map(avn -> getTrainOutcomeValue(avn, filter))
                .filter(ov -> ov.isPresent())
                .map(ov -> ov.get())
                .map(ov -> new AttributeValueNode(ov))
                .collect(Collectors.toList());
        // we build an and query out of them
        AndQuery andQuery = new AndQuery(queryOutcomeValues.stream().map(NodeQuery::new).collect(Collectors.toList()),
                avn -> avn.getAttribute().isOutcomeValue(), topK);
        List<SearchResult> res = andQuery.searchTopK(vecs, topK);
        return res;
    }

    protected Optional<NormalizedAttributeValuePair> getTrainOutcomeValue(AttributeValueNode avn, Predicate<Attribute> filter) {
        for (String doc : annotations.byDoc().keySet()) {
            if (annotations.byDoc().get(doc).stream().anyMatch(aavp ->
                    aavp.getAttribute().equals(avn.getAttribute()) && aavp.getValue().equals(avn.getValue()))) {
                return annotations.byDoc().get(doc).stream()
                        .filter(avp -> filter.test(avp.getAttribute()))
                        .findFirst();
            }
        }
        return Optional.empty();
    }

    private List<SearchResult> withFollowUps(List<SearchResult> outcomeValues, List<SearchResult> followUps) {
        if (followUps.isEmpty()) {
            return outcomeValues;
        } else {
            SearchResult firstFollowUp = followUps.get(0);
            String timepoint = reformatFollowUp(firstFollowUp.getNode().getValue());
            return outcomeValues.stream()
                    .map(sr -> new SearchResult(sr.getNode(), sr.getScore(), timepoint))
                    .collect(Collectors.toList());
        }
    }

    public static String reformatFollowUp(String followUp) {
        if (followUp.matches("\\d+(\\.\\d+)?")) {
            double weeks = Double.parseDouble(followUp);
            return (int)Math.floor(weeks) + " week" + (weeks > 1.0 ? "s" : "");
        } else return followUp;
    }
}
