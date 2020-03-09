package com.ibm.drl.hbcp.extraction.indexing;

import com.ibm.drl.hbcp.inforetrieval.indexer.*;
import com.ibm.drl.hbcp.util.LuceneField;
import com.ibm.drl.hbcp.util.Props;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Closeable;
import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

/**
 * An index manager (that can be shared across multiple LuceneQueryExtractors).
 * Lucene's notion of "docId" (an integer) is used to identify individual documents (often PDF) in the index.
 * The index is supposed to be built so that the following property is preserved:
 * docIds are contiguous and start from 0 (so N documents have docIds 0, 1,..., N-1).
 * This assumption is used in the implementation of several features across the extraction package.
 *
 * @author marting
 */
public class IndexManager implements Closeable {

    private final Directory directory;
    private final SearcherManager mainSearcher;
    private final Map<Integer, IndexSearcher> paragraphIndices;
    private final Map<Integer, IndexSearcher> pageIndices;
    private final List<ParagraphBuilder> paragraphBuilders;
    protected final Analyzer analyzer;

    private static final Logger logger = LoggerFactory.getLogger(IndexManager.class);

    public static final Analyzer DEFAULT_ANALYZER = getDefaultAnalyzer();

    protected IndexManager(Directory directory, List<ParagraphBuilder> paragraphBuilders) throws IOException {
        this.directory = directory;
        mainSearcher = new SearcherManager(directory, getSearcherFactory());
        paragraphIndices = new HashMap<>();
        pageIndices = new HashMap<>();
        analyzer = DEFAULT_ANALYZER; // right now we only use this analyzer all the time
        this.paragraphBuilders = paragraphBuilders;
    }

    /**
     * Returns a searcher on passages for a given document
     * @param docId the ID of the document
     * @return IndexSearcher for an individual passage index of that document
     * @throws IOException
     */
    public synchronized IndexSearcher get(int docId) throws IOException {
        IndexSearcher res = paragraphIndices.get(docId);
        if (res == null) {
            // add a new paragraph index
            Directory inMemParaIndex = buildParaIndex(docId);
            IndexReader paraReader = DirectoryReader.open(inMemParaIndex);
            res = getSearcher(paraReader);
            paragraphIndices.put(docId, res);
        }
        return res;
    }

    public synchronized IndexSearcher getPageIndex(int docId) throws IOException {
        IndexSearcher res = pageIndices.get(docId);
        if (res == null) {
            // add a new page index
            Directory inMemPageIndex = buildPageIndex(docId);
            IndexReader pageReader = DirectoryReader.open(inMemPageIndex);
            res = getSearcher(pageReader);
            pageIndices.put(docId, res);
        }
        return res;
    }

    public String getPage(String text, int docId) {
        try {
            IndexSearcher searcher = getPageIndex(docId);
            QueryParser parser = new QueryParser(ResearchDoc.FIELD_CONTENT, analyzer);
            Query query = parser.parse("\"" + text + "\"");
            TopDocs topDocs = searcher.search(query, 1000);
            for (int i = 0; i < topDocs.scoreDocs.length; i++) {
                ScoreDoc scoreDoc = topDocs.scoreDocs[i];
                String pageNumber = searcher.doc(scoreDoc.doc).get(ResearchDoc.FIELD_ID);
                return pageNumber;
            }
            return "0";
        } catch (IOException | ParseException e) {
            e.printStackTrace();
            return "0";
        }
    }

    public Optional<String> getTitle(int docId) throws IOException {
        IndexSearcher searcher = mainSearcher.acquire();
        String title = searcher.doc(docId).get(ResearchDoc.FIELD_TITLE);
        mainSearcher.release(searcher);
        return Optional.ofNullable(title);
    }

    /**
     * Builds up a paragraph index during negative sampling of learning from pseudo-negative examples.
     * @param refDocId A given document id.
     * @return An in-memory index of passages which are similar to a query but not annotated.
     */
    private Directory buildParaIndex(int refDocId) throws IOException {
        Directory ramdir = new RAMDirectory();
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);

        // Form multiple variable length windows
        for (ParagraphBuilder builder : paragraphBuilders) {
            getSubsetToSearch(refDocId, writer, builder);
        }

        writer.commit();
        writer.close();
        return writer.getDirectory();
    }

    /**
     * Builds up a page index.
     * @param refDocId A given document id.
     * @return An in-memory index of pages.
     */
    private Directory buildPageIndex(int refDocId) throws IOException {
        Directory ramdir = new RAMDirectory();
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);
        mainSearcher.maybeRefresh();
        IndexSearcher searcher = mainSearcher.acquire();
        addPages(refDocId, searcher, writer);
        mainSearcher.release(searcher);
        writer.commit();
        writer.close();
        return writer.getDirectory();
    }

    private IndexSearcher getSearcher(IndexReader reader) {
        IndexSearcher res = new IndexSearcher(reader);
        Similarity sim = new LMJelinekMercerSimilarity(0.6f);
        res.setSimilarity(sim);
        return res;
    }

    private SearcherFactory getSearcherFactory() {
        return new SearcherFactory() {
            @Override
            public IndexSearcher newSearcher(IndexReader reader, IndexReader previousReader) throws IOException {
                return getSearcher(reader);
            }
        };
    }

    private void getSubsetToSearch(int refDocId, IndexWriter writer, ParagraphBuilder builder) throws IOException {
        mainSearcher.maybeRefresh();
        IndexSearcher searcher = mainSearcher.acquire();
        String srcDocName = searcher.doc(refDocId).get(ResearchDoc.FIELD_ID);

        // construct a range query TODO: marting I don't know why we query the index here, we already know which doc we have
        TermQuery docNameQuery = new TermQuery(new Term(ResearchDoc.FIELD_ID, srcDocName));
        TopDocs topDocs = searcher.search(docNameQuery, 1000);

        ScoreDoc[] hits = topDocs.scoreDocs;
        for (ScoreDoc hit : hits) { // TODO: why is hit/hits useless here?
            addParagraphs(refDocId, searcher, writer, builder);
        }
        mainSearcher.release(searcher);
    }

    private void addParagraphs(int docId, IndexSearcher searcher, IndexWriter paraWriter, ParagraphBuilder builder) throws IOException {
        //boolean useSentenceBased= Boolean.parseBoolean(prop.getProperty("use.sentence.based", "false"));
        //int paraNumberOfSentences= Integer.parseInt(prop.getProperty("para.number.of.sentences", "1"));

        //SentenceBasedParagraphBuilder builderSB = new SentenceBasedParagraphBuilder(paraNumberOfSentences, analyzer);

        String content = searcher.doc(docId).get(ResearchDoc.FIELD_CONTENT);
        // Write out paragraphs...
        List<Paragraph> paragraphs = builder.constructParagraphs(docId, content);

        for (Paragraph p : paragraphs) {
            Document doc = getSimpleDocument(p.id, p.content, p.indexingMethod);
            logger.debug(p.content);

            paraWriter.addDocument(doc);
        }
    }

    private void addPages(int docId, IndexSearcher searcher, IndexWriter pageWriter) throws IOException {
        Document fullDoc = searcher.doc(docId);
        int pageCount = Integer.parseInt(fullDoc.get(ResearchDoc.FIELD_PAGE_COUNT));
        System.out.println("Uploaded PDF has " + pageCount + " pages.");
        // add individual pages
        for (int pageNumber = 0; pageNumber < pageCount ; pageNumber++) {
            String page = fullDoc.get(ResearchDoc.FIELD_PAGE(pageNumber));
            Document doc = getSimpleDocument(String.valueOf(pageNumber), page, IndexingMethod.NONE);
            pageWriter.addDocument(doc);
        }
        // also add double pages (to capture things that are spread across 2 pages)
        /*
        for (int pageNumber = 0; pageNumber < pageCount - 1; pageNumber++) {
            String page1 = fullDoc.get(ResearchDoc.FIELD_PAGE(pageNumber));
            String page2 = fullDoc.get(ResearchDoc.FIELD_PAGE(pageNumber + 1));
            String page = page1 + " " + page2;
            Document doc = getSimpleDocument(pageNumber + "-" + (pageNumber+1), page);
            pageWriter.addDocument(doc);
        }
        */
    }

    private Document getSimpleDocument(String id, String content, IndexingMethod indexingMethod) {
        Document doc = new Document();
        doc.add(new Field(ResearchDoc.FIELD_ID, id, LuceneField.STORED_NOT_ANALYZED.getType()));
        doc.add(new Field(ResearchDoc.FIELD_CONTENT, content,
                new LuceneField().stored(true).analyzed(true).termVectorsWithPositions().getType()));
        if (indexingMethod != null && indexingMethod != IndexingMethod.NONE)
            doc.add(new Field(ResearchDoc.INDEXING_METHOD, indexingMethod.toString(), LuceneField.STORED_NOT_ANALYZED.getType()));
        return doc;
    }

    @Override
    public void close() throws IOException {
        mainSearcher.close();
        for (IndexSearcher searcher : paragraphIndices.values()) {
            searcher.getIndexReader().close();
        }
    }

    public Analyzer getAnalyzer() { return analyzer; }

    private static Analyzer getDefaultAnalyzer() {
        String defaultPath = "nlp/stop.txt";
        String path;
        try {
            path = Props.loadProperties().getProperty("stopfile", defaultPath);
        } catch (IOException e) {
            path = defaultPath;
        }
        return PaperIndexer.constructAnalyzer(path);
    }

    public String getDocName(int docId) throws IOException {
        IndexSearcher searcher = mainSearcher.acquire();
        String res = searcher.doc(docId).get(ResearchDoc.FIELD_NAME);
        mainSearcher.release(searcher);
        return res;
    }

    public List<IndexedDocument> getAllDocuments() throws IOException {
        IndexManager manager = this;
        // TODO I don't think we need to refresh the searcherManager here
        IndexSearcher searcher = mainSearcher.acquire();
        int n = searcher.getIndexReader().numDocs();
        mainSearcher.release(searcher);
        List<Integer> validDocIds = IntStream.range(0, n).boxed().collect(Collectors.toList());
        return validDocIds.stream()
                .map(docId -> new IndexedDocument(manager, docId))
                .collect(Collectors.toList());
    }
}
