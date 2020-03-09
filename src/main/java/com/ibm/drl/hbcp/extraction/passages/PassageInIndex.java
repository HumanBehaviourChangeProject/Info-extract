package com.ibm.drl.hbcp.extraction.passages;

import com.ibm.drl.hbcp.extraction.indexing.IndexManager;

/**
 * A passage coming from a (paragraph) index
 *
 * @author marting
 */
public class PassageInIndex extends SimplePassage {
    private final IndexManager indexManager;

    public PassageInIndex(String text, String docname, double score, IndexManager indexManager) {
        super(text, docname, score);
        this.indexManager = indexManager;
    }

    public IndexManager getIndexManager() { return indexManager; }
}
