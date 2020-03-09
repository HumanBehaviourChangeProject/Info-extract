package com.ibm.drl.hbcp.parser.pdf.abbyy;

import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Block;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Cell;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Row;
import lombok.Getter;

import java.util.List;

public class AbbyyTable {

    @Getter
    private final Cell[][] matrixForm;

    public AbbyyTable(Block tableBlock) {
        assert tableBlock.getBlockType().equals("Table");
        matrixForm = computeMatrixForm(tableBlock.getRows());
    }

    private Cell[][] computeMatrixForm(List<Row> rows) {
        int rowCount = getRowCount(rows);
        int columnCount = getColumnCount(rows);
        Cell[][] res = new Cell[rowCount][columnCount];
        int i, j;
        i = 0;
        for (Row row : rows) {
            j = 0;
            for (Cell cell : row.getCells()) {
                while (res[i][j] != null) j++;
                fillMatrixWithValue(res, cell, i, j, cell.getRowSpan(), cell.getColSpan());
            }
            i++;
        }
        return res;
    }

    private boolean fillMatrixWithValue(Cell[][] matrix, Cell value, int iStart, int jStart, int rowSpan, int columnSpan) {
        try {
            for (int i = iStart; i < iStart + rowSpan; i++) {
                for (int j = jStart; j < jStart + columnSpan; j++) {
                    matrix[i][j] = value;
                }
            }
            return true;
        } catch (ArrayIndexOutOfBoundsException e) {
            e.printStackTrace();
            return false;
        }
    }

    private int getColumnCount(List<Row> rows) {
        int res = 0;
        if (!rows.isEmpty()) {
            // get the first row (assumes non-emptiness)
            Row first = rows.get(0);
            // computes the total number of cells weighted by their column span
            for (Cell cell : first.getCells()) {
                res += cell.getColSpan();
            }
        }
        return res;
    }

    private int getRowCount(List<Row> rows) {
        return rows.size();
    }
}
