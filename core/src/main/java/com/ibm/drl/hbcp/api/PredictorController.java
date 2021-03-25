package com.ibm.drl.hbcp.api;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.JsonArrayBuilder;
import javax.json.JsonObjectBuilder;
import javax.ws.rs.core.MediaType;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.predictor.api.*;
import com.ibm.drl.hbcp.util.Environment;
import io.swagger.annotations.Example;
import io.swagger.annotations.ExampleProperty;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.extraction.indexing.ExtractionIndexation;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.AttributeValues;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.evaluation.Evaluator;
import com.ibm.drl.hbcp.predictor.evaluation.baselines.TranslatingRanker;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.predictor.similarity.QueryArmSimilarity;
import com.ibm.drl.hbcp.util.Props;

import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;

/**
 * The REST controller for the Prediction API.
 *
 * Exposes endpoints allowing to get results for queries of the type: "Given this population characteristics and
 * this intervention (behavior change techniques), what would be the expected outcome?"
 *
 * @author marting
 */
@RestController
//@EnableAutoConfiguration(exclude = {org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration.class})
public class PredictorController {

    private final JSONRefParser jsonRefParser;
    private final List<NodeVecConfig> allConfigs = Lists.newArrayList(
            new NodeVecConfig("gt", "false"),
            new NodeVecConfig("gt", "true"),
            new NodeVecConfig("ie", "false"),
            new NodeVecConfig("ie", "true")
    );
    private final Map<NodeVecConfig, NodeVecs> nodeVecsPerConfig;
    private final Map<NodeVecConfig, TranslatingRanker> translatingRankers;
    private final MockArmSimilarityResultGenerator relevantDocsGenerator;
    private final QueryArmSimilarity armSimilarity;
    static org.slf4j.Logger logger = LoggerFactory.getLogger(PredictorController.class);

    public PredictorController() throws Exception {
        this(Props.loadProperties("init.properties"));
    }

    public PredictorController(Properties baseProps) throws Exception {
        jsonRefParser = new JSONRefParser(baseProps);
        nodeVecsPerConfig = new HashMap<>();
        translatingRankers = new HashMap<>();
        // completely disable indexing of values
        if (!Environment.isPredictionApiOnly()) { // outside of a docker-compose setting we fall back on a baseline
            // if not done, index the papers
            PaperIndexer.ensure(baseProps);
            // if not done, run the extraction and index the values
            ExtractionIndexation.ensure(baseProps);
            for (NodeVecConfig config : allConfigs) {
                Properties props = new Properties(baseProps);
                props.setProperty("prediction.source", config.predictionSource);
                props.setProperty("prediction.effects.priors", config.predictionEffectsPriors);
                //build graph
                RelationGraphBuilder rgb = new RelationGraphBuilder(props);
                AttribNodeRelations graph = rgb.getGraph(true);
                // get the vectors
                Node2Vec node2Vec = new Node2Vec(graph, props);
                NodeVecs nodeVecs = node2Vec.getNodeVectors();
                // build the translating ranker
                TranslatingRanker ranker = new TranslatingRanker(props, graph, graph);
                nodeVecsPerConfig.put(config, nodeVecs);
                translatingRankers.put(config, ranker);
            }
        }
        relevantDocsGenerator = new MockArmSimilarityResultGenerator();
        armSimilarity = new QueryArmSimilarity(jsonRefParser.getAttributeValuePairs(), baseProps);
        // at this point we're now ready to run queries
        logger.info("Prediction is ready to handle queries. If started with Spring Boot, Swagger UI should be available on the default port (for a local deployment http://localhost:8080/swagger-ui.html)");
    }

    /**
     * Returns the top K outcome results to a query asking for a combination of population characteristics, intervention
     * attributes, experimental settings.
     * The elements of the query are all required (which means that the results will be empty if one of them cannot
     * be found in the training data).
     * The results are scored according to their relevance to the query.
     *
     * @param populationAttributes list of population attribute nodes, to specify for example that we want a Min Age of 15
     * @param interventionAttributes the individual BCTs applied on the population
     * @param experimentalSettingAttributes experimental setting attributes
     * @param topK the number of results this call should return
     * @return list of scored results
     */
    @ApiOperation(value = "Predicts the outcome of an intervention.",
            notes = "Returns the top K outcome results to a query asking for a combination of population characteristics, " +
                    "intervention attributes, experimental settings.")
    @RequestMapping(value = "/api/predict/outcome", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String predictOutcomeEndpoint(
            //example: http://localhost:3001/api/predict/outcome?population=C:5579089:18&intervention=I:3673272:1&useannotations=false&useeffectsize=false
            @ApiParam(value = "One or multiple population attributes, in the format C:[id]:[value]", required = true, defaultValue = "C:5579089:18")
            @RequestParam(value="population") List<String> populationAttributes,
            @ApiParam(value = "One or multiple intervention attributes (Behavior Change Techniques) in the format I:[id]:1", required = true, defaultValue = "I:3673272:1")
            @RequestParam(value="intervention") List<String> interventionAttributes,
            @ApiParam(value = "Optional experimental setting attribute")
            @RequestParam(value="expsetting", required = false) List<String> experimentalSettingAttributes,
            @ApiParam(value = "Top K predictions to consider, one of the hyperparameters of the algorithm.")
            @RequestParam(value="top", required = false, defaultValue = "10") int topK,
            @ApiParam(value = "Whether to use the learned model from the annotations or the automatic extractions (false for the latter).")
            @RequestParam(value="useannotations", required = false, defaultValue = "false") boolean useAnnotations,
            @ApiParam(value = "Whether to use the effect size as a parameter in the model.")
            @RequestParam(value="useeffectsize", required = false, defaultValue = "false") boolean useEffectSize,
            @ApiParam(value = "Whether to use the neural prediction model.")
            @RequestParam(value="useneuralprediction", required = false, defaultValue = "false") boolean useNeuralPrediction
    ) {
        return predictOutcome(
                populationAttributes,
                interventionAttributes,
                experimentalSettingAttributes,
                topK, useAnnotations, useEffectSize,
                useNeuralPrediction).toPrettyString();
    }

    /**
     * Returns the top K interventions by predicted outcome values, to a query asking for a combination of population characteristics,
     * possible BC techniques, experimental settings.
     * The results are scored with a confidence score. */
    @ApiOperation(value = "Recommends best interventions for user scenarios.",
            notes = "Returns the top K interventions by predicted outcome values, to a query asking for a combination of population characteristics, " +
                    "possible BC techniques, experimental settings." +
                    "As an example of 'query', you can use: " +
                    "[{\"id\":\"3673271\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673272\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673273\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673274\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673275\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3675715\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673282\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673283\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673284\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3673285\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3675717\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3675718\",\"type\":\"boolean\",\"value\":true},{\"id\":\"3675719\",\"type\":\"boolean\",\"value\":true},{\"id\":\"5579088\",\"type\":\"numeric\",\"value\":30},{\"id\":\"5579096\",\"type\":\"numeric\",\"value\":50}]")
    @RequestMapping(value = "/api/predict/recommend", method = RequestMethod.POST, consumes = "application/json", produces="application/json;charset=utf-8")
    public String recommend(
            @ApiParam(value = "Maximum number of recommended scenarios (the more, the longer the API call).")
            @RequestParam(value="max", required = false, defaultValue = "10") int max,
            @ApiParam(value = "A set of attribute-value pairs representing Behavior Change intervention scenarios serving as input")
            @RequestBody AttributeValue[] query) throws IOException {
        // turn into good old AVPs
        List<AttributeValuePair> avps = Arrays.stream(query).map(AttributeValue::toAvp).collect(Collectors.toList());
        // produce the candidate interventions to send to the prediction model
        List<List<String>> recommendedInterventionsInfo = RecommendedInterventions.get().getRecommendedInterventions(avps, max).stream()
                .map(ArrayList::new)
                .collect(Collectors.toList());
        List<List<AttributeValuePair>> queries = RecommendedInterventions.get().getRecommendedScenarios(avps, max);
        // run the query
        RankedResults<SearchResult> predictions = runBatchQueries(queries, 1, true, false, true);
        // build the recommendation results
        List<RecommendedIntervention> res = new ArrayList<>();
        for (int i = 0; i < predictions.getResults().size(); i++) {
            List<String> intervention = recommendedInterventionsInfo.get(i);
            SearchResult prediction = predictions.getResults().get(i);
            RecommendedIntervention reco = new RecommendedIntervention(intervention,
                    prediction.getNode().getNumericValue(), prediction.getScore());
            res.add(reco);
        }
        // sort by predicted outcome value
        res.sort(Comparator.comparing((RecommendedIntervention reco) -> -reco.getPredictedValue()));
        // return JSON response
        return Jsonable.toPrettyString(Json.createObjectBuilder()
                .add("results", Jsonable.getJsonArrayFromCollection(res))
                .build());
    }

    public RankedResults<SearchResult> predictOutcome(
            List<String> populationAttributes,
            List<String> interventionAttributes,
            List<String> experimentalSettingAttributes,
            int topK,
            boolean useAnnotations,
            boolean useEffectSize,
            boolean useNeuralPrediction
    ) {
        // parse everything as AttributeValueNode's
        List<List<String>> attributeSets = Lists.newArrayList(populationAttributes, interventionAttributes, experimentalSettingAttributes);
        List<List<AttributeValueNode>> nodeIdSets = new ArrayList<>();
        for (List<String> attributeSet : attributeSets) {
            if (attributeSet != null && !attributeSet.isEmpty())
                nodeIdSets.add(attributeSet.stream().map(AttributeValueNode::parse).collect(Collectors.toList()));
        }
        List<AttributeValueNode> flattenedNodeIds = nodeIdSets.stream()
            .collect(ArrayList::new, ArrayList::addAll, ArrayList::addAll);
        // execute the query
        return runQuery(flattenedNodeIds, topK, useAnnotations, useEffectSize, useNeuralPrediction);
    }

    private RankedResults<SearchResult> runQuery(List<? extends AttributeValuePair> query,
                                                 int topK, boolean useAnnotations, boolean useEffectSize,
                                                 boolean usePredictionApi) {
        if (usePredictionApi || Environment.isPredictionApiOnly()) {
            // create a new predictor service (very fast)
            PredictionServiceConnector connector = PredictionServiceConnector.createForLocalService();
            // request a prediction
            Optional<PredictionServiceConnector.PredictionWithConfidence> predictionResponse = connector.requestPrediction(query);
            List<SearchResult> res = new ArrayList<>();
            if (predictionResponse.isPresent()) {
                SearchResult sr = new SearchResult(new AttributeValueNode(
                            new AttributeValuePair(Attributes.get().getFromName("Outcome value"), String.valueOf(predictionResponse.get().getValue()))),
                        predictionResponse.get().getConf());
                res.add(sr);
            }
            return new RankedResults<>(res);
        } else {
            // formulate the query
            AndQuery andQuery = AndQuery.forOutcomeValue(query.stream()
                .map(avp -> new NodeQuery(new AttributeValueNode(avp)))
                .collect(Collectors.toList()));
            // select the right graph/vectors
            NodeVecConfig config = NodeVecConfig.create(useAnnotations, useEffectSize);
            NodeVecs nodeVecs = nodeVecsPerConfig.get(config);
            // get the results
            //List<SearchResult> res = andQuery.searchTopK(nodeVecs, topK);
            TranslatingRanker ranker = translatingRankers.get(config);
            List<SearchResult> res = ranker.getResults(andQuery, nodeVecs, topK);
            // collapse to weighted average (this reduces the list to a singleton)
            List<SearchResult> collapsed = Evaluator.collapseToWeightedAverage(res);
            return new RankedResults<>(collapsed);
        }
    }

    private RankedResults<SearchResult> runBatchQueries(List<List<AttributeValuePair>> queries,
                                                 int topK, boolean useAnnotations, boolean useEffectSize,
                                                 boolean usePredictionApi) {
        if (usePredictionApi || Environment.isPredictionApiOnly()) {
            // create a new predictor service (very fast)
            PredictionServiceConnector connector = PredictionServiceConnector.createForLocalService();
            // request a prediction
            List<PredictionServiceConnector.PredictionWithConfidence> predictionResponses = connector.requestPredictionBatch(queries);
            List<SearchResult> res = predictionResponses.stream()
                    .map(predictionResponse -> new SearchResult(new AttributeValueNode(
                            new AttributeValuePair(Attributes.get().getFromName("Outcome value"), String.valueOf(predictionResponse.getValue()))),
                            predictionResponse.getConf()))
                    .collect(Collectors.toList());
            return new RankedResults<>(res);
        } else {
            return new RankedResults<>(new ArrayList<>());
        }
    }



    /**
     * Returns information about attributes, given their ID.
     * For attribute of ID "4507433", you would get that it is called "Min Age", it is a Population attribute,
     * and it has Numeric-type values.
     * @param attributeIds list of IDs to get the attribute information of
     * @return list of attribute information objects for each of the requested IDs
     */
    @ApiOperation(value = "Returns information about attributes, given their ID",
            notes = "Mainly to be called by front-end applications.")
    @RequestMapping(value = "/api/predict/attribute-info", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String attributeInfo(@RequestParam(value="id", required = true) List<String> attributeIds) {
        // get all the infos
        List<AttributeInfo> res = getAttributeInfos(attributeIds);
        // generate the JSON output
        return Jsonable.getJsonList(res);
    }

    /**
     * Returns all the population attributes handled by the prediction system.
     * Others will not yield relevant results.
     */
    @ApiOperation(value = "Returns the population attributes.",
            notes = "Returns all the population attributes handled by the prediction system. " +
                    "Others will not yield relevant results.")
    @RequestMapping(value = "/api/predict/options/population", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String populationOptionsEndpoint() {
        return Jsonable.getJsonList(getAttributeInfosFromName(populationOptions()));
    }

    private List<String> populationOptions() {
        return Attributes.get().stream()
                .filter(a -> a.getType() == AttributeType.POPULATION)
                .map(Attribute::getName)
                .collect(Collectors.toList());
    }

    /**
     * Returns all the intervention attributes handled by the prediction system.
     * Others will not yield relevant results.
     */
    @ApiOperation(value = "Returns the intervention attributes.",
            notes = "Returns all the intervention attributes handled by the prediction system. " +
                    "Others will not yield relevant results.")
    @RequestMapping(value = "/api/predict/options/intervention", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String interventionOptionsEndpoint() {
        return attributeInfo(interventionOptions());
    }

    private List<String> interventionOptions() {
        return Attributes.get().stream()
                .filter(a -> a.getType() == AttributeType.INTERVENTION)
                .map(Attribute::getId)
                .collect(Collectors.toList());
    }

    /**
     * Returns all the input attributes handled by the prediction system.
     * This combines population, intervention and experimental settings.
     * Others will not yield relevant results.
     */
    @ApiOperation(value = "Returns all the input attributes.",
            notes = "Returns all the input attributes handled by the prediction system. " +
                    "This combines population, intervention and experimental settings. " +
                    "Others will not yield relevant results.")
    @RequestMapping(value = "/api/predict/options/all", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String allInputOptions() {
        List<String> options = Attributes.get().stream()
                .map(Attribute::getId)
                .collect(Collectors.toList());
        return attributeInfo(options);
    }

    /** Returns all the input attributes handled by the prediction system, clustered by type. */
    @ApiOperation(value = "Returns a mock list of relevant documents, compared with an imaginary query.")
    @RequestMapping(value = "/api/predict/mockrelevantdocs", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String mockRelevantDocs() {
        List<ArmSimilarityResult> top10 = relevantDocsGenerator.generateTopK(10);
        return Jsonable.toPrettyString(Json.createObjectBuilder()
                .add("results", Jsonable.getJsonArrayFromCollection(top10))
                .build());
    }

    /** Returns all the input attributes handled by the prediction system, clustered by type. */
    @ApiOperation(value = "Returns prediction insights.",
            notes = "Returns prediction insights: a comparison of a predicted outcome value and relevant scientific articles " +
                    "with their reported outcome value. As an example of query, you can use: " +
                    "[{\"id\":\"5579096\",\"type\":\"numeric\",\"value\":50},{\"id\":\"3673271\",\"type\":\"boolean\",\"value\":true}]")
    @RequestMapping(value = "/api/predict/insights", method = RequestMethod.POST, consumes = "application/json", produces="application/json;charset=utf-8")
    public String predictionInsights(
            @ApiParam(value = "Whether to use the neural prediction model.")
            @RequestParam(value="useneuralprediction", required = false, defaultValue = "false") boolean useNeuralPrediction,
            @ApiParam(value = "A set of attribute-value pairs representing a Behavior Change intervention scenario serving as input",
            examples = @Example(value = @ExampleProperty(
                    mediaType = MediaType.APPLICATION_JSON,
                    value = "[{\"id\":\"5579096\",\"type\":\"numeric\",\"value\":50},{\"id\":\"3673271\",\"type\":\"boolean\",\"value\":true}]")))
            @RequestBody AttributeValue[] query) throws IOException {
        // turn into good old AVPs
        List<AttributeValuePair> avps = Arrays.stream(query).map(AttributeValue::toAvp).collect(Collectors.toList());
        JsonObjectBuilder res = Json.createObjectBuilder();
        // run the query
        RankedResults<SearchResult> predictions = runQuery(avps, 1, true, false, useNeuralPrediction);
        if (!predictions.getResults().isEmpty()) {
            SearchResult firstResult = predictions.getResults().get(0);
            double firstOutcomeValue = firstResult.node.getNumericValue();
            res.add("predictedOutcomeValue", firstOutcomeValue);
            res.add("followUp", firstResult.getFollowUp());
        }
        // retrieve arm similarities
        List<ArmSimilarityResult> armSimilarityResults = armSimilarity.querySimilarityByArm(avps, jsonRefParser);
        res.add("results", Jsonable.getJsonArrayFromCollection(armSimilarityResults.stream().limit(10).collect(Collectors.toList())));
        return Jsonable.toPrettyString(res.build());
    }

    /** Returns all the input attributes handled by the prediction system, clustered by type. */
    @ApiOperation(value = "Returns all the input attributes, clustered by type.",
            notes = "Returns all the input attributes handled by the prediction system, clustered by type. " +
                    "This combines population and intervention attributes. " +
                    "Others will not yield relevant results.")
    @RequestMapping(value = "/api/predict/options/allbytype", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String allAttributes() {
        List<AttributeType> supportedTypes = Lists.newArrayList(AttributeType.POPULATION, AttributeType.INTERVENTION, AttributeType.OUTCOME);
        JsonArrayBuilder res = Json.createArrayBuilder();
        for (AttributeType type : supportedTypes) {
            List<AttributeInfo> attributeInfos = Attributes.get().getAttributeSet().stream()
                    .filter(attribute -> attribute.getType() == type)
                    .map(attribute -> AttributeInfo.fromAttribute(attribute, RelevantPredictionQueryAttributes.get().contains(attribute)))
                    // remove the text attributes for now
                    .filter(attributeInfo -> attributeInfo.getValueType().getValueType() != AttributeValues.ValueType.TEXT)
                    .sorted(RelevantPredictionQueryAttributes.AttributeInfoComparatorForPredictionDemo)
                    .collect(Collectors.toList());
            res.add(Json.createObjectBuilder()
                    .add("name", type.getName())
                    .add("attributes", Jsonable.getJsonArrayFromCollection(attributeInfos))
                    .build());
        }
        return Jsonable.toPrettyString(Json.createObjectBuilder()
                .add("categories", res.build())
                .build());
    }

    private List<AttributeInfo> getAttributeInfosFromName(List<String> attributeNames) {
        return attributeNames.stream()
                .map(Attributes.get()::getFromName)
                .filter(attr -> attr != null)
                .map(AttributeInfo::fromAttribute)
                .collect(Collectors.toList());
    }

    private List<AttributeInfo> getAttributeInfos(List<String> attributeIds) {
        return attributeIds.stream()
                .map(Attributes.get()::getFromId)
                .filter(attr -> attr != null)
                .map(AttributeInfo::fromAttribute)
                .collect(Collectors.toList());
    }

    @Data
    public static class NodeVecConfig {
        private final String predictionSource; // gt or ie
        private final String predictionEffectsPriors; // true or false

        public static NodeVecConfig create(boolean useAnnotations, boolean useEffectSize) {
            return new NodeVecConfig(useAnnotations ? "gt" : "ie", String.valueOf(useEffectSize));
        }
    }

    /*
    @RequestMapping(value = "/", method = RequestMethod.GET)
    @ResponseStatus(value = HttpStatus.NOT_FOUND)
    protected String helloWorld() {
        return "Use the /predict/outcome/ endpoint.";
    }
    */

    public static void main(String[] args) throws Exception {
        //marting: for some reason there is a difference between running this in IntelliJ,
        // or running it with "mvn spring-boot:run"
        PredictorController controller = new PredictorController();
        List<Boolean> allBooleans = Lists.newArrayList(true, false);
        for (boolean useAnnotations : allBooleans) {
            for (boolean useEffectSize : allBooleans) {
                System.out.println(controller.predictOutcome(
                        Lists.newArrayList("C:5579089:18"),
                        Lists.newArrayList("I:3673272:1"), // I:3673271:1        I:3675717:1         I:3673272:1
                        Lists.newArrayList(),
                        10,
                        useAnnotations, useEffectSize, false
                ));
            }
        }
        //SpringApplication.run(PredictorController.class, args);
        // example of page to visit in-browser
        // localhost:8080/predict/outcome?population=C:4507564:25.99&intervention=I:3675717:1.0
    }
}
