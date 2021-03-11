package com.ibm.drl.hbcp.experiments.tapas;

import com.google.common.base.CaseFormat;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyTable;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Block;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Cell;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.stream.Collectors;

@Value
public class TapasInstanceBuilder {
    String docName;
    int id;
    Block tableInPdf; // in ABBYY format
    int tableId;
    List<TapasTableInstancesGenerator.QuestionData> questions;
    Map<Arm, TapasTableInstancesGenerator.Answer> armAnswers;
    Optional<TapasTableInstancesGenerator.Answer> longestFollowUpCell;

    public static final boolean ADD_YES_OR_NO_IN_TABLE = true;

    public TapasInstance build() {
        // finalize the table (potentially adding Yes/No rows)
        Cell[][] matrix = new AbbyyTable(tableInPdf).getMatrixForm();
        TapasTable table = ADD_YES_OR_NO_IN_TABLE ? TapasTable.createTableWithYesNoRows(matrix) : TapasTable.createTable(matrix);
        List<TapasQuestion> tapasQuestions = new ArrayList<>();
        // Generate the questions
        for (TapasTableInstancesGenerator.QuestionData question : questions) {
            boolean isAttributePresent = !question.getAnnotatedCells().isEmpty();
            // question about attribute presence
            TapasQuestion questionAttributePresence = getQuestion(
                    question.getAttribute().getName() + " " + " attribute presence",
                    "Is " + question.getAttribute().getName() + " present?",
                    Lists.newArrayList(isAttributePresent ? TapasTableInstancesGenerator.Answer.YES : TapasTableInstancesGenerator.Answer.NO),
                    matrix
            );
            tapasQuestions.add(questionAttributePresence);
            if (isAttributePresent) { // only ask further questions if the attribute is present in the table
                // arm presence question
                boolean armPresence = question.getAnnotatedCells().stream().noneMatch(annotatedCell -> annotatedCell.getAnnotation().getArm().isEmptyArm())
                        && !armAnswers.isEmpty();
                TapasQuestion questionArmPresence = getQuestion(
                        question.getAttribute().getName() + " arm presence",
                        "Are arm names present?",
                        Lists.newArrayList(armPresence ? TapasTableInstancesGenerator.Answer.YES : TapasTableInstancesGenerator.Answer.NO),
                        matrix
                );
                tapasQuestions.add(questionArmPresence);
                if (armPresence) {
                    // arms question
                    TapasQuestion questionArmExtraction = getQuestion(
                            question.getAttribute().getName() + " arms",
                            "What are the arm names?",
                            new ArrayList<>(armAnswers.values()),
                            matrix
                    );
                    tapasQuestions.add(questionArmExtraction);
                }
                if (question.getAttribute().isOutcomeValue()) {
                    // follow-up presence
                    boolean followUpPresence = longestFollowUpCell.isPresent();
                    TapasQuestion questionFollowUpPresence = getQuestion(
                            question.getAttribute().getName() + " follow-up presence",
                            "Are follow-ups present?",
                            Lists.newArrayList(followUpPresence ? TapasTableInstancesGenerator.Answer.YES : TapasTableInstancesGenerator.Answer.NO),
                            matrix
                    );
                    tapasQuestions.add(questionFollowUpPresence);
                    if (followUpPresence) {
                        // follow-up extraction question
                        TapasQuestion questionFollowUpExtraction = getQuestion(
                                question.getAttribute().getName() + " follow-up",
                                "What is the longest follow-up?",
                                Lists.newArrayList(longestFollowUpCell.get()),
                                matrix
                        );
                        tapasQuestions.add(questionFollowUpPresence);
                    }
                }
                // final questions for all arms found
                for (TapasTableInstancesGenerator.AnnotatedCell annotatedCell : question.getAnnotatedCells()) {
                    TapasQuestion questionFinalExtraction = getQuestion(
                            question.getAttribute().getName() + " " + annotatedCell.getAnnotation().getArm().getStandardName() + " final",
                            generateSimpleQuestion(
                                    annotatedCell.getAnnotation(),
                                    getArmName(annotatedCell.getAnnotation().getArm(), armAnswers.get(annotatedCell.getAnnotation().getArm()))
                            ),
                            Lists.newArrayList(new TapasTableInstancesGenerator.Answer(null, 0, 0, annotatedCell.getCell())),
                            matrix
                    );
                    tapasQuestions.add(questionFinalExtraction);
                }
            }
        }
        return new TapasInstance(docName, String.valueOf(tableId), table, tapasQuestions);
    }

    /*
    public String getTapasProtoText() {
        Cell[][] matrix = new AbbyyTable(table).getMatrixForm();
        StringBuilder sb = new StringBuilder();
        // id + table
        sb.append("id: \"" + docName + "_" + id + "\" table: { ");
        sb.append(StringUtils.join(Arrays.stream(matrix[0]).map(cell -> cellToTapasProtoText(cell, "columns")).collect(Collectors.toList()), " "));
        for (int i = 1; i < matrix.length; i++) {
            sb.append(" ");
            sb.append(rowToTapasProtoText(matrix[i]));
        }
        sb.append(" table_id: \"" + docName + "_" + tableId + "\" } ");
        // question(s)
        for (TapasTableInstancesGenerator.QuestionData question : questions) {
            sb.append(getQuestionProtoText(
                    preprocessId(question.getAnnotation().getAttribute().getName() + " " + question.getAnnotation().getArm().getStandardName()),
                    preprocessProtoString(generateSimpleQuestion(question.getAnnotation(),
                            question.getAnnotation().getArm().getStandardName(),
                            question.getAllArmAnnotations())),
                    Lists.newArrayList(new TapasTableInstancesGenerator.Answer("", 0, 0, question.getTableCell())),
                    matrix
            ));
        }
        return sb.toString().replaceAll(" {2}", " ");
    }*/

    public String getTapasProtoText2() {
        TapasInstance t = build();
        String[][] matrix = t.getTable().getMatrix();
        StringBuilder sb = new StringBuilder();
        // id + table
        sb.append("id: \"" + t.getDocId() + "_" + id + "\" table: { ");
        // write "column headers" row (first row)
        sb.append(StringUtils.join(Arrays.stream(matrix[0]).map(cell -> cellToTapasProtoText(cell, "columns")).collect(Collectors.toList()), " "));
        // write rest of the table
        for (int i = 1; i < matrix.length; i++) {
            sb.append(" ");
            sb.append(rowToTapasProtoText(matrix[i]));
        }
        sb.append(" table_id: \"" + t.getDocId() + "_" + t.getTableId() + "\" } ");
        // question data(s) (there should be only 1)
        for (TapasQuestion question : t.getQuestions()) {
            sb.append(getQuestionProtoText(question));
        }
        return sb.toString().replaceAll(" {2}", " ");
    }

    private TapasQuestion getQuestion(String id, String text, List<TapasTableInstancesGenerator.Answer> answers, Cell[][] matrix) {
        List<TapasAnswer> tapasAnswers = new ArrayList<>();
        for (TapasTableInstancesGenerator.Answer cell : answers) {
            int rowIndex, columnIndex;
            if (cell.getAnswerText() != null) {
                rowIndex = cell.getRowIndex();
                columnIndex = cell.getColumnIndex();
            } else {
                Pair<Integer, Integer> coordinates = getAnswerCoordinate(cell.getCell(), matrix);
                rowIndex = coordinates.getLeft();
                columnIndex = coordinates.getRight();
            }
            if (ADD_YES_OR_NO_IN_TABLE) {
                // we're forced to shift the row index
                rowIndex = cell.isYesOrNo() || rowIndex == 0 ? rowIndex : rowIndex + 2;
            }
            String answerText = preprocessProtoString(cell.toTextForQuestion());
            TapasAnswer tapasAnswer = new TapasAnswer(answerText, rowIndex, columnIndex);
            tapasAnswers.add(tapasAnswer);
        }
        return new TapasQuestion(
                preprocessId(id),
                preprocessProtoString(text),
                tapasAnswers
        );
    }

    private String getQuestionProtoText(TapasQuestion question) {
        StringBuilder sb = new StringBuilder();
        sb.append("questions: { ");
        sb.append("id: \"");
        sb.append(question.getId());
        sb.append("\" ");
        sb.append("original_text: \"");
        sb.append(question.getText());
        sb.append("\" ");
        sb.append("answer: { ");
        // print coordinates
        for (TapasAnswer cell : question.getAnswers()) {
            sb.append("answer_coordinates: { ");
            sb.append("row_index: " + cell.getRowIndex() + " ");
            sb.append("column_index: " + cell.getColumnIndex());
            sb.append(" } ");
        }
        for (TapasAnswer cell : question.getAnswers()) {
            sb.append("answer_texts: \"" + cell.getText() + "\" ");
        }
        sb.append("}");
        sb.append(" } ");
        return sb.toString();
    }

    private static String cellToTapasProtoText(String cell, String name) {
        return name + ": { text: \"" + preprocessProtoString(cell) + "\" }";
    }

    private static String rowToTapasProtoText(String[] row) {
        return "rows: { " +
                StringUtils.join(Arrays.stream(row).map(cell -> cellToTapasProtoText(cell, "cells")).collect(Collectors.toList()), " ") +
                " }";
    }

    private static String preprocessProtoString(String text) {
        return text.replaceAll("[\"\\n]", " ");
    }

    private static String preprocessId(String id) {
        String res = id;
        res = res.toLowerCase();
        res = res.replaceAll("[^a-zA-Z0-9]", "_");
        res = CaseFormat.LOWER_UNDERSCORE.to(CaseFormat.UPPER_CAMEL, res);
        return res;
    }

    private String generateSimpleQuestion(AnnotatedAttributeValuePair annotation, String armName) {
        return "What is the "
                + annotation.getAttribute().getName()
                + (!armName.equals("all") ? " of " + armName : "")
                + getFollowUpQuestionPart(annotation)
                + "?";
    }

    private static String getArmName(Arm arm, TapasTableInstancesGenerator.Answer armAnswer) {
        if (armAnswer != null) {
            return armAnswer.getAnswerText();
        } else {
            return arm.isEmptyArm() ? "all" : arm.getStandardName();
        }
    }

    private String getFollowUpQuestionPart(AnnotatedAttributeValuePair annotation) {
        if (annotation.getAttribute().isOutcomeValue() && longestFollowUpCell.isPresent()) {
            return " at " + longestFollowUpCell.get().toTextForQuestion();
        } else return "";
    }

    private Pair<Integer, Integer> getAnswerCoordinate(TableValue cell, Cell[][] matrix) {
        Pair<Integer, Integer> res = null;
        int answerCount = 0;
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                String matrixValue = matrix[i][j].getValue();
                if (cell.getValue().equals(matrixValue)) {
                    answerCount++;
                    res = Pair.of(i, j);
                }
            }
        }
        if (answerCount == 1) {
            return res;
        } else {
            if (answerCount > 1) {
                throw new RuntimeException("Too many answers for this question.");
            } else {
                throw new RuntimeException("No answer found in table.");
            }
        }
    }
}