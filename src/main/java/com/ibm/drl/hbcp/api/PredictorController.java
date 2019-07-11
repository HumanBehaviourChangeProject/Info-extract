package com.ibm.drl.hbcp.api;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import javax.json.Json;
import javax.json.JsonArrayBuilder;

import com.ibm.drl.hbcp.predictor.graph.*;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.google.common.collect.Lists;

import com.ibm.drl.hbcp.predictor.api.AttributeInfo;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.predictor.api.RankedResults;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.predictor.queries.NodeQuery;
import com.ibm.drl.hbcp.predictor.queries.Query;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.util.AttributeIdLookup;
import com.ibm.drl.hbcp.util.Props;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;

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

    protected final Properties props;
    private final NodeVecs nodeVecs;
    final Set<String> validOutcomes = Stream.of("3909808", "3937167", "3937812", "3937812","4217650","3909809")
         .collect(Collectors.toCollection(HashSet::new));

    public PredictorController() throws Exception {
        this(Props.loadProperties("init.properties"));
    }

    public PredictorController(Properties props) throws Exception {
        this.props = props;
        //build graph
        RelationGraphBuilder rgb = new RelationGraphBuilder(props);
        AttribNodeRelations graph = rgb.getGraph();
        // get the vectors
        Node2Vec node2Vec = new Node2Vec(graph, props);
        nodeVecs = node2Vec.getNodeVectors();
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
    public String predictOutcome(
            @RequestParam(value="population", required = true) List<String> populationAttributes,
            @RequestParam(value="intervention", required = true) List<String> interventionAttributes,
            @RequestParam(value="expsetting", required = false) List<String> experimentalSettingAttributes,
            @RequestParam(value="top", required = false, defaultValue = "10") int topK
    ) {
        // parse everything as AttributeValueNode's
        List<List<String>> attributeSets = Lists.newArrayList(populationAttributes, interventionAttributes, experimentalSettingAttributes);
        List<List<AttributeValueNode>> nodeIdSets = new ArrayList<>();
        for (List<String> attributeSet : attributeSets) {
            if (attributeSet != null && !attributeSet.isEmpty())
                nodeIdSets.add(attributeSet.stream().map(AttributeValueNode::parse).collect(Collectors.toList()));
        }
        Query andQuery = new AndQuery(nodeIdSets.stream()
                .flatMap(nodeIdSet -> nodeIdSet.stream().map(NodeQuery::new))
                .collect(Collectors.toList()))
                .filter(result -> validOutcomes.contains(result.node.getAttribute().getId()));
        // get the results
        List<SearchResult> res = andQuery.searchTopK(nodeVecs, topK);
        return new RankedResults<>(res).toPrettyString();
    }

    /**
     * Returns information about attributes, given their ID.
     * For attribute of ID "4507433", you would get that it is called "Min Age", it is a Population attribute,
     * and it has Numeric-type values.
     * @param attributeIds list of IDs to get the attribute information of
     * @return list of attribute information objects for each of the requested IDs
     */
    @ApiOperation(value = "Returns information about attributes, given their ID")
    @RequestMapping(value = "/api/predict/attribute-info", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String attributeInfo(@RequestParam(value="id", required = true) List<String> attributeIds) {
        // get all the infos
        List<AttributeInfo> res = getAttributeInfos(attributeIds);
        // generate the JSON output
        return getJsonList(res);
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
        return attributeInfo(populationOptions());
    }

    private List<String> populationOptions() {
        // get all the infos
        //return Lists.newArrayList("4507433", "4507434","4507435","4507429","4507432","4507427","4507426","4507430");
        return Lists.newArrayList("4507433", "4507434","4507435","4507429");
    
    }


    /**
     * Returns all the gender options handled by the prediction system.
     * Use this when displaying options in a UI.
     */
    @ApiOperation(value = "Returns the gender options.",
            notes = "Returns all the gender options handled by the prediction system. " +
                    "Use this when displaying options in a UI.")
    @RequestMapping(value = "/api/predict/options/gender", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String genderOptionsEndpoint() {
        return attributeInfo(genderOptions());
    }

    private List<String> genderOptions() {
        // get all the infos
        //return Lists.newArrayList("4507433", "4507434","4507435","4507429","4507432","4507427","4507426","4507430");
        //return Lists.newArrayList("4507432","4507427","4507426","4507430");
        //only all male and all female
        return  Lists.newArrayList("4507426","4507430");
    
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
        return attributeInfo(expOptions());
    }

    private List<String> expOptions() {
        return Lists.newArrayList("4087187","4087189","4087190","4101520","4134962","4087191","4087182","4087182");
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

    private List<AttributeInfo> getAttributeInfos(List<String> attributeIds) {
        return attributeIds.stream()
                .map(this::getAttributeInfo)
                .filter(ai -> ai != null)
                .collect(Collectors.toList());
    }

    private AttributeInfo getAttributeInfo(String attributeId) {
        return AttributeIdLookup.getInstance().getAttributeInfo(attributeId);
    }

    private <T extends Jsonable> String getJsonList(List<T> options) {
        JsonArrayBuilder builder = Json.createArrayBuilder();
        for (T option : options) {
            builder.add(option.toJson());
        }
        return Jsonable.toPrettyString(builder.build());
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
        System.out.println(controller.predictOutcome(
                Lists.newArrayList(),
                Lists.newArrayList("I:3675717:1"),
                //Lists.newArrayList("I:3674270:1.0"),
                Lists.newArrayList(),
                10
        ));

        //SpringApplication.run(PredictorController.class, args);
        // example of page to visit in-browser
        // localhost:8080/predict/outcome?population=C:4507564:25.99&intervention=I:3675717:1.0
    }
}
