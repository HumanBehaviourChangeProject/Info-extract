package com.ibm.drl.hbcp.experiments.lrec20;

import com.ibm.drl.hbcp.extraction.extractors.IndexBasedAVPExtractor;
import com.ibm.drl.hbcp.extraction.extractors.InformationExtractor;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.util.Props;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public class GenerateExtractionResults {

    public static void main(String[] args) {
        // verbose output shows tp/fp/fn/tn counts
        boolean verbose = true;
        
        for (int i = 1; i < 6; i++) {
            Properties overrideProperties = new Properties();
            String windowSize = Integer.toString(i * 10);
//            String windowSize = "10,20,30,40,50";
            overrideProperties.put("window.sizes", windowSize);
            try (InformationExtractor extractor = new InformationExtractor(Props.loadProperties("src/main/java/com/ibm/drl/hbcp/experiments/lrec20/lrec.properties"), overrideProperties)) {
                Map<IndexBasedAVPExtractor, List<RefComparison>> res = extractor.evaluateUnarmified();
//                Map<IndexBasedAVPExtractor, List<RefComparison>> res = extractor.evaluate();
                printResultsLatex(res, windowSize, false);
                printResultsTsv(res, windowSize, verbose);
            } catch (IOException | ParseException e) {
                e.printStackTrace();
            }
        }
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
                    if (i < 1) {  // with multiple ref comparisons, if i is 0, only print unarmfied; if i is 1 only armfied
                        try {
                            if (verbose)
                                bw.write(String.format("%s\t%d\t%d\t%d\t%d\t%.2f\t%.2f\t%.2f\n", ext, refComp.getTp(), refComp.getFp(), refComp.getFn(), refComp.getTn(), refComp.getPrec1()*100, refComp.getRecall1()*100, refComp.getFscore1()*100));
                            else
                                bw.write(String.format("%s\t%.2f\t%.2f\t%.2f\n", ext, refComp.getPrec1()*100, refComp.getRecall1()*100, refComp.getFscore1()*100));
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    }
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
