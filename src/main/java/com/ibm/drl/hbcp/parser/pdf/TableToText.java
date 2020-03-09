package com.ibm.drl.hbcp.parser.pdf;

import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class TableToText {

    private final Cell[][] matrix;
    private final Block table;
    private final Map<Cell, String> cellValues = new HashMap<>();

    private final static String NON_NUMERIC_VALUE_CHARACTER_REGEX = "[^0-9.\\-()=]";

    public TableToText(Cell[][] matrix, Block table) {
        this.matrix = matrix;
        this.table = table;
    }

    public List<TableValue> getValueCellsAndHeaders() {
        List<TableValue> res = new ArrayList<>();
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                Cell cell = matrix[i][j];
                String value = getCellValue(cell);
                if (isValueCell(value)) {
                    res.add(getTableValue(cell, i, j));
                }
            }
        }
        return res;
    }

    public TableValue getFromCell(Cell cell) {
        // look for the cell
        Pair<Integer, Integer> coordinates = getMatrixCoordinates(cell);
        if (coordinates != null) {
            int i = coordinates.getLeft();
            int j = coordinates.getRight();
            return getTableValue(cell, i, j);
        } else throw new RuntimeException("Cell wasn't in table: " + cell.toString());
    }

    private TableValue getTableValue(Cell cell, int i, int j) {
        Pair<List<String>, List<String>> headers = getHeaders(cell, i, j);
        return new TableValue(getCellValue(cell), headers.getLeft(), headers.getRight(), table);
    }

    private Pair<List<String>, List<String>> getHeaders(Cell cell, int rowIndex, int columnIndex) {
        List<String> rowHeaders = new ArrayList<>();
        // start from the left, find as many non-value cells as possible moving to the right
        for (int j = 0; j < columnIndex; j++) {
            String potentialRowHeader = getCellValue(matrix[rowIndex][j]);
            if (isValueCell(potentialRowHeader)) break;
            rowHeaders.add(potentialRowHeader);
        }
        List<String> colHeaders = new ArrayList<>();
        // start from the top, find as many non-value cells as possible moving to the bottom
        for (int i = 0; i < rowIndex; i++) {
            String potentialColumnHeader = getCellValue(matrix[i][columnIndex]);
            if (isValueCell(potentialColumnHeader)) break;
            colHeaders.add(potentialColumnHeader);
        }
        return Pair.of(rowHeaders, colHeaders);
    }

    private Pair<Integer, Integer> getMatrixCoordinates(Cell cell) {
        for (int i = 0; i < matrix.length; i++) {
            for (int j = 0; j < matrix[i].length; j++) {
                if (matrix[i][j] == cell)
                    return Pair.of(i, j);
            }
        }
        return null;
    }

    private String getCellValue(Cell cell) {
        return cellValues.computeIfAbsent(cell, Cell::getValue);
    }

    public static boolean isValueCell(String text) {
        String numericText = text.replaceAll(NON_NUMERIC_VALUE_CHARACTER_REGEX, "");
        boolean res = numericText.length() >= text.length() / 2.0;
        return res && !text.isEmpty();
    }
}
