package com.ibm.drl.hbcp.inforetrieval.apr;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.LuceneField;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.commons.lang3.StringUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.memory.MemoryIndex;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.TopDocs;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *  Enriches an indexed document representing a paragraph with
 *  annotations coming from JSON refs.
 *  You should create one annotator for the whole collection and call
 *  the {@link #annotate} method on each paragraph.
 *  @author mgleize
 */
public class ParagraphAnnotator {

    private final JSONRefParser jsonRefParser;
    private final Map<String, AttributeVec> lruCache = new LRUMap<>(10);
    private final Analyzer analyzer;
    private final QueryParser queryParser;

    // silly unique name just to make sure the analyzer uses the default fallback analyzer (if it's a PerField wrapper)
    private static final String SINGLE_FIELD_NAME = "DUMMY_content1";

    public ParagraphAnnotator(String propFileName, Analyzer analyzer) throws Exception {
        Properties prop = new Properties();
        try (FileReader fr = new FileReader(new File(propFileName))) {
            prop.load(fr);
        }
        // parse json refs
        jsonRefParser = new JSONRefParser(propFileName);
        jsonRefParser.buildAll();
        this.analyzer = analyzer;
        queryParser = new QueryParser(SINGLE_FIELD_NAME, analyzer);
    }

    public void annotate(Document paragraph, String filename, String content) throws IOException {
        // retrieve all the attributes in the full document
        AttributeVec allAttributes = getAttributesCached(filename);
        // determine which attributes are present in the paragraph
        List<AnnotatedAttributeValuePair> foundAttributes = getMatchingAttributes(content, allAttributes);
        // enrich the document
        enrichDocument(paragraph, foundAttributes);
    }

    private List<AnnotatedAttributeValuePair> getMatchingAttributes(String content, AttributeVec attributes) throws IOException {
        // quick in-memory index with a single field: the content of the paragraph
        MemoryIndex index = new MemoryIndex();
        index.addField(SINGLE_FIELD_NAME, content, analyzer);
        // query the index normally
        IndexSearcher searcher = index.createSearcher();
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        for (AttributeValuePair attribute : attributes.getSortedAttributes()) {
            // try first with the full context, then with the annotation
            // TODO check if this needs to be sorted and if we should sort annotatedattributevalue pair
            List<String> searchStrings = Lists.newArrayList(((AnnotatedAttributeValuePair) attribute).getContext(), attribute.getValue());
            for (String searchString : searchStrings) {
                TopDocs topDocs = searcher.search(getQuery(searchString), 1);
                if (topDocs.totalHits.value > 0) {
                    res.add((AnnotatedAttributeValuePair) attribute);
                    break;
                }
            }
        }
        return res;
    }

    private void enrichDocument(Document doc, List<AnnotatedAttributeValuePair> foundAttributes) {
        if (!foundAttributes.isEmpty()) { // this is an annotated document
            doc.add(new Field(ParagraphIndexer.APR_IS_ANNOTATED_PARAGRAPH_FIELD, "1", LuceneField.STORED_NOT_ANALYZED.getType()));
//            Collections.sort(foundAttributes);
            String id = joinAttributeField(foundAttributes, new FieldSelector() {
                public String getField(AnnotatedAttributeValuePair a) { return a.getAttribute().getId(); }
            });
            String value = joinAttributeField(foundAttributes, new FieldSelector() {
                public String getField(AnnotatedAttributeValuePair a) { return a.getHighlightedText(); }
            });
            String context = joinAttributeField(foundAttributes, new FieldSelector() {
                public String getField(AnnotatedAttributeValuePair a) { return a.getContext(); }
            });
            doc.add(new Field(ParagraphIndexer.APR_ATTRIB_ID_FIELD, id, LuceneField.STORED_NOT_ANALYZED.getType()));
            doc.add(new Field(ParagraphIndexer.APR_ATTRIB_VALUE_FIELD, value, LuceneField.STORED_NOT_ANALYZED.getType()));
            doc.add(new Field(ParagraphIndexer.APR_ATTRIB_CONTEXT_FIELD, context, LuceneField.STORED_NOT_ANALYZED.getType()));
        } else {
            doc.add(new Field(ParagraphIndexer.APR_IS_ANNOTATED_PARAGRAPH_FIELD, "0", LuceneField.STORED_NOT_ANALYZED.getType()));
        }
    }

    private Query getQuery(String searchString) {
        try {
            return queryParser.parse("\"" + searchString + "\"");
        } catch(ParseException e) {
            System.err.println("The annotation text query couldn't be parsed, this shouldn't happen.");
            e.printStackTrace();
            System.err.println("getQuery will now return null.");
            return null;
        }
    }

    private AttributeVec getAttributesCached(String docName) {
        // we're going to see several paragraphs from the same document, so a little caching won't hurt
        AttributeVec res = lruCache.get(docName);
        if (res == null) {
            res = jsonRefParser.getAttributesInDoc(docName);
            lruCache.put(docName, res);
        }
        return res;
    }

    private static interface FieldSelector {
        public String getField(AnnotatedAttributeValuePair a);
    }

    private String joinAttributeField(List<AnnotatedAttributeValuePair> attributes, FieldSelector fieldSelector) {
        List<String> fields = new ArrayList<>();

        for (AnnotatedAttributeValuePair a : attributes) fields.add(fieldSelector.getField(a));
        return StringUtils.join(fields, ParagraphIndexer.ATTRIB_SEPARATOR_PATTERN);
    }
}
