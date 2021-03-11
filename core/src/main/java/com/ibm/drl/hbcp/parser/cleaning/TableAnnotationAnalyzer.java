package com.ibm.drl.hbcp.parser.cleaning;

import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class TableAnnotationAnalyzer {

    //private final String numberRegex = "(\\(?-?\\d{1,3}(,\\d{3})*(\\.\\d+)?|^\\.\\d+\\)?)";
    private static final String numberRegex = "(?:-?\\d{1,3}(,\\d{3})*(\\.\\d+)?|^\\.\\d+)";
    private static final String extraCharactersRegex = "[0-9.\\-/()%]*";
    private static final String numberWithExtraCharacters = "(?:"
            + extraCharactersRegex
            + numberRegex
            + extraCharactersRegex
            + ")";
    private static final String cellSeparatorRegex = "[ \t]+";
    // 2 numbers separated by a space
    private static final Pattern tableRegex = Pattern.compile(numberRegex + cellSeparatorRegex + numberRegex);
    // 3 non-alpha tokens separated by a space
    private static final Pattern tableRegex2 = Pattern.compile(numberWithExtraCharacters + cellSeparatorRegex + numberWithExtraCharacters + cellSeparatorRegex + numberWithExtraCharacters);
    //private final Pattern numberCellSequenceRegex = Pattern.compile(numberRegex + "(?: " + numberRegex + ")+");
    private static final String numberSeparatorsRegex = " +(?:[^A-Za-z0-9]+ +)?";
    private static final Pattern numberCellSequenceRegex = Pattern.compile(numberWithExtraCharacters + "(?:" + numberSeparatorsRegex + numberWithExtraCharacters + ")+");

    public Optional<TableAnnotationAnalysis> analyze(AnnotatedAttributeValuePair avp) {
        if (containsNumberCells(avp.getContext())) {
            return Optional.of(new TableAnnotationAnalysis(avp, getAllSequences(avp.getContext())));
        } else {
            return Optional.empty();
        }
    }

    public static boolean isInTable(AnnotatedAttributeValuePair avp) {
        return new TableAnnotationAnalyzer().analyze(avp).isPresent();
    }

    public List<TableAnnotationAnalysis> analyze(Collection<AnnotatedAttributeValuePair> avps) {
        return avps.stream()
                .filter(avp -> containsNumberCells(avp.getContext()))
                .map(avp -> new TableAnnotationAnalysis(avp, getAllSequences(avp.getContext())))
                .collect(Collectors.toList());
    }

    public List<List<String>> getAllSequences(String context) {
        List<List<String>> res = new ArrayList<>();
        Matcher matcher = numberCellSequenceRegex.matcher(context);
        while (matcher.find()) {
            res.add(Arrays.asList(matcher.group().split(numberSeparatorsRegex)));
        }
        return res;
    }

    public boolean containsNumberCells(String context) {
        boolean matchesNumberSpaceNumber = tableRegex.matcher(context).find();
        boolean matchesMoreComplexPattern = tableRegex2.matcher(context).find();
        return matchesNumberSpaceNumber || matchesMoreComplexPattern;
    }

    @Data
    public static class TableAnnotationAnalysis {
        private final AnnotatedAttributeValuePair avp;
        private final List<List<String>> numericCellSequences;
    }

    public static void main(String[] args) throws IOException {
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = new JSONRefParser(new File("data/jsons/Smoking_AllAnnotations_01Apr19.json")).getAttributeValuePairs();
        TableAnnotationAnalyzer tad = new TableAnnotationAnalyzer();
        List<TableAnnotationAnalysis> analyses = tad.analyze(avps.getAllPairs());
        for (TableAnnotationAnalysis analysis : analyses) {
            System.out.println(analysis.getAvp().getContext());
            for (List<String> sequence : analysis.getNumericCellSequences()) {
                System.out.println("\t" + sequence);
            }
        }
    }

}
