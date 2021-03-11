package com.ibm.drl.hbcp.experiments.tapas;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.TableValueFinder;
import com.ibm.drl.hbcp.parser.pdf.TableToText;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyTable;
import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyXmlParser;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Block;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Cell;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Document;
import com.ibm.drl.hbcp.util.Props;
import lombok.Getter;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class TapasTableInstancesGenerator {

    private final AttributeValueCollection<AnnotatedAttributeValuePair> annotations;
    private final Random random = new Random(0);
    @Getter
    private final List<TapasInstanceBuilder> instances;

    public static final File ABBYY_PARSES_FOLDER = new File("../data/All_512Papers_ABBYY");
    public static final boolean USE_STRICT_CELL_EQUALITY = false;
    private static final Logger log = LoggerFactory.getLogger(TapasTableInstancesGenerator.class);

    public TapasTableInstancesGenerator() throws Exception {
        JSONRefParser refs = new JSONRefParser(Props.loadProperties());
        annotations = refs.getAttributeValuePairs();
        instances = generateAllInstances();
    }

    private List<TapasInstanceBuilder> generateAllInstances() throws IOException {
        // AnnotationOutcomeMiner cannot work because the new context is a fake sentence context, which cannot be used to locate something in a table,
        // annotations = annotationOutcomesMiner.withOtherOutcomes(annotations);
        List<TapasInstanceBuilder> res = new ArrayList<>();
        for (String docName : annotations.getDocNames()) {
            File abbyyXml = new File(ABBYY_PARSES_FOLDER, docName + ".xml");
            if (abbyyXml.exists()) {
                AbbyyXmlParser xmlParser = new AbbyyXmlParser(abbyyXml);
                Document abbyyDocument = xmlParser.getDocument();
                TableValueFinder finder = new TableValueFinder(abbyyDocument, USE_STRICT_CELL_EQUALITY);
                Map<Block, TableData> questionDataPerTable = new HashMap<>();
                Multiset<AnnotatedAttributeValuePair> annotationsForDoc = annotations.getPairsInDoc(docName);
                for (Attribute attribute : QAEntities.ENTITIES) {
                    // select relevant values for question sequence
                    List<AnnotatedAttributeValuePair> valuesForQuestion = annotationsForDoc.stream()
                            .filter(avp -> avp.getAttribute().equals(attribute))
                            .collect(Collectors.toList());
                    // find the corresponding tables and question cells
                    Map<Block, List<AnnotatedCell>> tableToValueCells = findQuestionCells(valuesForQuestion, finder);
                    // produce question data
                    for (Map.Entry<Block, List<AnnotatedCell>> tableAndQuestionCells : tableToValueCells.entrySet()) {
                        QuestionData questionData = new QuestionData(tableAndQuestionCells.getValue(), attribute);
                        questionDataPerTable.putIfAbsent(tableAndQuestionCells.getKey(), new TableData());
                        TableData tableData = questionDataPerTable.get(tableAndQuestionCells.getKey());
                        // add the question data to the table
                        tableData.getQuestionData().add(questionData);
                        // update the list of unseen attributes for this table
                        tableData.getUnseenAttributes().remove(attribute);
                    }
                }
                // generate question data of the negatives for attribute presence
                for (TableData tableData : questionDataPerTable.values()) {
                    List<Attribute> originalUnseen = new ArrayList<>(tableData.getUnseenAttributes());
                    List<Attribute> remainingUnseen = new ArrayList<>(originalUnseen);
                    // create as many negatives for this table as there are valid attribute question sequences
                    final int positiveQuestionCount = tableData.getQuestionData().size();
                    for (int i = 0; i < positiveQuestionCount; i++) {
                        Attribute negativeAttribute = remainingUnseen.isEmpty()
                                ? originalUnseen.get(random.nextInt(originalUnseen.size()))
                                : remainingUnseen.remove(0);
                        QuestionData negative = new QuestionData(new ArrayList<>(), negativeAttribute);
                        tableData.getQuestionData().add(negative);
                    }
                }
                // create TAPAS instances
                int instanceId = 0;
                int tableId = 0;
                for (Block table : questionDataPerTable.keySet()) {
                    // get arm and follow-up answers
                    Map<Arm, Answer> armAnswers = getArmCells(table, annotations.getArmifiedPairsInDoc(docName).keySet());
                    Optional<Answer> followUpAnswer = getLongestFollowUpCell(table, annotationsForDoc);
                    // in this setup, create one Tapas instance per question data
                    for (QuestionData questionData : questionDataPerTable.get(table).getQuestionData()) {
                        TapasInstanceBuilder instance = new TapasInstanceBuilder(docName, instanceId++, table, tableId++, Lists.newArrayList(questionData), armAnswers, followUpAnswer);
                        res.add(instance);
                    }
                }
            }
        }
        log.info("Total TAPAS instances: {}", res.size());
        return res;
    }

    public void writeTapasTrainDevTestSplit() throws Exception {
        // write train, dev, test files
        Map<String, List<TapasInstanceBuilder>> trainTestInstances = QAEntities.split(instances);
        for (Map.Entry<String, List<TapasInstanceBuilder>> docSplitInstances : trainTestInstances.entrySet()) {
            try (BufferedWriter bw = new BufferedWriter(new FileWriter("output/tapas_fullQA_" + docSplitInstances.getKey() + ".txt"))) {
                log.info("TAPAS {}: {} instances.", docSplitInstances.getKey(), docSplitInstances.getValue().size());
                for (TapasInstanceBuilder instance : docSplitInstances.getValue()) {
                    bw.write(instance.getTapasProtoText2());
                    bw.newLine();
                }
            }
        }
    }

    public void writeTapasTrainDevTestSplitHuggingFace() throws Exception {
        // write train, dev, test files
        Map<String, List<TapasInstanceBuilder>> trainTestInstances = QAEntities.split(instances);
        for (Map.Entry<String, List<TapasInstanceBuilder>> docSplitInstances : trainTestInstances.entrySet()) {
            List<TapasInstance> instances = docSplitInstances.getValue().stream()
                    .map(TapasInstanceBuilder::build).collect(Collectors.toList());
            HuggingFaceFormat.write(instances, new File("output/sqa_hbcp_tables_" + docSplitInstances.getKey()));
        }
    }

    private static Map<Block, List<AnnotatedCell>> findQuestionCells(List<AnnotatedAttributeValuePair> values,
                                                                                              TableValueFinder finder) {
        Map<Block, List<AnnotatedCell>> res = new HashMap<>();
        for (AnnotatedAttributeValuePair value : values) {
            Optional<TableValue> maybeCell = finder.findTableValue(value);
            if (maybeCell.isPresent()) {
                Block table = (Block) maybeCell.get().getTableBlock();
                res.putIfAbsent(table, new ArrayList<>());
                res.get(table).add(new AnnotatedCell(maybeCell.get(), value));
            }
        }
        return res;
    }

    private static Map<Arm, TapasTableInstancesGenerator.Answer> getArmCells(Block table, Collection<Arm> arms) {
        Cell[][] matrix = new AbbyyTable(table).getMatrixForm();
        Map<Arm, TapasTableInstancesGenerator.Answer> res = new HashMap<>();
        for (Arm arm : arms) {
            if (!arm.isEmptyArm()) {
                Optional<TapasTableInstancesGenerator.Answer> matchedCell = matchStringsWithCell(arm.getAllNames(), matrix, table);
                matchedCell.ifPresent(a -> res.put(arm, a));
            }
        }
        return res;
    }

    private static Optional<TapasTableInstancesGenerator.Answer> getLongestFollowUpCell(Block table, Collection<AnnotatedAttributeValuePair> annotationsForDoc) {
        return annotationsForDoc.stream()
                .filter(aavp -> aavp.getAttribute().isTimepoint())
                .findFirst()
                .flatMap(followUpAvp -> matchStringsWithCell(Lists.newArrayList(followUpAvp.getValue()), new AbbyyTable(table).getMatrixForm(), table));
    }

    private static Optional<TapasTableInstancesGenerator.Answer> matchStringsWithCell(List<String> possibleValues, Cell[][] matrix, Block block) {
        List<TapasTableInstancesGenerator.Answer> possibleAnswers = new ArrayList<>();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                final int iFinal = i;
                final int jFinal = j;
                boolean isCorrectCell = possibleValues.stream()
                        .anyMatch(armName -> matrix[iFinal][jFinal].getValue().contains(armName));
                if (isCorrectCell) {
                    TableToText ttt = new TableToText(block.getTableInMatrixForm(), block);
                    TableValue cell = ttt.getTableValue(Block.convertAbbyyCell(matrix[i][j], i, j), i, j);
                    possibleAnswers.add(new TapasTableInstancesGenerator.Answer(matrix[i][j].getValue(), iFinal, jFinal, cell));
                }
            }
        }
        // return the shortest answer's cell
        if (possibleAnswers.isEmpty()) {
            return Optional.empty();
        } else {
            return possibleAnswers.stream()
                    .sorted(Comparator.comparing(a -> a.getAnswerText().length()))
                    .findFirst();
        }
    }

    @Value
    public static class TableData {
        List<QuestionData> questionData = new ArrayList<>();
        Set<Attribute> unseenAttributes = new HashSet<>(QAEntities.ENTITIES);
    }

    @Value
    public static class QuestionData {
        List<AnnotatedCell> annotatedCells;
        Attribute attribute;
    }

    @Value
    public static class AnnotatedCell {
        TableValue cell;
        AnnotatedAttributeValuePair annotation;
    }

    @Value
    public static class Answer {
        String answerText;
        int rowIndex;
        int columnIndex;
        TableValue cell;

        public static final Answer YES = new Answer("Yes", 1, 0, null);
        public static final Answer NO = new Answer("No", 2, 0, null);

        public boolean isYesOrNo() {
            return this == YES || this == NO;
        }

        public String toTextForQuestion() {
            return answerText != null ? answerText : cell.getValue();
        }

    }

    public static void main(String[] args) throws Exception {
        TapasTableInstancesGenerator gen = new TapasTableInstancesGenerator();
        gen.writeTapasTrainDevTestSplitHuggingFace();
    }
}
