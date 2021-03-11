package com.ibm.drl.hbcp.extraction.indexing;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.ByteBuffersDirectory;
import org.apache.lucene.store.Directory;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import com.ibm.drl.hbcp.extraction.indexing.docparsing.PdfDocParser;
import com.ibm.drl.hbcp.extraction.indexing.docparsing.ResearchDocParser;
import com.ibm.drl.hbcp.inforetrieval.indexer.ParagraphBuilder;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;

public class InMemoryDocumentIndexManager extends IndexManager {

    protected InMemoryDocumentIndexManager(List<DocumentInputStream> documents,
                                           ResearchDocParser docParser,
                                           List<ParagraphBuilder> paragraphBuilders) throws IOException, TikaException, SAXException {
        super(getInMemoryDirectory(documents, docParser, DEFAULT_ANALYZER), paragraphBuilders);
    }

    /** This default call is for raw PDF input */
    protected InMemoryDocumentIndexManager(List<DocumentInputStream> documents,
                                           List<ParagraphBuilder> paragraphBuilders) throws IOException, TikaException, SAXException {
        this(documents, new PdfDocParser(), paragraphBuilders);
    }

    /**
     * Used by the API to extract information from any document
     * uploaded from the web interface (or any document as a file on-disk)
     *
     * @return A pointer to an in-memory Lucene index (a 'Directory' object).
     */
    private static Directory getInMemoryDirectory(List<DocumentInputStream> documents,
                                                  ResearchDocParser docParser,
                                                  Analyzer analyzer) throws IOException, TikaException, SAXException {
        // first filter out all the non-PDF files
        documents = new ArrayList<>(documents);

        Directory ramdir = new ByteBuffersDirectory();
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);

        int docId = 0;
        for (DocumentInputStream document : documents) {
            ResearchDoc researchDoc = docParser.parse(document);

            Document doc = researchDoc.constructDoc(docId++, document.getDocName());
            writer.addDocument(doc);
        }

        writer.commit();
        writer.close();
        return writer.getDirectory();
    }
}
