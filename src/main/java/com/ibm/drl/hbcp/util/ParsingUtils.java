package com.ibm.drl.hbcp.util;

import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.cleaning.TableAnnotationAnalyzer;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public class ParsingUtils {

    private static final Pattern DOUBLE_REGEX = Pattern.compile("((?<![0-9])-?[0-9]*\\.?[0-9]+)");

    public static List<String> parseAllDoubleStrings(String s) {
        List<String> res = new ArrayList<>();
        Matcher m = DOUBLE_REGEX.matcher(s);
        while (m.find()) {
            res.add(m.group(1));
        }
        return res;
    }

    public static List<Double> parseAllDoubles(String s) {
        return parseAllDoubleStrings(s).stream()
                .map(Double::parseDouble)
                .collect(Collectors.toList());
    }

    public static String parseFirstDoubleString(String s) {
        List<String> strings = parseAllDoubleStrings(s);
        if (strings.isEmpty()) throw new NumberFormatException(s);
        return strings.get(0);
    }

    public static double parseFirstDouble(String s) {
        return Double.parseDouble(parseFirstDoubleString(s));
    }

    public static boolean isDouble(String s) {
        if (s == null) return false;
        List<String> doubles = parseAllDoubleStrings(s);
        return doubles.size() == 1 && doubles.get(0).equals(s);
    }

    public static List<List<String>> parseAllNumberLines(String s) {
        String[] lines = s.split("\n");
        List<List<String>> res = new ArrayList<>();
        for (String line : lines) {
            List<String> numbers = parseAllDoubleStrings(line);
            if (!numbers.isEmpty())
                res.add(numbers);
        }
        return res;
    }

    public static List<String> split(String text, List<String> separators) {
        List<String> res = new ArrayList<>();
        res.add(text);
        for (String separator : separators) {
            List<String> newRes = new ArrayList<>();
            for (String s : res) {
                List<String> splits = Arrays.asList(s.split(separator));
                newRes.addAll(splits);
            }
            res = newRes;
        }
        return res;
    }

    public static String escapeRegex(String stringForRegex) {
        return stringForRegex.replaceAll("([\\\\+*?\\[\\](){}|.^$])", "\\\\$1");
    }
}
