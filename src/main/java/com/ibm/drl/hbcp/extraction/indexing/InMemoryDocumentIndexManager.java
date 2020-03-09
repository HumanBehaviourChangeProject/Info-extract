package com.ibm.drl.hbcp.extraction.indexing;

import com.ibm.drl.hbcp.inforetrieval.indexer.ParagraphBuilder;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.apache.tika.exception.TikaException;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public class InMemoryDocumentIndexManager extends IndexManager {

    protected InMemoryDocumentIndexManager(List<DocumentInputStream> documents, List<ParagraphBuilder> paragraphBuilders) throws IOException, TikaException, SAXException {
        super(getInMemoryDirectory(documents, DEFAULT_ANALYZER), paragraphBuilders);
    }

    /**
     * Used by the API to extract information from any document
     * uploaded from the web interface (or any document as a file on-disk)
     *
     * @return A pointer to an in-memory Lucene index (a 'Directory' object).
     */
    private static Directory getInMemoryDirectory(List<DocumentInputStream> documents, Analyzer analyzer) throws IOException, TikaException, SAXException {
        // first filter out all the non-PDF files
        documents = documents.stream()
                // TODO: we don't filter here anymore because the document might be stripped of its name (e.g. at upload time in a web form)
                //.filter(doc -> FilenameUtils.getExtension(doc.getDocName()).equalsIgnoreCase("pdf"))
                .collect(Collectors.toList());

        Directory ramdir = new RAMDirectory();
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);

        int docId = 0;
        for (DocumentInputStream document : documents) {
            ResearchDoc researchDoc = new ResearchDoc(document.getInputStream());
            researchDoc.extractInfoFromDOM();

            Document doc = researchDoc.constructDoc(docId++, document.getDocName());
            writer.addDocument(doc);
        }

        writer.commit();
        writer.close();
        return writer.getDirectory();
    }
}
