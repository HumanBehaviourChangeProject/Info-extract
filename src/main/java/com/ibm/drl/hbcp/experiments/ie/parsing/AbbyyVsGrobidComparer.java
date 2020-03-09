package com.ibm.drl.hbcp.experiments.ie.parsing;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.extraction.extractors.IndexBasedAVPExtractor;
import com.ibm.drl.hbcp.extraction.extractors.InformationExtractor;
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexerWithParser;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToAbbyyParse;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToGrobidParse;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVWriter;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Compares ABBYY and Grobid in term of the Extractors' performance.
 * Requires Grobid installed and set up correctly, will crash otherwise.
 *
 * @author mgleize
 */
public class AbbyyVsGrobidComparer {

    public static final String PDF_FOLDER = "data/pdfs_413";
    public static final String OUTPUT_FOLDER = "data/temp/";

    private final List<PdfToDocumentFunction> parsers;
    private final Properties props;

    public AbbyyVsGrobidComparer() throws IOException {
        props = Props.loadProperties();
        parsers = Lists.newArrayList(
                new PdfToAbbyyParse(props),
                new PdfToGrobidParse()
        );
    }

    /** Parses all the PDFs with both parsers (some papers will fail for each) */
    public void tryToParseWithAll(File pdfFolder) {
        for (File pdf : pdfFolder.listFiles()) {
            if (pdf.getName().endsWith(".pdf")) {
                for (PdfToDocumentFunction parser : parsers) {
                    try {
                        System.out.println(pdf.getName() + " with " + parser.toString());
                        Document doc = parser.getDocument(pdf);
                        doc.writeToFile(new File(OUTPUT_FOLDER + parser.toString() + "/" + pdf.getName() + ".json"));
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }
            }
        }
    }

    /** Returns the name of the PDF output files (things like "2014 Smith.pdf.json") obtained with both parsers, the ones
     * which one of them failed on are not returned. */
    public Set<String> getCommonPaperFilenames() {
        Map<String, Set<String>> parserToFilenames = new HashMap<>();
        for (File parserFolder : new File(OUTPUT_FOLDER).listFiles()) {
            if (parserFolder.isDirectory()) {
                for (File parserOutput : parserFolder.listFiles()) {
                    if (parserOutput.getName().endsWith(".pdf.json")) {
                        parserToFilenames.putIfAbsent(parserFolder.getName(), new HashSet<>());
                        parserToFilenames.get(parserFolder.getName()).add(parserOutput.getName().replace(".pdf.json", ".pdf"));
                    }
                }
            }
        }
        Set<String> res = new HashSet<>(parserToFilenames.values().iterator().next());
        for (Set<String> outputs : parserToFilenames.values()) {
            res.retainAll(outputs);
        }
        return res;
    }

    /** folderName can be either 'Abbyy' or 'Grobid' */
    public Map<String, List<PRF>> evaluateWith(String folderName, Set<String> commonPaperFilenames) throws IOException, TikaException, SAXException, ParseException {
        // index the papers using the parsing output
        PaperIndexer indexer = new PaperIndexerWithParser("init.properties", new ReparsePdfToDocument(new File("data/temp/" + folderName))) {
            @Override
            protected boolean isValid(File pdf) {
                return super.isValid(pdf) && commonPaperFilenames.contains(pdf.getName());
            }
        };
        indexer.processAll();
        // before evaluation, override the prop that points to the JSON parser output folder
        InformationExtractor ie = new InformationExtractor(Props.overrideProps(props, Lists.newArrayList(
                Pair.of("coll.extracted.reparse", "data/temp/" + folderName))));
        Map<IndexBasedAVPExtractor, List<RefComparison>> evaluation = ie.evaluate();
        return parseEvaluateOutput(evaluation);
    }

    /** Was a std output parser initially, now it looks useless mostly */
    private Map<String, List<PRF>> parseEvaluateOutput(Map<IndexBasedAVPExtractor, List<RefComparison>> output) throws IOException {
        Map<String, List<PRF>> res = new HashMap<>();
        for (IndexBasedAVPExtractor extractor : output.keySet()) {
            List<RefComparison> evals = output.get(extractor);
            String key = extractor.toString().replaceAll("@[abcdef0-9]+", "");
            List<PRF> perfs = evals.stream()
                    .map(refComparison -> new PRF(refComparison.getPrec1(), refComparison.getRecall1(), refComparison.getFscore1()))
                    .collect(Collectors.toList());
            res.put(key, perfs);
        }
        return res;
    }

    /** the value of the return object is a list of rows (one row per evaluator). Rows have several column (one per evaluation/parser)
     * evaluations should be non-empty
     * */
    private Map<String, List<List<PRF>>> transposeEvaluations(List<Map<String, List<PRF>>> evaluations) {
        // first, simply groups across attributes
        Map<String, List<List<PRF>>> nonTransposedRes = new LinkedHashMap<>();
        for (Map<String, List<PRF>> evaluation : evaluations) {
            for (Map.Entry<String, List<PRF>> row : evaluation.entrySet()) {
                nonTransposedRes.putIfAbsent(row.getKey(), new ArrayList<>());
                nonTransposedRes.get(row.getKey()).add(row.getValue());
            }
        }
        // at this stage we have all the rows from 1 evaluation, then all the rows from the second evaluation, we need to zip those lists
        Map<String, List<List<PRF>>> res = new LinkedHashMap<>();
        for (String attribute : nonTransposedRes.keySet()) {
            List<List<PRF>> allPerfRowsPerEvaluation = nonTransposedRes.get(attribute);
            if (allPerfRowsPerEvaluation.isEmpty()) {
                res.put(attribute, new ArrayList<>());
            } else {
                List<List<PRF>> transposed = new ArrayList<>();
                for (int i = 0; i < allPerfRowsPerEvaluation.get(0).size(); i++) {
                    List<PRF> newRow = new ArrayList<>();
                    for (List<PRF> fullRowsOfOneEvaluation : allPerfRowsPerEvaluation) {
                        newRow.add(fullRowsOfOneEvaluation.get(i));
                    }
                    transposed.add(newRow);
                }
                res.put(attribute, transposed);
            }
        }
        return res;
    }

    public void writeEvaluationCsv(File csv, List<Map<String, List<PRF>>> evaluations) throws IOException {
        Map<String, List<List<PRF>>> transposedEvaluation = transposeEvaluations(evaluations);
        try (CSVWriter w = new CSVWriter(new FileWriter(csv))) {
            // write header
            w.writeNext(new String[] {"", "P(A)", "P(G)", "R(A)", "R(G)", "F(A)", "F(G)"});
            for (String attribute : transposedEvaluation.keySet()) {
                List<List<PRF>> perfMatrix = transposedEvaluation.get(attribute);
                for (List<PRF> row : perfMatrix) {
                    // add the columns in the order you want (PRF is 3 numbers)
                    List<String> csvRow = new ArrayList<>();
                    csvRow.add(attribute);
                    for (PRF perfForEvaluation : row) { csvRow.add(String.valueOf(perfForEvaluation.precision)); }
                    for (PRF perfForEvaluation : row) { csvRow.add(String.valueOf(perfForEvaluation.recall)); }
                    for (PRF perfForEvaluation : row) { csvRow.add(String.valueOf(perfForEvaluation.fscore)); }
                    String[] csvRowArray = new String[0];
                    w.writeNext(csvRow.toArray(csvRowArray));
                }
            }
        }
    }



    @Data
    public static class PRF {
        private final double precision;
        private final double recall;
        private final double fscore;
    }

    public static void main(String[] args) throws IOException, ParseException, TikaException, SAXException {
        AbbyyVsGrobidComparer comparer = new AbbyyVsGrobidComparer();
        // run the two parsers on the entire set of PDFs (can be commented out after the first call of main when running multiple times)
        comparer.tryToParseWithAll(new File(PDF_FOLDER));
        Set<String> commonPapers = comparer.getCommonPaperFilenames();
        System.out.println("# of common papers: " + commonPapers.size());

        // evaluate with Abbyy then Grobid; will override the index; zero, one or both can be commented out
        Map<String, List<PRF>> evalAbbyy = comparer.evaluateWith("Abbyy", commonPapers);
        Map<String, List<PRF>> evalGrobid = comparer.evaluateWith("Grobid", commonPapers);
        comparer.writeEvaluationCsv(new File("data/temp/abbyy_vs_grobid.csv"), Lists.newArrayList(evalAbbyy, evalGrobid));
    }
}