package com.ibm.drl.hbcp.parser.pdf.experiments;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToAbbyyParse;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;

import lombok.Data;

/**
 * Computes various recall-like stats of tables as parsed by Grobid compared to the tables in the ABBYY PDF parsing output.
 * The PDFs used are the same set for both Grobid and ABBYY extraction, coming from Sprint 1,2,3,4.
 *
 * @author mgleize
 */
public class GrobidRecall {

    private Properties props;
    private final PdfToDocumentFunction grobidParser;
    private final PdfToDocumentFunction reparseParser;
    private final PdfToDocumentFunction abbyyParser;

    private static File PDF_FOLDER = new File("data/All_330_PDFs/");
    private static File ABBYY_PDF_FOLDER = new File("data/pdfs_Sprint1234/");
    private static File ABBYY_PDF_BY_GROBID_FOLDER = new File("data/pdfs_Sprint1234_extracted_Grobid/");

    public GrobidRecall(Properties props) {
        this.props = props;
        abbyyParser = new PdfToAbbyyParse(props);
        grobidParser = new PdfToAbbyyParse(props);
        reparseParser = new ReparsePdfToDocument(ABBYY_PDF_BY_GROBID_FOLDER);
    }

    public GrobidRecallStats computeRecallOfAbbyyTablesByGrobidTables() throws IOException {
        List<Document> abbyyDocs = new ArrayList<>();
        List<Document> grobidDocs = new ArrayList<>();
        for (File pdf : getFilesHandledByAbbyy()) {
            if (pdf.getName().endsWith(".pdf")) {
                try {
                    grobidDocs.add(reparseParser.getDocument(pdf));
                    abbyyDocs.add(abbyyParser.getDocument(pdf));
                } catch (IOException e) {
                    // for the 2 papers that couldn't be parsed by Grobid, ignore them
                }
            }
        }
        return recall(grobidDocs, abbyyDocs);
    }

    private GrobidRecallStats recall(List<Document> grobidOutput, List<Document> abbyyOutput) {
        int totalAbbyyTables = 0;
        int foundTables = 0;
        double recallOnCellsTotal = 0;
        for (int i = 0; i < abbyyOutput.size(); i++) {
            List<Set<String>> grobidTables = grobidOutput.get(i).getTables().stream()
                    .map(t -> getCells(t))
                    .collect(Collectors.toList());
            for (Block table : abbyyOutput.get(i).getTables()) {
                Set<String> abbyyCells = getCells(table);
                double recall = maxValueRecall(grobidTables, abbyyCells);
                if (recall >= 0.5) {
                    foundTables++;
                    recallOnCellsTotal += recall;
                }
                totalAbbyyTables++;
            }
        }
        return new GrobidRecallStats(recallOnCellsTotal / totalAbbyyTables,
                (double)foundTables / totalAbbyyTables);
    }

    private double maxValueRecall(List<Set<String>> grobidTables, Set<String> abbyyTable) {
        return grobidTables.stream().map(table -> valueRecall(table, abbyyTable)).max(Double::compareTo).orElse(0.0);
    }

    private double valueRecall(Set<String> grobidTable, Set<String> abbyyTable) {
        return (double)Sets.intersection(abbyyTable, grobidTable).size() / abbyyTable.size();
    }

    private Set<String> getCells(Block table) {
        return table.getTable().stream()
                .map(TableValue::getValue)
                .filter(t -> !ParsingUtils.parseAllDoubles(t).isEmpty())
                .map(ParsingUtils::parseFirstDouble)
                .map(String::valueOf)
                .collect(Collectors.toSet());
    }

    private List<File> getFilesHandledByAbbyy() {
        List<File> res = new ArrayList<>();
        for (File pdf : PDF_FOLDER.listFiles()) {
            if (pdf.getName().endsWith(".pdf")) {
                try {
                    Document docParsedByAbbyy = abbyyParser.getDocument(pdf);
                    res.add(pdf);
                } catch (IOException e) {

                }
            }
        }
        return res;
    }

    @Data
    public static class GrobidRecallStats {
        private final double cellRecall;
        private final double tableRecall;
    }

    /** This will only work with Grobid installed */
    private void analyzeEveryAbbyyPaperWithGrobid(File outputFolder) {
        List<File> abbyyPdfs = getFilesHandledByAbbyy();
        for (File pdf : abbyyPdfs) {
            try {
                Document doc = grobidParser.getDocument(pdf);
                File grobidOutput = new File(outputFolder, pdf.getName() + ".json");
                grobidOutput.getParentFile().mkdirs();
                doc.writeToFile(grobidOutput);
                System.out.println(pdf.getName() + " ... OK");
            } catch (IOException e) {
                System.err.println("FAILED FOR " + pdf.getName());
            }
        }
    }

    public static void main(String[] args) throws IOException {
        GrobidRecall grobidRecall = new GrobidRecall(Props.loadProperties());
        System.out.println(grobidRecall.computeRecallOfAbbyyTablesByGrobidTables());
    }
}
