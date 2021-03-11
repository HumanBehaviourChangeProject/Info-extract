package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class OverallEvaluation {

    public static void main(String[] args) throws Exception {
        String[] evalNames = { "F1_final", "alpha_final" };
        Metric[] metrics = { new F1AllAttributes(), new AvcKrippendorffAlphaAgreement() };
        AttributeValueCollection<AnnotatedAttributeValuePair> goldAvc = new JSONRefParser(Props.getDefaultPropFilename()).getAttributeValuePairs();
        AttributeValueCollection<? extends ArmifiedAttributeValuePair> flairAvc = InterAnnotatorAgreementEvaluation.loadFlairResults(new File("../data/precomputed/flairExtraction"));
        for (int i = 0; i < evalNames.length; i++) {
            Map<Attribute, List<Double>> evaluate = metrics[i].evaluate(flairAvc, goldAvc);
            try (CSVWriter w = new CSVWriter(new FileWriter("output/" + evalNames[i] + ".csv"))) {
                List<String> header = new ArrayList<>();
                header.add("attribute");
                header.add(evalNames[i]);
                String[] rowArray = new String[header.size()];
                w.writeNext(header.toArray(rowArray));
                for (Map.Entry<Attribute, List<Double>> attribute : evaluate.entrySet()) {
                    List<String> row = new ArrayList<>();
                    row.add(attribute.getKey().getName());
                    List<Double> metricsValues = attribute.getValue();
                    if (metricsValues != null) {
                        int metricIndex = i == 0 ? 2 : 0;
                        row.add(String.valueOf(metricsValues.get(metricIndex)));
                    } else {
                        row.add("");
                    }
                    w.writeNext(row.toArray(rowArray));
                }
            }

        }
    }
}
