package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.util.ParsingUtils;
import lombok.Data;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import static com.ibm.drl.hbcp.parser.cleaning.TableValueFinder.escapeRegex;

/**
 * Normalizes time points and durations to a number of weeks.
 *
 * @author mgleize
 */
public class TimepointNormalizer implements Normalizer<ArmifiedAttributeValuePair> {

    private enum Unit { NONE, WEEK, MONTH }
    private final List<Pair<Pattern, Unit>> regexToUnits = Lists.newArrayList(
            Pair.of(Pattern.compile("week"), Unit.WEEK),
            Pair.of(Pattern.compile("month"), Unit.MONTH),
            Pair.of(Pattern.compile("mo(?: |$)"), Unit.MONTH)
    );
    private static final String[] DIGITS_IN_WORDS = { "zero", "one", "two", "three", "four", "five",
            "six", "seven", "eight", "nine", "ten", "eleven", "twelve"
    };
    private static final int WEEK_THRESHOLD = 12;  //higher than this ==> classify as weeks
    public static final double WEEKS_IN_A_MONTH = 4.35;

    @Override
    public String getNormalizedValue(ArmifiedAttributeValuePair pair) {
        String value = pair.getValue().toLowerCase();
        String context = pair.getContext().toLowerCase();
        Optional<Double> numericValue = getNumericValue(value);
        if (numericValue.isPresent()) {
            List<String> searchedForUnits = Lists.newArrayList(value, context);
            Unit unit = Unit.NONE;
            for (String searchedForUnit : searchedForUnits) {
                List<Integer> valuePositions = findValuePositions(value, searchedForUnit);
                List<Pair<Unit, Integer>> unitPositions = allDetectedUnitsWithPositions(searchedForUnit);
                // sort the distances between values and units, take the closest pairs
                List<UnitValuePositionPair> unitValuePairs = getClosestUnitValuePositionPairs(valuePositions, unitPositions);
                unit = getUnit(unitValuePairs);
                // if the unit is found in value, don't look for it in context
                if (unit != Unit.NONE) break;
            }
            // fallback if we haven't found the unit
            if (unit == Unit.NONE) {
                unit = numericValue.get() >= WEEK_THRESHOLD ? Unit.WEEK : Unit.MONTH;
            }
            // we have a unit, build the normalized value
            double valueInWeeks = getValueInWeeks(numericValue.get(), unit);
            return String.valueOf(valueInWeeks);
        } else return pair.getValue(); // if nothing has been found, return the same value
    }

    private Optional<Double> getNumericValue(String value) {
        List<Double> doubles = ParsingUtils.parseAllDoubles(value);
        if (!doubles.isEmpty()) {
            return Optional.of(doubles.get(0));
        } else {
            // look at numbers written in letters
            for (int i = 0; i < DIGITS_IN_WORDS.length; i++) {
                if (value.contains(DIGITS_IN_WORDS[i]))
                    return Optional.of((double)i);
            }
            // we haven't found any recognizable number
            return Optional.empty();
        }
    }

    private List<Integer> findValuePositions(String value, String context) {
        List<Integer> res = new ArrayList<>();
        Pattern valueRegex = Pattern.compile("(?:^|[^0-9a-zA-Z])" + "(" + escapeRegex(value) + ")" + "(?:[^0-9a-zA-Z]|$)");
        Matcher matcher = valueRegex.matcher(context);
        while (matcher.find()) {
            // group 1 is the value itself (the surrounding characters are excluded)
            res.add(matcher.start(1));
        }
        return res;
    }

    private List<Pair<Unit, Integer>> allDetectedUnitsWithPositions(String context) {
        List<Pair<Unit, Integer>> res = new ArrayList<>();
        for (Pair<Pattern, Unit> regexToUnit : regexToUnits) {
            Matcher matcher = regexToUnit.getKey().matcher(context);
            while (matcher.find()) {
                res.add(Pair.of(regexToUnit.getValue(), matcher.start()));
            }
        }
        return res;
    }

    private List<UnitValuePositionPair> getClosestUnitValuePositionPairs(List<Integer> valuePositions, List<Pair<Unit, Integer>> unitPositions) {
        List<UnitValuePositionPair> allPairs = allUnitPositionValuePositionPairs(valuePositions, unitPositions);
        // sort
        List<UnitValuePositionPair> sorted = allPairs.stream()
                .sorted(Comparator.comparing(UnitValuePositionPair::distance))
                .collect(Collectors.toList());
        if (sorted.isEmpty()) {
            return sorted;
        } else {
            // return all things that have the same distance as the first
            int firstDistance = sorted.get(0).distance();
            return sorted.stream().filter(pair -> pair.distance() == firstDistance).collect(Collectors.toList());
        }
    }

    private List<UnitValuePositionPair> allUnitPositionValuePositionPairs(List<Integer> valuePositions, List<Pair<Unit, Integer>> unitPositions) {
        List<UnitValuePositionPair> res = new ArrayList<>();
        // we add an infinite position because the value might not be found in the context (but a valid unit might)
        List<Integer> valuePositionsPlusInfinite = new ArrayList<>(valuePositions);
        valuePositionsPlusInfinite.add(Integer.MAX_VALUE);
        for (int valuePosition : valuePositionsPlusInfinite) {
            for (Pair<Unit, Integer> unitPosition : unitPositions) {
                res.add(new UnitValuePositionPair(valuePosition, unitPosition.getRight(), unitPosition.getLeft()));
            }
        }
        return res;
    }

    private Unit getUnit(List<UnitValuePositionPair> unitValuePairs) {
        // prioritize month over week
        if (unitValuePairs.stream().anyMatch(pair -> pair.getUnit() == Unit.MONTH)) {
            return Unit.MONTH;
        } else if (unitValuePairs.stream().anyMatch(pair -> pair.getUnit() == Unit.WEEK)) {
            return Unit.WEEK;
        } else {
            return Unit.NONE;
        }
    }

    private double getValueInWeeks(double value, Unit unit) {
        // convert months to weeks
        if (unit == Unit.MONTH) {
            return value * WEEKS_IN_A_MONTH;
        } else if (unit == Unit.WEEK){
            return value;
        } else {
            throw new AssertionError("This method shouldn't be used if the unit is NONE (hasn't been determined)");
        }
    }

    @Data
    private static class UnitValuePositionPair {
        private final int valuePosition;
        private final int unitPosition;
        private final Unit unit;

        public int distance() {
            return Math.abs(valuePosition - unitPosition);
        }
    }

}
