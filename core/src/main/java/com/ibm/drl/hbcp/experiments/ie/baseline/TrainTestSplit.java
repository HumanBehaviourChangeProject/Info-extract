package com.ibm.drl.hbcp.experiments.ie.baseline;

import com.ibm.drl.hbcp.experiments.flair.GenerateTrainingData_NameAsCategory;
import com.ibm.drl.hbcp.extraction.extractors.IndexBasedAVPExtractor;
import com.ibm.drl.hbcp.extraction.extractors.InformationExtractor;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 *  This is a kind of ugly approach for running the baseline 'unsupervised' approach to compare numbers to Flair.
 *  It uses the training/test splits from {@link GenerateTrainingData_NameAsCategory}.
 *  This runs the unsupervised extractors on the training data to pick the window size (10-50, steps of 10) and then
 *  runs the extractors on the test data and collects the results for each entity by the best window size from the
 *  training data.
 *
 *  This also dumps all data in tsv and tex files.
 *
 *
 * @author charlesj
 *
 */
public class TrainTestSplit {

    public static void main(String[] args) throws Exception {
        // verbose output shows tp/fp/fn/tn counts
        boolean verbose = true;
        boolean training = true;

        GenerateTrainingData_NameAsCategory genTrainTestData = new GenerateTrainingData_NameAsCategory();
        Map<String, List<String>> trainingTestingFiles = genTrainTestData.generateTrainingTestingFiles();

        Map<String, Pair<Double, String>> bestTrainResults = new HashMap<>();
        Map<String, Double> finalResults = new HashMap<>();

        for (int trainTestLoop = 0; trainTestLoop < 2; trainTestLoop++) {  // run once as training, once for test
            for (int i = 1; i < 6; i++) {  // loop for different window sizes
                Properties overrideProperties = new Properties();
                String windowSize = Integer.toString(i * 10);
                overrideProperties.put("window.sizes", windowSize);
                try (InformationExtractor extractor = new InformationExtractor(Props.loadProperties("init.properties"), overrideProperties)) {
                    Map<IndexBasedAVPExtractor, List<RefComparison>> res;
                    if (training)
                        res = extractor.evaluateUnarmified(trainingTestingFiles.get("train"));
                    else
                        res = extractor.evaluateUnarmified(trainingTestingFiles.get("test"));
                    //                Map<IndexBasedAVPExtractor, List<RefComparison>> res = extractor.evaluate();
                    if (training) {
//                        printResultsLatex(res, windowSize+"train", false);
                        printResultsTsv(res, windowSize+"train", verbose);
                        accumulateBestTestResults(bestTrainResults, res, windowSize);
                    } else {
//                        printResultsLatex(res, windowSize+"test", false);
                        printResultsTsv(res, windowSize+"test", verbose);
                        collectFinalResults(finalResults, bestTrainResults, res, windowSize);
                    }
                } catch (IOException | ParseException e) {
                    e.printStackTrace();
                }
            }
            training = false;
        }

        // print final list
        BufferedWriter bw = new BufferedWriter(new FileWriter("/tmp/finalResults.tsv"));
        bw.write("Entity\tF1\tWindowSize\n");
        for (Map.Entry<String, Double> resEntry : finalResults.entrySet()) {
            bw.write(resEntry.getKey() + "\t" + resEntry.getValue() + "\t" + bestTrainResults.get(resEntry.getKey()).getRight() + "\n");
        }
        bw.close();
    }

    private static void collectFinalResults(Map<String, Double> bestTestResults, Map<String, Pair<Double, String>> bestResults, Map<IndexBasedAVPExtractor, List<RefComparison>> res, String windowSize) {
        res.forEach((ext, refComps) -> {
            Pair<Double, String> f1WindowPair = bestResults.get(ext.toString());
            if (f1WindowPair == null) {
                System.err.println("WARN: Missing result for " + ext.toString());
            }
            if (f1WindowPair == null || f1WindowPair.getRight().equals(windowSize)) {
                int refCompIndex = 0;
                if (refComps.size() == 3) { // not presence entity, 'presence' entities will have only 2 refComps
                    refCompIndex = 1;
                }
                float fscore1 = refComps.get(refCompIndex).getFscore1();
                bestTestResults.put(ext.toString(), (double) fscore1);
            }
        });

    }

    private static void accumulateBestTestResults(Map<String, Pair<Double, String>> bestResults, Map<IndexBasedAVPExtractor, List<RefComparison>> res, String windowSize) {
        res.forEach((ext, refComps) -> {
            Pair<Double, String> f1WindowPair = bestResults.get(ext.toString());
            int refCompIndex = 0;
            if (refComps.size() == 3) { // not presence entity, 'presence' entities will have only 2 refComps
                refCompIndex = 1;
            }
            float fscore1 = refComps.get(refCompIndex).getFscore1();
            if (f1WindowPair == null || f1WindowPair.getLeft() < fscore1) {
                bestResults.put(ext.toString(), Pair.of((double) fscore1, windowSize));
            }
        });

    }

    private static void printResultsTsv(Map<IndexBasedAVPExtractor, List<RefComparison>> res,
            String windowSize, boolean verbose) {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter("/tmp/resultsWinSize" + windowSize + ".tsv"))) {
            if (verbose)
                bw.write("Attribute\tTP\tFP\tFN\tTN\tPrec\tRec\tF1\n");
            else
                bw.write("Attribute\tPrec\tRec\tF1\n");
            res.forEach((ext, refComps) -> {
                int i = 0;  // used to print only unarmified/armified
                for (RefComparison refComp : refComps) {
//                    if (i < 1) {  // with multiple ref comparisons, if i is 0, only print unarmfied; if i is 1 only armfied
                        try {
                            if (verbose)
                                bw.write(String.format("%s\t%d\t%d\t%d\t%d\t%.2f\t%.2f\t%.2f\n", ext, refComp.getTp(), refComp.getFp(), refComp.getFn(), refComp.getTn(), refComp.getPrec1()*100, refComp.getRecall1()*100, refComp.getFscore1()*100));
                            else
                                bw.write(String.format("%s\t%.2f\t%.2f\t%.2f\n", ext, refComp.getPrec1()*100, refComp.getRecall1()*100, refComp.getFscore1()*100));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
//                    }
                    i++;
                }
            });
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printResultsLatex(Map<IndexBasedAVPExtractor, List<RefComparison>> res, String windowSize, boolean verbose) {
        // TODO sort results to try and be consistent?
        System.out.println("\\begin{table}\n" + 
                "    \\centering\n");
        if (verbose)
            System.out.println("    \\begin{tabular}{lccccccc}\n");
        else
            System.out.println("    \\begin{tabular}{lccc}\n");
        System.out.println("\\hline\n");
        System.out.println("Attribute & Prec & Rec & F$_1$ \\\\\n\\hline");
        res.forEach((ext, refComps) -> {
            int i = 0;  // used to print only unarmified/armified
            for (RefComparison refComp : refComps) {
                if (i < 1) {  // with multiple ref comparisons, if i is 0, only print unarmfied; if i is 1 only armfied
                    if (verbose)
                        System.out.printf("%s & %d & %d & %d & %d & %.1f & %.1f & %.1f \\\\\n", ext, refComp.getTp(), refComp.getFp(), refComp.getFn(), refComp.getTn(), refComp.getPrec1() * 100, refComp.getRecall1() * 100, refComp.getFscore1() * 100);
                    else
                        System.out.printf("%s & %.1f & %.1f & %.1f \\\\\n", ext, refComp.getPrec1() * 100, refComp.getRecall1() * 100, refComp.getFscore1() * 100);
                }
                i++;
            }
        });
        System.out.println("\\hline\n" + 
                "    \\end{tabular}\n" + 
                "    \\caption{Results with unsupervised extraction (window size = " + windowSize + ")}\n" + 
                "    \\label{tab:results}\n" + 
                "\\end{table}\n");

    }

}
