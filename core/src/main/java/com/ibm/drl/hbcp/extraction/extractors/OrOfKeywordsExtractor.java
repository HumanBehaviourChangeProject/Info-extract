package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.util.ParsingUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;

import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

public abstract class OrOfKeywordsExtractor<T> extends RegexQueryExtractor<T> {

    protected OrOfKeywordsExtractor(IndexingMethod indexingMethod, int numberOfTopPassages, List<String> queryTerms, boolean keepOriginalToken) throws ParseException {
        super(indexingMethod, numberOfTopPassages, makeOrQueryString(queryTerms), getRegexes(queryTerms), keepOriginalToken);
    }

    /**
     * Very simplified version of the extract of {@link TokenMatchingQueryExtractor}. No verification on tokens,
     * scoring function and whatnot,
     * just uses regex to extract potential candidates in a binary way.
     */
    @Override
    public Collection<CandidateInPassage<T>> extract(PassageInIndex passage) {
        String preProcessedWindow = preProcess(passage.getText());
        Set<String> matches = getAllMatchingCandidates(preProcessedWindow);

        return matches.stream()
                .map(match -> newCandidate(match, 1.0, passage))
                .collect(Collectors.toList());
    }

    private static List<Pattern> getRegexes(List<String> queryTerms) {
        return queryTerms.stream()
                .map(word -> ParsingUtils.escapeRegex(word))
                .map(word -> "(" + word + ")")
                .map(word -> Pattern.compile(word))
                .collect(Collectors.toList());
    }

    private static String makeOrQueryString(List<String> words) {
        List<String> quoted = words.stream()
                .map(w -> w.contains(" ") ? "\"" + w + "\"" : w)
                .collect(Collectors.toList());
        return StringUtils.join(quoted, " OR ");
    }
}
