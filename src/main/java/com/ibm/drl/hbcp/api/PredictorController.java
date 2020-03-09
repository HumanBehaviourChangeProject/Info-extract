package com.ibm.drl.hbcp.api;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.extraction.indexing.ExtractionIndexation;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.predictor.api.AttributeInfo;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.predictor.api.RankedResults;
import com.ibm.drl.hbcp.predictor.evaluation.Evaluator;
import com.ibm.drl.hbcp.predictor.evaluation.baselines.TranslatingRanker;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.Query;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.util.Props;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Data;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

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

    private final List<NodeVecConfig> allConfigs = Lists.newArrayList(
            new NodeVecConfig("gt", "false"),
            new NodeVecConfig("gt", "true"),
            new NodeVecConfig("ie", "false"),
            new NodeVecConfig("ie", "true")
    );
    private final Map<NodeVecConfig, NodeVecs> nodeVecsPerConfig;
    private final Map<NodeVecConfig, TranslatingRanker> translatingRankers;
    private final Set<String> validOutcomes = Sets.newHashSet("3909808", "3937167", "3937812", "3937812","4217650","3909809");

    public PredictorController() throws Exception {
        this(Props.loadProperties("init.properties"));
    }

    public PredictorController(Properties baseProps) throws Exception {
        nodeVecsPerConfig = new HashMap<>();
        translatingRankers = new HashMap<>();
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
        // at this point we're now ready to run queries
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
            @RequestParam(value="useannotations", required = false, defaultValue = "true") boolean useAnnotations,
            @ApiParam(value = "Whether to use the effect size as a parameter in the model.")
            @RequestParam(value="useeffectsize", required = false, defaultValue = "false") boolean useEffectSize
    ) {
        return predictOutcome(
                populationAttributes,
                interventionAttributes,
                experimentalSettingAttributes,
                topK, useAnnotations, useEffectSize).toPrettyString();
    }

    public RankedResults<SearchResult> predictOutcome(
            List<String> populationAttributes,
            List<String> interventionAttributes,
            List<String> experimentalSettingAttributes,
            int topK,
            boolean useAnnotations,
            boolean useEffectSize
    ) {
        // parse everything as AttributeValueNode's
        List<List<String>> attributeSets = Lists.newArrayList(populationAttributes, interventionAttributes, experimentalSettingAttributes);
        List<List<AttributeValueNode>> nodeIdSets = new ArrayList<>();
        for (List<String> attributeSet : attributeSets) {
            if (attributeSet != null && !attributeSet.isEmpty())
                nodeIdSets.add(attributeSet.stream().map(AttributeValueNode::parse).collect(Collectors.toList()));
        }
        AndQuery andQuery = AndQuery.forOutcomeValue(nodeIdSets.stream()
                .flatMap(nodeIdSet -> nodeIdSet.stream().map(NodeQuery::new))
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

    private boolean isValidOutcome(SearchResult result) {
        String attributeId = result.node.getAttribute().getId();
        return validOutcomes.contains(attributeId)
                || result.node.getAttribute().getType() == AttributeType.OUTCOME_VALUE;
                //|| result.node.getAttribute().getType() == AttributeType.OUTCOME;
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
        // get all the infos
        //return Lists.newArrayList("4507433", "4507434","4507435","4507429","4507432","4507427","4507426","4507430");
        //return Lists.newArrayList("4507433", "4507434","4507435","4507429");
        return Lists.newArrayList(
                "Minimum Age",
                "Maximum Age",
                "Mean Age"
        );
    }


    /**
     * Returns all the gender options handled by the prediction system.
     * Use this when displaying options in a UI.
     */
    @ApiOperation(value = "Returns the gender options.",
            notes = "Returns all the gender options handled by the prediction system. " +
                    "Mainly to be called by front-end applications.")
    @RequestMapping(value = "/api/predict/options/gender", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String genderOptionsEndpoint() {
        return Jsonable.getJsonList(getAttributeInfosFromName(genderOptions()));
    }

    private List<String> genderOptions() {
        // get all the infos
        //return Lists.newArrayList("4507433", "4507434","4507435","4507429","4507432","4507427","4507426","4507430");
        //return Lists.newArrayList("4507432","4507427","4507426","4507430");
        //only all male and all female
        //return  Lists.newArrayList("4507426","4507430");
        return Lists.newArrayList("All Male", "All Female");
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
        return Lists.newArrayList("3673271", "3673272", "3675717","3673281","3675716","3673293","3673297","3673304","3673778","3675723","3674256","3674260","3675610","3675679","3675686","3675692");
    }

    /**
     * Returns all the experimental settings attributes handled by the prediction system.
     * Others will not yield relevant results.
     */
    @ApiOperation(value = "Returns the experimental settings attributes.",
            notes = "Returns all the experimental settings attributes handled by the prediction system. " +
                    "Others will not yield relevant results.")
    @RequestMapping(value = "/api/predict/options/expsettings", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String expOptionsEndpoint() {
        return Jsonable.getJsonList(getAttributeInfosFromName(expOptions()));
    }

    private List<String> expOptions() {
        return Lists.newArrayList("Longest follow up");
        //return Lists.newArrayList("4087187","4087189","4087190","4101520","4134962","4087191","4087182","4087182");
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
        List<String> options = new ArrayList<>(populationOptions());
        options.addAll(interventionOptions());
        return attributeInfo(options);
    }

    private List<AttributeInfo> getAttributeInfosFromName(List<String> attributeNames) {
        return attributeNames.stream()
                .map(Attributes.get()::getFromName)
                .filter(attr -> attr != null)
                .map(AttributeInfo::new)
                .collect(Collectors.toList());
    }

    private List<AttributeInfo> getAttributeInfos(List<String> attributeIds) {
        return attributeIds.stream()
                .map(Attributes.get()::getFromId)
                .filter(attr -> attr != null)
                .map(AttributeInfo::new)
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
        List<Boolean> allBooleans = Lists.newArrayList(false);
        for (boolean useAnnotations : allBooleans) {
            for (boolean useEffectSize : allBooleans) {
                System.out.println(controller.predictOutcome(
                        Lists.newArrayList("C:5579089:18"),
                        Lists.newArrayList("I:3673272:1"), // I:3673271:1        I:3675717:1         I:3673272:1
                        Lists.newArrayList(),
                        10,
                        true, useEffectSize
                ).toPrettyString());
            }
        }
        //SpringApplication.run(PredictorController.class, args);
        // example of page to visit in-browser
        // localhost:8080/predict/outcome?population=C:4507564:25.99&intervention=I:3675717:1.0
    }
}
