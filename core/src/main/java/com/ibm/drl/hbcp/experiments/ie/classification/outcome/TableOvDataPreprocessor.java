package com.ibm.drl.hbcp.experiments.ie.classification.outcome;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.experiments.flair.GenerateTrainingData_NameAsCategory;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.enriching.AnnotationOutcomesMiner;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVWriter;
import edu.emory.mathcs.backport.java.util.Collections;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TableOvDataPreprocessor {

    private final PdfToDocumentFunction pdfParser;
    private final File pdfFolder;

    public TableOvDataPreprocessor(PdfToDocumentFunction pdfParser, File pdfFolder) {
        this.pdfParser = pdfParser;
        this.pdfFolder = pdfFolder;
    }

    public TableOvDataPreprocessor(Properties props) {
        this(new ReparsePdfToDocument(new File(props.getProperty("coll.extracted.reparse"))), new File(props.getProperty("coll")));
    }

    public static void main(String[] args) throws Exception {
        mainProduceTestInstances(args);
    }

    public static void mainOutcomeCellClassification(String[] args) throws Exception {
        JSONRefParser ref = new JSONRefParser(Props.loadProperties());
        TableOvDataPreprocessor ovData = new TableOvDataPreprocessor(Props.loadProperties());
        AnnotationOutcomesMiner miner = new AnnotationOutcomesMiner(Props.loadProperties());
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = miner.withOtherOutcomesInSeparateArms(ref.getAttributeValuePairs());
        List<TableOVInstance> dataset = new ArrayList<>();
        for (String docName : annotations.getDocNames()) {
            List<TableOVInstance> instances = ovData.getInstances(miner, annotations, docName);
            System.out.println("== " + docName + "=================");
            instances.stream().limit(3).forEach(i -> System.out.println(i));
            dataset.addAll(instances);
        }
        // load the FLAIR train test split
        Map<String, List<String>> trainTestDocNames = new GenerateTrainingData_NameAsCategory().generateTrainingTestingFiles();
        try (CSVWriter w = new CSVWriter(new FileWriter("output/ov.csv"))) {
            List<TableOVInstance> trainDataset = dataset.stream()
                    .filter(i -> trainTestDocNames.get("train").contains(i.getDocName()))
                    .collect(Collectors.toList());
            List<TableOVInstance> positives = trainDataset.stream()
                    .filter(i -> i.isOutcomeValue)
                    .collect(Collectors.toList());
            List<TableOVInstance> negatives = trainDataset.stream()
                    .filter(i -> !i.isOutcomeValue)
                    .collect(Collectors.toList());
            Random random = new Random(0);
            Collections.shuffle(positives, random);
            Collections.shuffle(negatives, random);
            List<TableOVInstance> balancedDataset = new ArrayList<>();
            // stratify it
            for (int i = 0; i < Math.min(positives.size(), negatives.size()); i++) {
                balancedDataset.add(positives.get(i));
                balancedDataset.add(negatives.get(i));
            }
            w.writeNext(new String[] { "idx", "sentence", "label" });
            int index = 0;
            for (TableOVInstance instance : balancedDataset) {
                w.writeNext(new String[] { String.valueOf(index++), encodeCell(instance.cell), instance.isOutcomeValue ? "1" : "0" });
            }
        }
        List<TableOVInstance> testDataset = dataset.stream()
                .filter(i -> trainTestDocNames.get("test").contains(i.getDocName()))
                .collect(Collectors.toList());
        try (CSVWriter w = new CSVWriter(new FileWriter("output/test.csv"))) {
            w.writeNext(new String[] { "idx", "sentence", "label" });
            int index = 0;
            for (TableOVInstance instance : testDataset) {
                w.writeNext(new String[] { String.valueOf(index++), encodeCell(instance.cell), instance.isOutcomeValue ? "1" : "0" });
            }
        }
    }

    public static void mainTableClassif(String[] args) throws IOException {
        JSONRefParser ref = new JSONRefParser(Props.loadProperties());
        AnnotationOutcomesMiner miner = new AnnotationOutcomesMiner(Props.loadProperties());
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = ref.getAttributeValuePairs();
        List<Block> positives = new ArrayList<>();
        List<Block> negatives = new ArrayList<>();
        for (String docName : annotations.getDocNames()) {
            Pair<List<Block>, List<Block>> positivesAndNegatives = miner.getGoldAndNonOutcomeTables(annotations, docName);
            if (positivesAndNegatives != null) {
                positives.addAll(positivesAndNegatives.getLeft());
                negatives.addAll(positivesAndNegatives.getRight());
            }
        }
        System.out.println("Positives: " + positives.size());
        System.out.println("Negatives: " + negatives.size());
        try (CSVWriter w = new CSVWriter(new FileWriter("output/tables_train.csv"))) {
            Random random = new Random(0);
            Collections.shuffle(positives, random);
            Collections.shuffle(negatives, random);
            List<Pair<Block, Boolean>> balancedDataset = new ArrayList<>();
            // stratify it
            for (int i = 0; i < Math.min(positives.size(), negatives.size()); i++) {
                balancedDataset.add(Pair.of(positives.get(i), true));
                balancedDataset.add(Pair.of(negatives.get(i), false));
            }
            w.writeNext(new String[] { "idx", "sentence", "label" });
            int index = 0;
            for (Pair<Block, Boolean> tableWithLabel : balancedDataset) {
                w.writeNext(new String[] {
                        String.valueOf(index++),
                        encodeTable(tableWithLabel.getKey()),
                        tableWithLabel.getValue() ? "1" : "0"
                });
            }
        }
        /*
        try (CSVWriter w = new CSVWriter(new FileWriter("output/tables_all.csv"))) {
            w.writeNext(new String[] { "idx", "sentence", "label" });
            int index = 0;
            List<Block> fullDataset = new ArrayList<>(positives);
            fullDataset.addAll(negatives);
            for (Block table : fullDataset) {
                w.writeNext(new String[] {
                        String.valueOf(index++),
                        encodeTable(tableWithLabel.getKey()),
                        tableWithLabel.getValue() ? "1" : "0"
                });
            }
        }
        */
    }

    public static void mainProduceTestInstances(String[] args) throws Exception {
        TableOvDataPreprocessor processor = new TableOvDataPreprocessor(Props.loadProperties());
        // load the FLAIR train test split
        Map<String, List<String>> trainTestDocNames = new GenerateTrainingData_NameAsCategory().generateTrainingTestingFiles();
        List<TableValue> cells = processor.getAllCells(trainTestDocNames.get("test"));
        List<Block> tables = processor.getAllTables(trainTestDocNames.get("test"));
        try (CSVWriter w = new CSVWriter(new FileWriter("output/test_cells.csv"))) {
            w.writeNext(new String[] { "idx", "sentence", "label" });
            int index = 0;
            for (TableValue instance : cells) {
                w.writeNext(new String[] { String.valueOf(index++), encodeCell(instance), "0" });
            }
        }
        try (CSVWriter w = new CSVWriter(new FileWriter("output/test_tables.csv"))) {
            w.writeNext(new String[] { "idx", "sentence", "label" });
            int index = 0;
            for (Block instance : tables) {
                w.writeNext(new String[] { String.valueOf(index++), encodeTable(instance), "0" });
            }
        }
    }

    private List<TableOVInstance> getInstances(AnnotationOutcomesMiner miner,
                                               AttributeValueCollection<AnnotatedAttributeValuePair> annotations,
                                               String docName) throws IOException {
        List<AnnotationOutcomesMiner.OtherOutcomeCandidate> allRelevantCells = miner.getContextCellsWithOutcomeStatus(annotations, docName);
        return allRelevantCells.stream()
                .map(c -> new TableOVInstance(c.getCell(), c.isOutcomeValue(), c.isAnnotation(), docName))
                .collect(Collectors.toList());
    }

    private List<TableValue> getAllCells(List<String> docNames) throws IOException {
        List<TableValue> res = new ArrayList<>();
        for (String docName : docNames) {
            File pdfFile = new File(pdfFolder, docName);
            // parse the PDF
            try {
                Document doc = pdfParser.getDocument(pdfFile);
                List<Block> tables = doc.getTables();
                for (Block table : tables) {
                    res.addAll(table.getTable());
                }
            } catch (FileNotFoundException fnfe) {
                // we couldn't find the parsed PDF
            }
        }
        return res;
    }

    private List<Block> getAllTables(List<String> docNames) throws IOException {
        List<Block> res = new ArrayList<>();
        for (String docName : docNames) {
            File pdfFile = new File(pdfFolder, docName);
            // parse the PDF
            try {
                Document doc = pdfParser.getDocument(pdfFile);
                List<Block> tables = doc.getTables();
                res.addAll(tables);
            } catch (FileNotFoundException fnfe) {
                // we couldn't find the parsed PDF
            }
        }
        return res;
    }

    public static String encodeCell(TableValue cell) {
        return StringUtils.join(cell.getRowHeaders(), " [SEP] ")
                + " [START] " + cell.getValue().trim() + " [END] "
                + StringUtils.join(Lists.reverse(new ArrayList<>(cell.getColumnHeaders())), " [SEP] ");
    }

    public static String encodeTable(Block table) {
        List<String> rowHeaders = table.getTable().stream()
                .flatMap(tv -> tv.getRowHeaders().stream())
                .collect(Collectors.toList());
        List<String> columnHeaders = table.getTable().stream()
                .flatMap(tv -> tv.getColumnHeaders().stream())
                .collect(Collectors.toList());
        return StringUtils.join(rowHeaders, " [SEP] ")
                + " [ROWS] "
                + StringUtils.join(columnHeaders, " [SEP] ");
    }

    @Value
    private static class TableOVInstance {
        TableValue cell;
        boolean isOutcomeValue;
        boolean isAnnotation;
        String docName;
    }
}
