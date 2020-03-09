package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.extraction.answerselectors.AnswerSelector;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;
import java.util.stream.Collectors;

/**
 * An extractor extracting candidate answers from individual tokens in passages
 * retrieved from a Lucene index. The answers are assigned a score based on
 * their average closeness to the query terms.
 *
 * @param <E> type of answer
 *
 * @author marting
 */
public abstract class TokenMatchingQueryExtractor<E> extends LuceneQueryExtractor<E> {

    protected final String[] queryTerms;
    private final boolean keepUnAnalyzedToken;

    private static final Logger logger = LoggerFactory.getLogger(TokenMatchingQueryExtractor.class);

    protected TokenMatchingQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages,
            String query) throws ParseException {
        this(indexingMethod, numberOfTopPassages, query, false);
    }

    protected TokenMatchingQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages,
                                          String query, boolean keepOriginalToken) throws ParseException {
        this(indexingMethod, numberOfTopPassages, query, keepOriginalToken, LuceneQueryExtractor.getDefaultAnswerSelector());
    }

    protected TokenMatchingQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages,
                                          String query, boolean keepOriginalToken, AnswerSelector<E, CandidateInPassage<E>> answerSelector) throws ParseException {
        super(indexingMethod, numberOfTopPassages, query, answerSelector);
        queryTerms = extractQueryTerms(constructQuery(query));
        keepUnAnalyzedToken = keepOriginalToken;
    }

    /**
     * Extracts all the potential candidates directly from the string window
     */
    protected abstract Set<String> getAllMatchingCandidates(String window);

    /**
     * Creates a new candidate. This method will usually be a wrapper around a
     * constructor of the desired type of the answers. For example, a candidate
     * holding an ArmifiedAttributeValuePair.
     */
    protected abstract CandidateInPassage<E> newCandidate(String value, double score, Passage passage);

    @Override
    public Collection<CandidateInPassage<E>> extract(PassageInIndex passage) {
        String preProcessedWindow = preProcess(passage.getText());
        Set<String> matches = getAllMatchingCandidates(preProcessedWindow);

        String[] tokens = tokenize(preProcessedWindow);

        List<Pair<String, Integer>> candidatesWithPosition = new ArrayList<>();
        List<Pair<String, Integer>> queryTermsWithPosition = new ArrayList<>();

        for (int i = 0; i < tokens.length; i++) {
            int position = i; // forced to use an extra variable for the lambda
            String rawToken = tokens[i];
            Set<String> tokenVersions = Sets.newHashSet(rawToken);
            if (!keepUnAnalyzedToken) {
                String analyzedToken = PaperIndexer.analyze(passage.getIndexManager().getAnalyzer(), rawToken);
                if (analyzedToken == null) {
                    logger.error("Could not analyze token " + rawToken);
                } else {
                    tokenVersions = Sets.newHashSet(analyzedToken); // I wanted to test with "add" but I think it would deteriorate some extractors
                }
            }

            List<String> candidates = new ArrayList<>();
            for (String token : tokenVersions) {
                if (isValidCandidate(token)) {
                    candidates = getValidCandidates(token, matches);
                }
            }

            List<Pair<String, Integer>> withPosition = candidates.stream().map(c -> Pair.of(c, position)).collect(Collectors.toList());
            candidatesWithPosition.addAll(withPosition);

            // see if that token is a query term and set the position
            for (String token : tokenVersions) {
                int index = Arrays.binarySearch(this.queryTerms, token);
                if (index > -1) {
                    queryTermsWithPosition.add(Pair.of(token, position)); // i is the position of the query term in 'window'.
                }
            }
        }

        List<Pair<String, Double>> res = getScoredCandidates(candidatesWithPosition, queryTermsWithPosition);
        return res.stream()
                .map(candidateWithScore -> newCandidate(candidateWithScore.getLeft(), candidateWithScore.getRight(), passage))
                .collect(Collectors.toList());
    }

    /**
     * Applies preprocessing on the entire passage
     */
    protected String preProcess(String window) {
        return window;
    }

    /**
     * Applies filters on the raw (not postprocessed) token
     */
    protected boolean isValidCandidate(String token) {
        return true;
    }

    /**
     * Applies postprocessing on a single token
     */
    protected String postProcess(String token) {
        return token;
    }

    /**
     * Extracts all the valid final string values from a single token, given all
     * the pattern matches in the window
     */
    protected List<String> getValidCandidates(String token, Set<String> patternMatches) {
        String postProcessedToken = postProcess(token);
        if (patternMatches.contains(postProcessedToken)) {
            return Lists.newArrayList(postProcessedToken);
        } else {
            return new ArrayList<>();
        }
    }

    protected List<Pair<String, Double>> getScoredCandidates(List<Pair<String, Integer>> candidatesWithPosition,
            List<Pair<String, Integer>> queryTermsWithPosition) {
        return candidatesWithPosition.stream()
                .map(candidateWithPosition -> {
                    double res = 0.0;
                    for (Pair<String, Integer> queryTermWithPosition : queryTermsWithPosition) {
                        res += similarity(candidateWithPosition, queryTermWithPosition);
                    }
                    return Pair.of(candidateWithPosition.getLeft(), res);
                })
                .collect(Collectors.toList());
    }

    protected double similarity(Pair<String, Integer> candidateWithPosition, Pair<String, Integer> queryTermWithPosition) {
        double distance = candidateWithPosition.getRight() - queryTermWithPosition.getRight();
        return Math.exp(-1 * distance * distance);
    }

    protected static String[] tokenize(String window) {
        return window.split("[.,;:]*\\s+");
    }

    private String[] extractQueryTerms(Query q) {
        WeightedTerm[] wqueryTerms = QueryTermExtractor.getTerms(q, false, contentFieldName);

        String[] queryTerms = new String[wqueryTerms.length];

        for (int i = 0; i < wqueryTerms.length; i++) {
            queryTerms[i] = wqueryTerms[i].getTerm();
        }
        Arrays.sort(queryTerms);
        return queryTerms;
    }
}
