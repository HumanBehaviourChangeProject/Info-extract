package com.ibm.drl.hbcp.experiments.ie;

import com.ibm.drl.hbcp.extraction.extractors.IndexBasedAVPExtractor;
import com.ibm.drl.hbcp.extraction.extractors.InformationExtractor;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVWriter;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.text.DecimalFormat;
import java.util.List;
import java.util.Map;

public class CsvEvaluation {

    private static final String DEFAULT_PATH = "output/ie_50.csv";

    public static void evaluate(String[] args) throws IOException, ParseException {
        File path = new File(args.length > 0 ? args[0] : DEFAULT_PATH);
        path.getParentFile().mkdirs();
        try (CSVWriter w = new CSVWriter(new FileWriter(path))) {
            w.writeNext(new String[] { "Attribute", "Precision", "Recall", "F1" });
            try (InformationExtractor extractor = new InformationExtractor(Props.loadProperties("init.properties"))) {
                Map<IndexBasedAVPExtractor, List<RefComparison>> res = extractor.evaluate();
                for (Map.Entry<IndexBasedAVPExtractor, List<RefComparison>> extractorAndRes : res.entrySet()) {
                    IndexBasedAVPExtractor ext = extractorAndRes.getKey();
                    RefComparison firstComp = extractorAndRes.getValue().get(0);
                    w.writeNext(new String[] {
                            ext.toString(),
                            format(firstComp.getPrec1()),
                            format(firstComp.getRecall1()),
                            format(firstComp.getFscore1())});
                }
            }
        }
        System.out.println("Done.");
    }

    private static String format(float measure) {
        return new DecimalFormat("#.##").format(measure);
    }

    public static void main(String[] args) throws IOException, ParseException {
        evaluate(args);
    }
}
