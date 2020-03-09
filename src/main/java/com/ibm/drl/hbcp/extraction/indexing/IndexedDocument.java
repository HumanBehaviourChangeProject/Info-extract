package com.ibm.drl.hbcp.extraction.indexing;

import java.io.IOException;

/**
 * An index (accessed through a manager class) and a docId, representing an individual document in the index.
 * Used as input for LuceneQueryExtractor's extract method.
 *
 * @author marting
 */
public class IndexedDocument {

    private final IndexManager indexManager;
    private final int docId;

    public IndexedDocument(IndexManager indexManager, int docId) {
        this.indexManager = indexManager;
        this.docId = docId;
    }

    public IndexManager getIndexManager() {
        return indexManager;
    }

    public int getDocId() {
        return docId;
    }

    public String getDocName() throws IOException {
        return indexManager.getDocName(getDocId());
    }
}
