package com.ibm.drl.hbcp.inforetrieval.indexer;

import java.util.Properties;

public interface IndexingMethod {
    // mostly interesting for its toString method

    /** No special indication of indexing, tells LuceneQueryExtractor to ignore this constraint */
    IndexingMethod NONE = new IndexingMethod() {
        @Override
        public String toString() {
            return "";
        }
    };

    /** Specific sliding-window method for a window size (can be by-passed through a flag in the properties) */
    static IndexingMethod slidingWindow(int windowSize, Properties props) {
        if (Boolean.parseBoolean(props.getProperty("extract.one.indexing.method.per.extractor"))) {
            return new SlidingWindowParagraphBuilder.SlidingWindowMethod(windowSize);
        } else {
            return NONE;
        }
    }
}
