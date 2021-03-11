package com.ibm.drl.hbcp.predictor.evaluation.parameters;

import com.ibm.drl.hbcp.predictor.evaluation.metrics.RMSE;
import com.ibm.drl.hbcp.predictor.queries.AndQuery;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;

public class GridSearch {

    private final Properties props;
    private final List<PredictionParameters> allParameterCombos;
    private static final String rmseMetricName = new RMSE().toString();

    public GridSearch(Properties props, List<PredictionParameters> allParameterCombos) {
        this.props = props;
        this.allParameterCombos = allParameterCombos;
    }

    public Pair<PredictionParameters, Double> bestParametersAndRMSE() throws IOException {
        TreeSet<Pair<PredictionParameters, Double>> res = createScoredPairSet();
        for (PredictionParameters params : allParameterCombos) {
            // override the functions of AndQuery here
            AndQuery.subResultScoring = params.getSubResultScoring().get(0);
            AndQuery.resultPlusSubsScoring = params.getResultPlusSubsScoring().get(0);
            // compute RMSE (this is long and prints a lot of stuff)
            // TODO replace EndToEnd with NearestNeighborQueryFlow
            Map<String, Double> metricResults = null; //EndToEndEvaluationPipeline.evaluate(props,true,
//                    params.getWindowSize().get(0),
//                    params.getP().get(0), 1.0 - params.getP().get(0),
//                    params.getTopK().get(0),
//                    params.getFlattenQuery().get(0), false);
            double rmse = metricResults.get(rmseMetricName);
            System.out.println("\t=== Finished evaluation for " + params);
            System.out.println("\t=== RMSE = " + rmse);
            res.add(Pair.of(params, rmse));
            System.out.println("\t=== Best so far was: " + res.first().getValue() + " for " + res.first().getKey());
        }
        // get the best
        return res.first();
    }

    private <E> TreeSet<Pair<E, Double>> createScoredPairSet() {
        return new TreeSet<>(Comparator.comparingDouble(Pair::getValue));
    }

    public static void main(String[] args) throws IOException {
        List<PredictionParameters> params = PredictionParameters.ALL.getAllCombinations();
        Collections.shuffle(params);
        // It used to be "ov.prediction.properties", switched to "init.properties"
        GridSearch search = new GridSearch(Props.loadProperties("init.properties"), params);
        System.out.println("[!] Old code running, please double check the loaded property file.");
        Pair<PredictionParameters, Double> best = search.bestParametersAndRMSE();
        System.out.println("The best RMSE was obtained for: ");
        System.out.println("Parameters:");
        System.out.println(best.getKey());
        System.out.println("RMSE = " + best.getValue());
    }
}
