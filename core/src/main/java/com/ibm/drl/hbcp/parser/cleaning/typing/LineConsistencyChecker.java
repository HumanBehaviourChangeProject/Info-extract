package com.ibm.drl.hbcp.parser.cleaning.typing;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * In a value comprising multiple lines (separated with \n), this class checks that the lines are all of homogeneous types,
 * meaning that you can split it across the \n and produce several values (of the same type) with the same context.
 *
 * @author marting
 */
public class LineConsistencyChecker {

    private static final Pattern TEXT_REGEX = Pattern.compile("[a-zA-Z][a-zA-Z]+");
    private static final Pattern INTEGER_REGEX = Pattern.compile("(?<!\\.)[0-9]+(?!\\.)");
    private static final Pattern PURE_FLOATING_POINT_REGEX = Pattern.compile("[0-9]*\\.[0-9]+");
    private static final Pattern PERCENTAGE_REGEX = Pattern.compile("[0-9]*\\.?[0-9]+ ?(?:%|percent|pc)");

    private static final List<Pattern> TYPE_REGEXES = Lists.newArrayList(
            TEXT_REGEX,
            INTEGER_REGEX,
            PURE_FLOATING_POINT_REGEX,
            PERCENTAGE_REGEX
    );

    public static boolean areLinesHomogeneous(String value) {
        return areLinesHomogeneous(value.split("\n"));
    }

    public static boolean areLinesHomogeneous(String[] lines) {
        return Arrays.stream(lines)
                .map(LineConsistencyChecker::getMatchedRegexes)
                .collect(Collectors.toSet())
                .size() == 1;
    }

    private static Set<Pattern> getMatchedRegexes(String line) {
        return TYPE_REGEXES.stream()
                .filter(pattern -> pattern.matcher(line).find())
                .collect(Collectors.toSet());
    }

    // prints all the values with lines of homogeneous types
    public static void main(String[] args) throws IOException {
        JSONRefParser parser = new JSONRefParser(Props.loadProperties());
        AttributeValueCollection<AnnotatedAttributeValuePair> aavps = parser.getAttributeValuePairs();
        int total = 0;
        for (AnnotatedAttributeValuePair aavp : aavps.getAllPairs()) {
            if (aavp.getValue().split("\n").length > 1) {
                if (LineConsistencyChecker.areLinesHomogeneous(aavp.getValue())) {
                    total++;
                    System.out.println("OK:");
                    for (String line : aavp.getValue().split("\n")) {
                        System.out.println("\t" + line);
                    }
                }
            }
        }
        System.out.println(total);
    }
}
