package com.ibm.drl.hbcp.extraction.indexing;

import java.io.IOException;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.store.Directory;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.inforetrieval.indexer.ParagraphBuilder;
import com.ibm.drl.hbcp.inforetrieval.indexer.SentenceBasedParagraphBuilder;

/**
 * An index manager (that can be shared across multiple LuceneQueryExtractors), for a sentence-based indexing
 * of paragraphs in a document.
 *
 * @author marting
 */
public class SentenceBasedIndexManager extends IndexManager {

    private final int paraNumberOfSentences;

    public SentenceBasedIndexManager(Directory directory, int paraNumberOfSentences, Analyzer paragraphAnalyzer) throws IOException {
        super(directory, buildParagraphBuilders(paraNumberOfSentences, paragraphAnalyzer));
        this.paraNumberOfSentences = paraNumberOfSentences;
    }

    /**
     * Sentence-based paragraph builders
     */
    public static List<ParagraphBuilder> buildParagraphBuilders(int paraNumberOfSentences, Analyzer paragraphAnalyzer) {
        return Lists.newArrayList(
                new SentenceBasedParagraphBuilder(paraNumberOfSentences, paragraphAnalyzer)
        );
    }
}