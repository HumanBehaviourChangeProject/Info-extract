package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.util.ParsingUtils;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Finds table values (cells with numeric content) matching the annotations in the JSON.
 * Useful especially for outcome values, but works on any numeric-type attribute in general.
 *
 * @author mgleize
 */
public class TableValueFinder {

    private final Map<Block, TablePreprocessing> tablePreprocessings;
    private final boolean useStrictCellEquality;

    private static final TableAnnotationAnalyzer tableAnnotationAnalyzer = new TableAnnotationAnalyzer();

    public TableValueFinder(Document document, boolean useStrictCellEquality) {
        tablePreprocessings = preprocessAbbyyTable(document);
        this.useStrictCellEquality = useStrictCellEquality;
    }

    public Optional<TableValue> findTableValue(AnnotatedAttributeValuePair avp) {
        // detect if this AVP was found in a table
        Optional<TableAnnotationAnalyzer.TableAnnotationAnalysis> tableAnalysis = tableAnnotationAnalyzer.analyze(avp);
        if (!tableAnalysis.isPresent())
            return Optional.empty();
        if (tablePreprocessings.isEmpty()) // it means that the AVP actually wasn't in a table (false positive of the TableAnnotationAnalyzer)
            return Optional.empty();
        // find the table most likely to contain the value (by computing some kind of recall of the cells in the context)
        List<String> numbersInTableContext = tableAnalysis.get().getNumericCellSequences().stream()
                .flatMap(List::stream)
                .map(ParsingUtils::parseFirstDoubleString)
                .collect(Collectors.toList());
        List<TablePreprocessing> closestTables = findClosestTables(numbersInTableContext, tablePreprocessings);
        // find the value in the ABBYY table
        return findCell(avp.getValue(), closestTables);
    }

    public Pair<List<TableValue>, List<TableValue>> getSameRowAndSameColumnValues(TableValue value) {
        List<TableValue> fullTable = value.getTableBlock().getTable();
        List<TableValue> sameRow = fullTable.stream()
                .filter(otherValue -> otherValue.getRowHeaders().equals(value.getRowHeaders()))
                .filter(otherValue -> !otherValue.equals(value))
                .collect(Collectors.toList());
        List<TableValue> sameColumn = fullTable.stream()
                .filter(otherValue -> otherValue.getColumnHeaders().equals(value.getColumnHeaders()))
                .filter(otherValue -> !otherValue.equals(value))
                .collect(Collectors.toList());
        return Pair.of(sameRow, sameColumn);
    }

    private List<TablePreprocessing> findClosestTables(List<String> numbersInTableContext, Map<Block, TablePreprocessing> tables) {
        List<TablePreprocessing> res = new ArrayList<>();
        long max = Long.MIN_VALUE;
        for (Block table : tables.keySet()) {
            TablePreprocessing preprocessedTable = tables.get(table);
            long sharedCells = countSharedCells(numbersInTableContext, preprocessedTable.getNumbers());
            if (sharedCells > max) {
                max = sharedCells;
                res = Lists.newArrayList(preprocessedTable);
            } else if (sharedCells == max) {
                res.add(preprocessedTable);
            }
        }
        return res;
    }

    private Optional<TableValue> findCell(String value, List<TablePreprocessing> tables) {
        // create the regex that will match the value
        Pattern valueRegex = Pattern.compile("(?:^|[^0-9a-zA-Z])" + escapeRegex(value) + "(?:[^0-9a-zA-Z]|$)");
        boolean isValueADouble = ParsingUtils.isDouble(value);
        return tables.stream()
                .flatMap(table -> table.getTable().stream())
                // matches exactly the value alone in the cell
                // or matches the value contained in the cell (with smart safeguard against substrings/subdigits)
                .filter(tableValue -> useStrictCellEquality ?
                        (isValueADouble ? cellContains(tableValue, value) : tableValue.getValue().equalsIgnoreCase(value))
                        : valueRegex.matcher(tableValue.getValue()).find())
                .findFirst();
    }

    private Map<Block, TablePreprocessing> preprocessAbbyyTable(Document document) {
        Map<Block, TablePreprocessing> res = new LinkedHashMap<>();
        for (Block table : document.getTables()) {
            List<TableValue> tableValues = table.getTable();
            TablePreprocessing preprocessing = new TablePreprocessing(tableValues);
            res.put(table, preprocessing);
        }
        return res;
    }

    private static class TablePreprocessing {
        // this used to be a more complex class :D
        @Getter
        private final List<TableValue> table;
        @Getter
        private final Set<String> numbers;

        TablePreprocessing(List<TableValue> table) {
            this.table = table;
            numbers = table.stream()
                    .flatMap(tableValue -> ParsingUtils.parseAllDoubleStrings(tableValue.getValue()).stream())
                    .collect(Collectors.toSet());
        }
    }

    private boolean cellContains(TableValue cell, String value) {
        return ParsingUtils.parseAllDoubleStrings(cell.getValue()).contains(value);
    }

    private long countSharedCells(List<String> contextNumbers, Set<String> tableNumbers) {
        return contextNumbers.stream()
                .distinct()
                .filter(tableNumbers::contains)
                .count();
    }

    private String escapeRegex(String pattern) {
        return pattern.replaceAll("[-\\[\\]{}()*+?.,\\\\\\^$|#\\s]", "\\\\$0");
    }
}
