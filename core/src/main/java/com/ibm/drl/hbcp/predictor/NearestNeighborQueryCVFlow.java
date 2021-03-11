package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.data.CVSplitter;
import com.ibm.drl.hbcp.predictor.data.DataSplitter;
import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import com.ibm.drl.hbcp.util.Props;

import java.io.IOException;
import java.util.Properties;

import java.io.FileReader;
import java.util.ArrayList;
import java.util.List;

public class NearestNeighborQueryCVFlow extends NearestNeighborQueryFlow {

    protected NearestNeighborQueryCVFlow(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                       AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                       DataSplitter splitter,
                                       Properties props) throws IOException {
        super(values, annotations, splitter, props);
    }

    public static void main(String[] args) throws IOException {
        Properties extraProps = new Properties();
        if (args.length > 0) {
            extraProps.load(new FileReader(args[0])); // overriding arguments
        }
        Properties props = Props.overrideProps(Props.loadProperties(), extraProps);
        JSONRefParser refParser = new JSONRefParser(props);
        
        
        final int NUM_FOLDS = 5;
        CVSplitter splitter = new CVSplitter(NUM_FOLDS);
        List<PredictionTuple> predictions = new ArrayList<>();
        
        for (int i=0; i < NUM_FOLDS; i++) {
            NearestNeighborQueryCVFlow flow = new NearestNeighborQueryCVFlow(
                    refParser.getAttributeValuePairs(),
                    refParser.getAttributeValuePairs(),
                    splitter,
                    props
            );
            predictions.addAll(flow.run());  // collect the results
        }
        printAndSaveResults(predictions); // evaluate after 5 folds (5 runs)               
    }
}
