package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.Lists;
import com.google.common.collect.Streams;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * When it is clear that the value of an AttributeValuePair is incomplete (missing characters), this cleaner
 * tries to automatically complete it from the context.
 *
 * @author marting
 */
public class ValueCompletionCleaner implements Cleaner {

    private static final String ALPHA = "a-zA-Z\\-";
    private static final String DIGITS = "0-9";

    // "absurd number" testing
    private static final double MIN_BIG_NUMBER = 500;
    private static final double MAX_BIG_NUMBER = 500000;
    private static final int MAX_SIGNIFICANT_DIGITS_FOR_BIG_NUMBER = 2;

    private static final List<String> characterClasses = Lists.newArrayList(
            ALPHA, // we complete words (most of the cases)
            DIGITS // we complete pure digits
    );

    @Override
    public List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original) {
        return original.stream()
                .map(avp -> shouldValueBeCompleted(avp) ? clean(avp) : avp)
                .collect(Collectors.toList());
    }

    private boolean shouldValueBeCompleted(AnnotatedAttributeValuePair avp) {
        if (avp.getValue().isEmpty())
            return false;
        // check that the first and last character can potentially indicate we need to complete the value
        String value = avp.getValue();
        String context = avp.getContext();
        Optional<String> regexFirst = getCharacterClassAt(value, 0);
        Optional<String> regexLast = getCharacterClassAt(value, value.length() - 1);
        if (!regexFirst.isPresent() && !regexLast.isPresent()) // nothing to correct
            return false;
        // look for the exact value (i.e. not surrounded by compatible characters)
        String valueRegex = ParsingUtils.escapeRegex(value.trim());
        String regex = valueRegex;
        if (regexFirst.isPresent())
            // an incompatible character OR at the start of the string
            regex = "([^" + regexFirst.get() + "]|^)" + regex;
        if (regexLast.isPresent())
            // an incompatible character OR at the end of the string
            regex = regex + "([^" + regexLast.get() + "]|$)";
        // the value should be completed if
        // 1) it's found in the context (duh)
        // 2) it's not already complete
        boolean canBeExtended = !Pattern.compile(regex).matcher(avp.getContext()).find();
        // 3) if it would be completed to a number, the number doesn't have multiple decimal points or have an absurdly high value
        boolean notAPotentialAbsurdNumber = willNotBeCompletedToAbsurdNumber(valueRegex, avp.getContext());
        return context.contains(value)
                && canBeExtended
                && notAPotentialAbsurdNumber;
    }

    private AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair avp) {
        // find the value in the text (we know it's there)
        String context = avp.getContext();
        int start = context.indexOf(avp.getValue());
        int end = start + avp.getValue().length() - 1; // index of the last character
        // extend the value to the left
        int newStart = getExtendedStart(context, start, -1);
        // extend the value to the right
        int newEnd = getExtendedStart(context, end, +1);
        // check if the value has changed, return the original otherwise
        if (newStart == start && newEnd == end)
            return avp;
        // get the new value
        String value = context.substring(newStart, newEnd + 1);
        return avp.withValue(value);
    }

    private int getExtendedStart(String context, int start, int increment) {
        // get the class of characters of the first/last character in the value
        Optional<String> characterClass = getCharacterClassAt(context, start);
        if (!characterClass.isPresent()) // this is non-numeric non-alpha character, the original value boundary is correct
            return start;
        // take the first character outside the value, in the context
        int index = start + increment;
        while (index >= 0 && index < context.length()
                && getPattern(characterClass.get()).matcher(String.valueOf(context.charAt(index))).find()) {
            index += increment;
        }
        // re-establish the last valid boundary:
        index -= increment;
        return index;
    }

    private boolean willNotBeCompletedToAbsurdNumber(String valueRegex, String context) {
        // Search for a decimal number-like pattern including the value
        String decimalNumberLikePattern = "[" + DIGITS + ".]*"; // greedy match (we want as many of these as possible)
        String regex = decimalNumberLikePattern + valueRegex + decimalNumberLikePattern;
        // search the context for that decimal-like number
        Matcher matcher = Pattern.compile(regex).matcher(context);
        if (matcher.find()) {
            // we check that the number would make sense
            String potentialNumber = matcher.group();
            try {
                double parsedNumber = Math.abs(Double.parseDouble(potentialNumber));
                // the number has to either be small enough, or big but with a reasonable amount of significant digits
                return parsedNumber < MIN_BIG_NUMBER
                        || (parsedNumber < MAX_BIG_NUMBER && numberOfDigitsAfterDecimalPoint(potentialNumber) <= MAX_SIGNIFICANT_DIGITS_FOR_BIG_NUMBER);
            } catch (NumberFormatException e) {
                // this wasn't even a valid number (likely multiple decimal points), it would have been completed to something terrible
                return false;
            }
        } else {
            // if this hasn't been found, no worries on that test
            return true;
        }
    }

    private int numberOfDigitsAfterDecimalPoint(String numberString) {
        int index = numberString.lastIndexOf(".");
        if (index < 0) {
            // not even a decimal point
            return 0;
        } else {
            return numberString.length() - index - 1;
        }
    }

    private Optional<String> getCharacterClassAt(String text, int index) {
        String c = String.valueOf(text.charAt(index));
        return characterClasses.stream()
                .filter(regex -> getPattern(regex).matcher(c).find())
                .findFirst();
    }

    private Pattern getPattern(String characterClass) {
        return Pattern.compile("[" + characterClass + "]");
    }

    /** Prints all the completed values and their original form */
    public static void main(String[] args) throws IOException {
        JSONRefParser parser = new JSONRefParser(Props.loadProperties());
        ValueCompletionCleaner cleaner = new ValueCompletionCleaner();
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = parser.getAttributeValuePairs();
        AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaner.getCleaned(avps);
        Set<AnnotatedAttributeValuePair> delta = Cleaner.delta(avps, cleaned);
        for (AnnotatedAttributeValuePair dirtyAvp : delta) {
            System.out.println("==Original: " + dirtyAvp.getValue());
            System.out.println("==Cleaned: " + cleaner.clean(dirtyAvp).getValue());
        }
        System.out.println(delta.size());
    }
}
