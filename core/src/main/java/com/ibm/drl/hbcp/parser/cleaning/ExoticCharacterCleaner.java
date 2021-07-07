package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Attempts to get rid of weird exotic characters introduced during annotation,
 * mostly ligatures like "ﬁ" instead of the 2 characters "fi"
 *
 * @author mgleize
 */
public class ExoticCharacterCleaner implements Cleaner {

    private static final List<Pair<Character, String>> CHAR_REPLACEMENTS = Lists.newArrayList(
            Pair.of('ﬁ', "fi"),
            Pair.of('ﬂ', "fl"),
            Pair.of('ﬄ', "ffl"),
            Pair.of('ﬃ', "ffi"),
            Pair.of('ﬀ', "ff")
    );

    private static final Set<Character> EXOTIC_CHARS = CHAR_REPLACEMENTS.stream()
            .map(Pair::getKey).collect(Collectors.toSet());

    @Override
    public List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original) {
        return original.stream()
                .map(this::clean)
                .collect(Collectors.toList());
    }

    private AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair original) {
        String cleanedValue = clean(original.getValue());
        String cleanedContext = clean(original.getContext());
        String cleanedHighlightedText = clean(original.getHighlightedText());
        AnnotatedAttributeValuePair newAvp = new AnnotatedAttributeValuePair(original.getAttribute(),
                cleanedValue, original.getDocName(), original.getArm(),
                cleanedContext,
                cleanedHighlightedText, original.getSprintNo(), original.getAnnotationPage()
        );
        // return it only if it has changed compared to the original
        return newAvp.equals(original) ? original : newAvp;
    }

    private String clean(String s) {
        for (Pair<Character, String> charAndReplacement : CHAR_REPLACEMENTS) {
            s = StringUtils.replace(s, charAndReplacement.getKey().toString(), charAndReplacement.getValue());
        }
        return s;
    }
}
