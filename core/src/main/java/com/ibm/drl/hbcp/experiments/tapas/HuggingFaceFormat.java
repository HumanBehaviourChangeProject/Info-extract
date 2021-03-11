package com.ibm.drl.hbcp.experiments.tapas;

import com.opencsv.CSVWriter;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class HuggingFaceFormat {

    public static final String[] QUESTION_CSV_HEADER = { "id", "annotator", "position", "question", "table_file",
        "answer_coordinates", "answer_text" };

    public static void write(List<TapasInstance> instances, File outputFolder) throws IOException {
        outputFolder.mkdirs();
        try (CSVWriter w = new CSVWriter(new FileWriter(new File(outputFolder, "hbcp_table_questions.csv")))) {
            w.writeNext(QUESTION_CSV_HEADER);
            for (TapasInstance t : instances) {
                // write the table CSV in the table folder
                writeTableCsv(t.getTable(), t.getDocId(), t.getTableId(), outputFolder);
                // write questions in common question file
                for (int i = 0; i < t.getQuestions().size(); i++) {
                    TapasQuestion question = t.getQuestions().get(i);
                    w.writeNext(getQuestionRow(question, t.getDocId(), t.getTableId(), i));
                }
            }
        }
    }

    private static String[] getQuestionRow(TapasQuestion question, String docId, String tableId, int questionIndex) {
        String[] res = new String[QUESTION_CSV_HEADER.length];
        int i = 0;
        res[i++] = docId + "-" + tableId; // id
        res[i++] = "0"; // annotator
        res[i++] = String.valueOf(questionIndex); // position
        res[i++] = question.getText();
        res[i++] = getTableFilename(docId, tableId); // table_file
        res[i++] = getAnswerCoordinatesString(question); // answer_coordinates
        res[i++] = getAnswerTextString(question); // answer_text
        return res;
    }

    private static String getAnswerCoordinatesString(TapasQuestion question) {
        List<String> pairs = question.getAnswers().stream()
                .map(answer -> "(" + answer.getRowIndex() + ", " + answer.getColumnIndex() + ")")
                .collect(Collectors.toList());
        return wrapStrings(pairs);
    }

    private static String getAnswerTextString(TapasQuestion question) {
        List<String> texts = question.getAnswers().stream().map(TapasAnswer::getText).collect(Collectors.toList());
        return wrapStrings(texts);
    }

    private static String wrapStrings(List<String> strings) {
        List<String> escaped = strings.stream()
                .map(s -> s.contains("'") ? "\"\"\"" + s + "\"\"\"" : "'" + s + "'")
                .collect(Collectors.toList());
        return "[" + StringUtils.join(escaped, ", ") + "]";
    }

    private static void writeTableCsv(TapasTable table, String docId, String tableId, File outputFolder) throws IOException {
        File tableFile = new File(outputFolder, getTableFilename(docId, tableId));
        tableFile.getParentFile().mkdirs();
        try (CSVWriter w = new CSVWriter(new FileWriter(tableFile))) {
            for (int i = 0; i < table.getMatrix().length; i++) {
                w.writeNext(table.getMatrix()[i]);
            }
        }
    }

    private static String getTableFilename(String docId, String tableId) {
        String res = "table_csv/" + docId + "_" + tableId + ".csv";
        res = res.replaceAll("\\s", "_");
        return res;
    }
}
