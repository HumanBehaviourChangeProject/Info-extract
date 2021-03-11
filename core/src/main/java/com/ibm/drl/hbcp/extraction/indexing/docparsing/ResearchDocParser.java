package com.ibm.drl.hbcp.extraction.indexing.docparsing;

import com.ibm.drl.hbcp.extraction.indexing.DocumentInputStream;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;

import java.io.IOException;

public interface ResearchDocParser {

    ResearchDoc parse(DocumentInputStream documentInputStream) throws IOException;
}
