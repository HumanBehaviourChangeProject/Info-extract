package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.extraction.answerselectors.AnswerSelector;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import org.apache.lucene.queryparser.classic.ParseException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * With this extractor, valid token candidates are extracted from the passages using regexes.
 * @param <T> type of candidate answers
 *
 * @author marting
 */
public abstract class RegexQueryExtractor<T> extends TokenMatchingQueryExtractor<T> {

    protected final List<Pattern> regexes;

    protected RegexQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages,
                                  String query, List<Pattern> regexes) throws ParseException {
        this(indexingMethod, numberOfTopPassages, query, regexes, false);
    }
    
    protected RegexQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages,
                                  String query, List<Pattern> regexes, boolean keepOriginalToken) throws ParseException {
        this(indexingMethod, numberOfTopPassages, query, regexes, keepOriginalToken, LuceneQueryExtractor.getDefaultAnswerSelector());
    }

    protected RegexQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages,
                                  String query, List<Pattern> regexes, boolean keepOriginalToken,
                                  AnswerSelector<T, CandidateInPassage<T>> answerSelector) throws ParseException {
        super(indexingMethod, numberOfTopPassages, query, keepOriginalToken, answerSelector);
        this.regexes = regexes;
    }


    /**
     * Returns all the candidates matching the regexes. Formerly included in "preProcess".
     * @param window the passage on which this extractor operates
     */
    @Override
    protected Set<String> getAllMatchingCandidates(String window) {
        Set<String> res = new HashSet<>();
        for (Pattern p : regexes) {
            Matcher m = p.matcher(window);
            while (m.find()) {
                res.addAll(getValidMatches(m));
            }
        }
        return res;
    }

    protected Set<String> getValidMatches(Matcher matcher) {
        return Sets.newHashSet(matcher.group(1)).stream()
                .map(this::postProcess)
                .collect(Collectors.toSet());
    }

    /** Regexes used to extract valid candidates from each passage */
    public List<Pattern> getRegexes() { return regexes; }
}
