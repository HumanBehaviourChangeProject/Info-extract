package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import com.ibm.drl.hbcp.util.ParsingUtils;
import lombok.Getter;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;

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
        Optional<TableAnnotationAnalyzer.TableAnnotationAnalysis> tableAnalysis = getTableAnnotationAnalysis(avp);
        if (!tableAnalysis.isPresent())
            return Optional.empty();
        if (tablePreprocessings.isEmpty()) // no tables detected in the document
            return Optional.empty();
        // find the table most likely to contain the value (by computing some kind of recall of the cells in the context)
        List<String> numbersInTableContext = tableAnalysis.get().getNumericCellSequences().stream()
                .flatMap(List::stream)
                .map(ParsingUtils::parseFirstDoubleString)
                .collect(Collectors.toList());
        List<TablePreprocessing> closestTables = findClosestTables(avp, numbersInTableContext, tablePreprocessings);
        // find the value in the ABBYY table
        return findCell(avp.getValue(), closestTables);
    }

    private Optional<TableAnnotationAnalyzer.TableAnnotationAnalysis> getTableAnnotationAnalysis(AnnotatedAttributeValuePair avp) {
        Optional<TableAnnotationAnalyzer.TableAnnotationAnalysis> tableAnalysis = tableAnnotationAnalyzer.analyze(avp);
        if (!tableAnalysis.isPresent() && isOtherTableValue(avp)) {
            TableAnnotationAnalyzer.TableAnnotationAnalysis emptyAnalysis = new TableAnnotationAnalyzer.TableAnnotationAnalysis(
                    avp,
                    new ArrayList<>()
            );
            tableAnalysis = Optional.of(emptyAnalysis);
        }
        return tableAnalysis;
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

    private List<TablePreprocessing> findClosestTables(AnnotatedAttributeValuePair avp,
                                                       List<String> numbersInTableContext,
                                                       Map<Block, TablePreprocessing> tables) {
        List<TablePreprocessing> closestTables;
        if (numbersInTableContext.isEmpty() && isOtherTableValue(avp)) {
            // first use the Physical Activity convention (the more recent one):
            // a table value has empty context (and the table caption has been added in "highlighted text")
            String tableCaption = avp.getHighlightedText();
            closestTables = findClosestTablesWithTableCaption(tableCaption, tables);
        } else {
            // then use the Smoking Cessation convention (the older one):
            // the table value has a context with 3 rows of the table to help the locating of the table
            closestTables = findClosestTablesWithContextNumbers(numbersInTableContext, tables);
        }
        return closestTables;
    }

    private List<TablePreprocessing> findClosestTablesWithTableCaption(String tableCaption, Map<Block, TablePreprocessing> tables) {
        List<TablePreprocessing> res = new ArrayList<>();
        for (Block table : tables.keySet()) {
            TablePreprocessing preprocessedTable = tables.get(table);
            if (tableHasHeader(preprocessedTable, tableCaption)) {
                res.add(preprocessedTable);
            }
        }
        return res;
    }

    private List<TablePreprocessing> findClosestTablesWithContextNumbers(List<String> numbersInTableContext, Map<Block, TablePreprocessing> tables) {
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
        Pattern valueRegex = Pattern.compile("(?:^|[^0-9a-zA-Z.])" + escapeRegex(value) + "(?:[^0-9a-zA-Z.]|$)");
        boolean isValueADouble = ParsingUtils.isDouble(value);
        List<TableValue> matchingCells = tables.stream()
                .flatMap(table -> table.getTable().stream())
                // matches exactly the value alone in the cell
                // or matches the value contained in the cell (with smart safeguard against substrings/subdigits)
                .filter(tableValue -> useStrictCellEquality ?
                        (isValueADouble ? cellContains(tableValue, value) : tableValue.getValue().equalsIgnoreCase(value))
                        : valueRegex.matcher(tableValue.getValue()).find())
                .collect(Collectors.toList());
        // if we found multiple matching cells, it means the context/value was ambiguous and we cannot decide
        if (matchingCells.size() == 1) {
            return Optional.of(matchingCells.get(0));
        } else {
            return Optional.empty();
        }
    }

    /** Detects other types of table values that the TableAnnotationAnalyzer couldn't detect such as:
     * table values in Physical Activity for which the convention is that the context stays empty
     * and the table caption is found somewhere else */
    private boolean isOtherTableValue(AnnotatedAttributeValuePair avp) {
        // highlighted text will carry the table caption
        return avp.getContext().isEmpty() && !avp.getHighlightedText().isEmpty();
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

    private boolean tableHasHeader(TablePreprocessing table, String searchString) {
        Set<String> headers = getHeaders(table);
        for (String header : headers) {
            if (headerMatchesSearchString(header, searchString)) {
                return true;
            }
        }
        return false;
    }

    private Set<String> getHeaders(TablePreprocessing table) {
        Set<String> res = new HashSet<>();
        for (TableValue value : table.getTable()) {
            res.addAll(value.getRowHeaders());
            res.addAll(value.getColumnHeaders());
        }
        return res;
    }

    private boolean headerMatchesSearchString(@NotNull String header, String searchString) {
        // soft match on the start of the string
        if (header.length() < "Table X".length()) {
            return header.equals(searchString);
        } else {
            return searchString.startsWith(header);
        }
    }

    public static String escapeRegex(String pattern) {
        return pattern.replaceAll("[-\\[\\]{}()*+?.,\\\\\\^$|#\\s]", "\\\\$0");
    }
}
