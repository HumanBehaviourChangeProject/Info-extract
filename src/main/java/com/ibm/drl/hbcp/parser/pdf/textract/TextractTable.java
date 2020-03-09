package com.ibm.drl.hbcp.parser.pdf.textract;

import com.ibm.drl.hbcp.parser.pdf.Cell;
import com.ibm.drl.hbcp.parser.pdf.Line;
import com.ibm.drl.hbcp.parser.pdf.TableToText;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.parser.pdf.textract.structure.Block;
import lombok.Getter;

import java.util.List;

public class TextractTable extends TextractBlock implements TextractBoundingBoxed {

    private final Cell[][] cells;

    private final List<TableValue> tableValues;

    @Getter
    private final double left;
    @Getter
    private final double top;

    protected TextractTable(Block tableBlock, List<Cell> cells) {
        super(Type.TABLE, null, cells);
        this.cells = getCellMatrix(cells);
        tableValues = new TableToText(this.cells, this).getValueCellsAndHeaders();
        left = tableBlock.getGeometry().getBoundingBox().getLeft();
        top = tableBlock.getGeometry().getBoundingBox().getTop();
    }

    private Cell[][] getCellMatrix(List<Cell> cells) {
        int rowCount = getRowCount(cells);
        int columnCount = getColumnCount(cells);
        Cell[][] res = new Cell[rowCount][columnCount];
        for (Cell cell : cells) {
            fillMatrixWithValue(res, cell, cell.getRowIndex() - 1, cell.getColumnIndex() - 1, cell.getRowSpan(), cell.getColumnSpan());
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

    private int getRowCount(List<Cell> cells) {
        return cells.stream()
                .filter(c -> c.getColumnIndex() == 1)
                // TODO: confirm that it's column span here
                .map(Cell::getRowSpan)
                .reduce(0, Integer::sum);
    }

    private int getColumnCount(List<Cell> cells) {
        return cells.stream()
                .filter(c -> c.getRowIndex() == 1)
                // TODO: confirm that it's row span here
                .map(Cell::getColumnSpan)
                .reduce(0, Integer::sum);
    }

    @Override
    public List<? extends Line> getLines() {
        return null;
    }

    @Override
    public List<TableValue> getTable() {
        return tableValues;
    }
}
