package com.ibm.drl.hbcp.experiments.tapas;

import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Cell;
import lombok.Value;

@Value
public class TapasTable {

    String[][] matrix;

    public static TapasTable createTable(Cell[][] matrix) {
        String[][] res = new String[matrix.length][];
        for (int i = 0; i < matrix.length; i++) {
            res[i] = new String[matrix[i].length];
            for (int j = 0; j < res[i].length; j++) {
                res[i][j] = preprocessProtoString(matrix[i][j].getValue());
            }
        }
        return new TapasTable(res);
    }

    /** Firt row of argument should not be empty */
    public static TapasTable createTableWithYesNoRows(Cell[][] table) {
        Cell[][] tableWithYesNoRows = new Cell[table.length + 2][];
        Cell[] yesRow = createFakeRowWithRowHeader("Yes", table[0].length);
        Cell[] noRow = createFakeRowWithRowHeader("No", table[0].length);
        tableWithYesNoRows[0] = table[0];
        tableWithYesNoRows[1] = yesRow;
        tableWithYesNoRows[2] = noRow;
        System.arraycopy(table, 1, tableWithYesNoRows, 3, table.length - 1);
        return createTable(tableWithYesNoRows);
    }

    private static Cell[] createFakeRowWithRowHeader(String label, int size) {
        Cell[] res = new Cell[size];
        res[0] = Cell.createCell(label, 1, 1);
        for (int i = 1; i < res.length; i++) {
            res[i] = Cell.createCell("", 1, 1);
        }
        return res;
    }

    private static String preprocessProtoString(String text) {
        return text.replaceAll("[\"\\n]", " ");
    }
}
