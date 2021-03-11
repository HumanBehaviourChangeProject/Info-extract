package com.ibm.drl.hbcp.inforetrieval.indexer;

import java.io.IOException;
import java.util.List;

public interface ParagraphBuilder {
    /**
     * Constructs a paragraph for a given document identifier and the content
     * @param docId
     * @param content
     * @return
     * @throws IOException
     */
    List<Paragraph> constructParagraphs(int docId, String content) throws IOException;


}
