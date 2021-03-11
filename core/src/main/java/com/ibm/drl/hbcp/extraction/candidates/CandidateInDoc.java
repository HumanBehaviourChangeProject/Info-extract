package com.ibm.drl.hbcp.extraction.candidates;

import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;

public class CandidateInDoc<T> extends Candidate<T> {
    private final IndexedDocument doc;

    public CandidateInDoc(IndexedDocument doc, T answer) {
        super(answer);
        this.doc = doc;
    }

    @Override // maybe some formula of the 2 scores here
    public double getScore() { return 1.0; }

    public IndexedDocument getDocument() { return doc; }
}