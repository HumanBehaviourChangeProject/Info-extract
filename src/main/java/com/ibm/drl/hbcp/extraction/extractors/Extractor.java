package com.ibm.drl.hbcp.extraction.extractors;

import com.ibm.drl.hbcp.extraction.candidates.Candidate;

import java.io.IOException;
import java.util.Collection;

/**
 * Defines an extractor: a function that outputs candidate answers from a text.
 * @param <Document> Type of document (usually something from which text can be read)
 * @param <Answer> Type of elements it extracts from the document
 *
 * @author marting
 */
public interface Extractor<Document, Answer, CandidateAnswer extends Candidate<Answer>> {

    /**
     * Extract scored candidates from a document.
     * @param doc document
     * @return collection of scored candidates found in the text
     */
    Collection<CandidateAnswer> extract(Document doc) throws IOException;
}
