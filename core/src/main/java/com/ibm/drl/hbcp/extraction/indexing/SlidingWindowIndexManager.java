package com.ibm.drl.hbcp.extraction.indexing;

import com.ibm.drl.hbcp.inforetrieval.indexer.ParagraphBuilder;
import com.ibm.drl.hbcp.inforetrieval.indexer.SlidingWindowParagraphBuilder;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.store.Directory;

import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An index manager (that can be shared across multiple LuceneQueryExtractors), for a sliding window-based indexing
 * of paragraphs in a document.
 *
 * @author marting
 */
public class SlidingWindowIndexManager extends IndexManager {

    public SlidingWindowIndexManager(Directory directory, String[] windowSizes, Analyzer paragraphAnalyzer) throws IOException {
        super(directory, buildParagraphBuilders(windowSizes, paragraphAnalyzer));
    }

    /** Sliding-window paragraph builders for all the specified window sizes */
    public static List<ParagraphBuilder> buildParagraphBuilders(String[] windowSizes, Analyzer paragraphAnalyzer) {
        return Arrays.stream(windowSizes)
                .map(Integer::parseInt)
                .map(windowSize -> new SlidingWindowParagraphBuilder(windowSize, paragraphAnalyzer))
                .collect(Collectors.toList());
    }

    /** Default sliding-window paragraph builder for all a specified window size of 20 and a whitespace analyzer */
    public static List<ParagraphBuilder> buildParagraphBuilders() {
        return buildParagraphBuilders(new String[] {"50", "20"}, new WhitespaceAnalyzer());
    }

    /** Default sliding-window paragraph builder for all a specified window size of 20 and a whitespace analyzer */
    public static List<ParagraphBuilder> buildParagraphBuilders(String[] windowSizes) {
        return buildParagraphBuilders(windowSizes, new WhitespaceAnalyzer());
    }
}
