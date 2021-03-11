package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.experiments.flair.GenerateTrainingData_NameAsCategory;
import com.ibm.drl.hbcp.experiments.ie.classification.outcome.TableOvDataPreprocessor;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.SimplePassage;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVReader;
import org.jetbrains.annotations.NotNull;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class OutcomeValueClassificationExtractor implements IndexBasedAVPExtractor {

    private int totalPapers;
    private int noTables;
    private int noPredictedOutcomeTable;
    private int noAnswerGivenTotal;

    private final File pdfFolder;
    private final PdfToDocumentFunction pdfParser;
    private final Map<String, Document> parsedDocs = new HashMap<>();
    private final Map<String, Boolean> tablePredictions;
    private final Map<String, Boolean> cellPredictions;

    private final static Attribute ATTRIBUTE = Attributes.get().getFromName("Outcome value");

    public OutcomeValueClassificationExtractor(Properties props) throws IOException {
        pdfFolder = new File(props.getProperty("coll"));
        pdfParser = new ReparsePdfToDocument(new File(props.getProperty("coll.extracted.reparse")));
        tablePredictions = loadPredictions("../information-extraction-experiments/table-ov/test_tables.csv",
                "../information-extraction-experiments/table-ov/test_tables_predictions.csv");
        cellPredictions = loadPredictions("../information-extraction-experiments/table-ov/test_cells.csv",
                "../information-extraction-experiments/table-ov/test_cells_predictions.csv");
    }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(IndexedDocument doc) throws IOException {
        List<CandidateInPassage<ArmifiedAttributeValuePair>> res = new ArrayList<>();
        Optional<Document> parsedDocMaybe = getParsedDoc(doc.getDocName());
        if (parsedDocMaybe.isPresent()) {
            Document parsedDoc = parsedDocMaybe.get();
            List<Block> tables = parsedDoc.getTables().stream()
                    .filter(table -> tablePredictions.get(TableOvDataPreprocessor.encodeTable(table)))
                    .collect(Collectors.toList());
            if (parsedDoc.getTables().isEmpty()) {
                noTables++;
            } else if (tables.isEmpty()) {
                noPredictedOutcomeTable++;
            }
            List<TableValue> allPossibleOVCells = tables.stream()
                    .flatMap(table -> table.getTable().stream())
                    .collect(Collectors.toList());
            List<TableValue> cells = allPossibleOVCells.stream()
                    .filter(cell -> cellPredictions.get(TableOvDataPreprocessor.encodeCell(cell)))
                    .collect(Collectors.toList());
            for (TableValue cell : cells) {
                String context = cell.toText();
                ArmifiedAttributeValuePair avp = new ArmifiedAttributeValuePair(ATTRIBUTE, cell.getValue(), doc.getDocName(), Arm.EMPTY, context);
                CandidateInPassage<ArmifiedAttributeValuePair> candidate = new CandidateInPassage<>(
                        new SimplePassage(context, doc.getDocName(), 1.0),
                        avp,
                        1.0, 1.0
                );
                res.add(candidate);
            }
            if (!tables.isEmpty() && res.isEmpty()) {
                noAnswerGivenTotal++;
            }
        } else {
            noTables++;
        }
        totalPapers++;
        return res;
    }

    private Optional<Document> getParsedDoc(String docName) throws IOException {
        Document doc = parsedDocs.get(docName);
        if (doc == null) {
            File pdfFile = new File(pdfFolder, docName);
            // parse the PDF
            try {
                doc = pdfParser.getDocument(pdfFile);
            } catch (FileNotFoundException fnfe) {
                fnfe.printStackTrace();
                return Optional.empty();
            }
            parsedDocs.put(docName, doc);
        }
        return Optional.of(doc);
    }

    private static Map<String, Boolean> loadPredictions(String pathInput, String pathPredictions) throws IOException {
        Map<String, Boolean> res = new HashMap<>();
        try (CSVReader rTables = new CSVReader(new FileReader(pathInput))) {
            try (CSVReader rPredictions = new CSVReader(new FileReader(pathPredictions))) {
                rTables.readNext();
                String[] lineTables;
                String[] linePredictions;
                while ((lineTables = rTables.readNext()) != null) {
                    String encodedInput = lineTables[1];
                    linePredictions = rPredictions.readNext();
                    boolean prediction = Double.parseDouble(linePredictions[0]) < Double.parseDouble(linePredictions[1]);
                    res.put(encodedInput, prediction);
                }
            }
        }
        return res;
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
                    @Override
                    public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                        return expected.getContext().contains(predicted.getValue());
                    }
                };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new MultiValueUnarmifiedEvaluator<>(predicateUnarmifiedEvaluator),
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return Sets.newHashSet(ATTRIBUTE);
    }

    public static void main(String[] args) throws Exception {
        Properties props = Props.loadProperties("init.properties");
        OutcomeValueClassificationExtractor ovExtractor = new OutcomeValueClassificationExtractor(Props.loadProperties());
        for (RefComparison evaluation : ovExtractor.evaluate(props, new GenerateTrainingData_NameAsCategory().generateTrainingTestingFiles().get("test"))) {
            System.out.println(evaluation);
        }
        System.out.println("No tables in paper: " + ovExtractor.noTables + " / " + ovExtractor.totalPapers);
        System.out.println("No predicted outcome table (but has tables): " + ovExtractor.noPredictedOutcomeTable);
        System.out.println("No answer given (but there was a predicted outcome table): " + ovExtractor.noAnswerGivenTotal + " / " + ovExtractor.totalPapers);
    }
}
