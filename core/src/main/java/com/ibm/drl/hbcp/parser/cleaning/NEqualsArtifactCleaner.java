package com.ibm.drl.hbcp.parser.cleaning;

import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;

import java.util.Collection;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Cleans the "n5"-for-"n=" artifacts appearing in contexts due to EPPI encoding. Papers use the "(n=120)" notation a lot
 * to indicate population counts but it gets transformed in the context to "(n5120)", messing up the number.
 *
 * @author mgleize
 */
public class NEqualsArtifactCleaner implements Cleaner {

    private static final String N_EQUALS_ARTIFACT_REGEX = "(?:[^a-zA-Z]|^)" // not a letter, or the start of the string
        + "[nN]5" // n then an "equals" sign that has been replaced by EPPI with a "5"
        + "[0-9]+"; // at least one other digit
    private final Pattern nEqualsArtifactPattern = Pattern.compile(N_EQUALS_ARTIFACT_REGEX);

    @Override
    public List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original) {
        return original.stream()
                .map(this::clean)
                .collect(Collectors.toList());
    }

    private AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair original) {
        String context = cleanString(original.getContext());
        String value = cleanString(original.getValue());
        // only create a new AVP if there was really a change
        if (!(context.equals(original.getContext()) && value.equals(original.getValue()))) {
            AnnotatedAttributeValuePair newAvp = original.withContext(context).withValue(value);
            return newAvp;
        } else {
            return original;
        }
    }

    private String cleanString(String originalString) {
        String res = originalString;
        Matcher matcher = nEqualsArtifactPattern.matcher(originalString);
        while (matcher.find()) {
            String match = matcher.group();
            // replace this pattern in a new string with the "=" instead of a 5
            res = res.replace(match, match.replaceFirst("(?<=[nN])5", "="));
        }
        return res;
    }
}
