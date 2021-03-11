package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Map;

import org.apache.commons.collections4.map.LRUMap;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.extraction.answerselectors.AcrossPassagesAggregator;
import com.ibm.drl.hbcp.extraction.answerselectors.AnswerSelector;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.EqualityEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.inforetrieval.normrsv.NormalizedRSVRetriever;

/**
 * An extractor based on a query to a Lucene index.
 * 1) Queries the Lucene index for relevant passages
 * 2) Extract answers from each passage
 * 3) Performs some score aggregation for each answer
 * @param <E> type of answer returned by the extractor
 *
 * @author marting
 */
public abstract class LuceneQueryExtractor<E> implements EvaluatedExtractor<IndexedDocument, E, CandidateInPassage<E>> {

    protected final String contentFieldName;
    private final int numberOfTopPassages;
    protected final String query;
    private final IndexingMethod indexingMethod;
    private final Map<IndexManager, Query> queryCache;
    private final AnswerSelector<E, CandidateInPassage<E>> answerSelector;

    protected final Logger LOGGER = LoggerFactory.getLogger(this.getClass());

    protected LuceneQueryExtractor(String contentFieldName, IndexingMethod indexingMethod,
                                   int numberOfTopPassages,
                                   String query,
                                   AnswerSelector<E, CandidateInPassage<E>> answerSelector) {
        this.contentFieldName = contentFieldName;
        this.numberOfTopPassages = numberOfTopPassages;
        this.indexingMethod = indexingMethod;
        this.query = query;
        queryCache = new LRUMap<>(10);
        this.answerSelector = answerSelector;
    }

    protected LuceneQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages,
                                   String query,
                                   AnswerSelector<E, CandidateInPassage<E>> answerSelector) {
        this(ResearchDoc.FIELD_CONTENT, indexingMethod, numberOfTopPassages, query, answerSelector);
    }

    protected LuceneQueryExtractor(IndexingMethod indexingMethod, int numberOfTopPassages, String query) {
        this(indexingMethod, numberOfTopPassages, query,
                // default answer selection strategy: aggregate across passages, then select the answer with the best score
                getDefaultAnswerSelector());
    }

    /**
     * Extract scored candidates from a passage. A passage is the result of a query to an index, and as such is a piece
     * of text with a score. Each candidate returned can be assigned a score relatively to the passage and the query, in
     * addition to a final aggregation score (that can be left as 1.0 by default, aggregation will be done by the extract
     * method on the whole document).
     * @param passage A piece of text with a score
     * @return A collection of candidate answers with various scores
     */
    public abstract Collection<CandidateInPassage<E>> extract(PassageInIndex passage);

    @Override
    public Collection<CandidateInPassage<E>> extract(IndexedDocument doc) throws IOException {
        IndexSearcher searcher = doc.getIndexManager().get(doc.getDocId()); // if docId is invalid, this will throw an IOException
        String docName = doc.getDocName();

        // retrieve the top passages for the query
        TopDocs topDocs = searcher.search(getQuery(doc.getIndexManager()), numberOfTopPassages);  // for classification flow retrieve just the top-most
        if (topDocs.scoreDocs.length == 0) {
            //logger.info("No information found in document " + docName + "(" + doc.getDocId() + ") for query " + query);
            return new ArrayList<>();
        }
        // Normalize the retrieval model scores to ensure that we don't need to
        // compute cosine similarity inside the construct() function
        // of AbstractDetectPresenceAttribute.
        NormalizedRSVRetriever normalizer = new NormalizedRSVRetriever(searcher, ResearchDoc.FIELD_CONTENT);
        topDocs = normalizer.rerank(topDocs);

        List<CandidateInPassage<E>> res = new ArrayList<>();
        for (int i = 0; i < topDocs.scoreDocs.length; i++) {
            ScoreDoc scoreDoc = topDocs.scoreDocs[i];
            String content = searcher.doc(scoreDoc.doc).get(ResearchDoc.FIELD_CONTENT);
            PassageInIndex passage = new PassageInIndex(content, docName, scoreDoc.score, doc.getIndexManager());
            Collection<CandidateInPassage<E>> resultsForPassage = extract(passage);
            res.addAll(resultsForPassage);
        }
        // apply the answer selection strategy
        return answerSelector.select(res);
    }

    /** Queries depend on the analyzer, which is provided by IndexManagers. As a LuceneQueryExtractor can be used
     * with potentially several IndexManager, we cache the Query objects so that they're correctly constructed but
     * hopefully not for each new document */
    private Query getQuery(IndexManager indexManager) throws IOException {
        Query res = queryCache.get(indexManager);
        if (res == null) {
            try {
                res = constructQuery(query, indexManager.getAnalyzer());
                queryCache.put(indexManager, res);
            } catch (ParseException e) {
                // we rethrow the ParseException as an IOException (which is the only exception extract throws)
                throw new IOException("Query " + query + " couldn't be parsed", e);
            }
        }
        return res;
    }

    protected static <E> AnswerSelector<E, CandidateInPassage<E>> getDefaultAnswerSelector() {
        return new AcrossPassagesAggregator<>();
        // only useful in the unarmified setup
        //return new BestCandidateInPassageAggregator<E>().then(new BestAnswerSelector<>());
    }

    private Query constructQuery(String query, Analyzer analyzer) throws ParseException {
        // build the query on the content
        QueryParser parser = new QueryParser(contentFieldName, analyzer);
        Query baseQuery = parser.parse(query);
        BooleanQuery.Builder res = new BooleanQuery.Builder();
        res.add(baseQuery, BooleanClause.Occur.MUST);
        if (indexingMethod != IndexingMethod.NONE) {
            // window size constraint (or sentence-based)
            TermQuery windowSizeConstraint = new TermQuery(new Term(ResearchDoc.INDEXING_METHOD, indexingMethod.toString()));
            // TODO: might be interesting to try SHOULD, might lead to better results overall
            res.add(windowSizeConstraint, BooleanClause.Occur.MUST);
        }
        return res.build();
    }

    protected Query constructQuery(String query) throws ParseException {
        return constructQuery(query, IndexManager.DEFAULT_ANALYZER);
    }

    /** The query (parsable by Lucene's QueryParser) */
    public String getQuery() { return query; }

    @Override
    public List<Evaluator<IndexedDocument, E>> getEvaluators() {
        return Lists.newArrayList(
                // simple equality test on single values by default, but shouldn't really be used in concrete extractors
                new EqualityEvaluator<>()
        );
    }
}
