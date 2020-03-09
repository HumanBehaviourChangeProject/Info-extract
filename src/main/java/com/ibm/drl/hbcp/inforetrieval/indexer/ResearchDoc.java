/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import com.ibm.drl.hbcp.util.LuceneField;
import org.apache.commons.io.IOUtils;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.parser.DefaultParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

import javax.swing.text.AbstractDocument;
import java.io.*;
import java.util.List;
import java.util.stream.Collectors;

import static com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer.EOS;

/**
 * A class to represent the in-memory structure of the Lucene index format.
 * 
 * @author dganguly
 */
public class ResearchDoc {
    File file;
    String title;
    String authors;
    String intro;
    String ppText;
    String plainText;
    List<String> pages;
    InputStream fstream;
    int unjudged;
    
    static final public String FIELD_ID = "id";
    static final public String FIELD_NAME = "name";
    
    static final public String FIELD_TITLE = "title";
    static final public String FIELD_AUTHORS = "authors";
    static final public String FIELD_CONTENT = "content";
    static final public String FIELD_INTRO = "intro";
    static final public String FIELD_UNJUDGED = "unjudged";
    
    static final public String UNJUDGED_FOLDER_NAME = "unjudged";

    public final static String FIELD_PAGE_COUNT = "pagecount";
    public final static String FIELD_PAGE_X = "page";

    public final static String INDEXING_METHOD = "indexing";

    public static String FIELD_PAGE(int pageNumber) { return FIELD_PAGE_X + pageNumber; }
    
    /**
     * Constructs this object from a given file.
     * @param file File object pointing to a pdf document.
     */    
    public ResearchDoc(File file) {
        this.file = file;        
        title = "";
        authors = "";
        intro = "";
        
        String path = file.getAbsolutePath();
        unjudged = path.contains(UNJUDGED_FOLDER_NAME) ? 1 : 0;
    }
    
    /**
     * Called from the REST API
     * @param fstream InputStream of a file uploaded through the Swagger interface
     */    
    public ResearchDoc(InputStream fstream) {
        this.fstream = fstream;
        title = "";
        authors = "";
        intro = "";
    }
    
    /**
     * Constructs this object from a Lucene 'Document' object read from an index.
     * @param doc Lucene 'Document' object
    */    
    public ResearchDoc(Document doc) {
        title = doc.get(FIELD_TITLE);
        authors = doc.get(FIELD_AUTHORS);
        intro = leadingWords(doc.get(FIELD_INTRO), 30); // first 30 words
        unjudged = Integer.parseInt(doc.get(FIELD_UNJUDGED));
    }
    
    /**
     * Uses Jsoup to extract the content from each tag of the text extracted through
     * the Tika API.
    */    
    public void extractInfoFromDOM() throws TikaException, SAXException, IOException {
        // store the input stream into a byte array
        byte[] bytes = file != null ? null : storeStream(fstream);
        // this also extracts pages now (in the 'pages' field)
        String extractedText = file != null? parseToPlainText(file) : parseToPlainText(recallStream(bytes));
        ppText = removeReferences(extractedText);
        pages = pages.stream().map(this::removeReferences).collect(Collectors.toList());
        String xml = file!=null? parseToHTML(file) : parseToHTML(recallStream(bytes));
        
        xml = preProcess(xml);
        
        /* DEBUG: To write out the processed text...
        String outFile = BaseDirInfo.getBaseDir() + ".txt";
        FileWriter fout = new FileWriter(outFile);
        System.out.println("Writing plain text to " + outFile);
        fout.write(ppText);
        fout.close();
        */
        
        org.jsoup.nodes.Document jdoc = Jsoup.parse(xml);

        try {
            Elements titleElement = jdoc.select("meta[name=pdf:docinfo:title]");
            title = titleElement.first().attr("content").toString();

            Elements authorElements = jdoc.select("meta[name=meta:author]");
            if (authorElements!=null) {
                for (Element authorElt : authorElements) {
                    authors += authorElt.attr("content").toString() + "; ";
                }
            }

            intro = extractSection(jdoc, "Introduction");        
        }
        catch (NullPointerException nex) { System.err.println("Some metadata may be missing!");}
        plainText = ppText;
    }
    
    String preProcess(String content) throws IOException {
        // Changes <p>Section-name to <p name='Section-name>"... 
        // This is done so that the DOM can be traveresed easily afterwards.
        try (BufferedReader br = new BufferedReader(new StringReader(content))) {
            String line;
            StringBuffer ppContent = new StringBuffer();

            final String pattern = "<p>";
            final int patternOffset = pattern.length();

            while ((line = br.readLine()) != null) {
                if (!line.startsWith(pattern)) {
                    ppContent.append(line);
                    continue;
                }
                ppContent.append("<p name=\"" + line.substring(patternOffset) + "\">");
            }
            return ppContent.toString();
        }
    }
    
    private String parseToHTML(File file) throws IOException, TikaException, SAXException {
        return parseFile(file, new ToXMLContentHandler());
    }
    
    private String parseToHTML(InputStream fstream) throws IOException, TikaException, SAXException {
        return parse(fstream, new ToXMLContentHandler());
    }
        
    private String parseToPlainText(File file) throws IOException, SAXException, TikaException {
        return parseFile(file, new BodyContentHandler(-1), new PageContentHandler());
    }
    
    private String parseToPlainText(InputStream fstream) throws IOException, SAXException, TikaException {
        return parse(fstream, new BodyContentHandler(-1), new PageContentHandler());
    }

    private String parseFile(File file, ContentHandler handler, PageContentHandler pageHandler) throws IOException, SAXException, TikaException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return parse(fis, handler, pageHandler);
        }
    }

    private String parseFile(File file, ContentHandler handler) throws IOException, SAXException, TikaException {
        try (FileInputStream fis = new FileInputStream(file)) {
            return parse(fis, handler);
        }
    }

    private String parse(InputStream fis, ContentHandler handler) throws IOException, SAXException, TikaException {
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        parser.parse(fis, handler, metadata);
        return handler.toString();
    }

    private String parse(InputStream fis, ContentHandler handler, PageContentHandler pageHandler) throws IOException, SAXException, TikaException {
        // copy the InputStream in a buffer
        byte[] bytes = IOUtils.toByteArray(fis);
        // create 2 input streams from that
        InputStream is = new ByteArrayInputStream(bytes);
        InputStream isPages = new ByteArrayInputStream(bytes);
        // now you can read from both is and isPages independently
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        Metadata metadataThrowaway = new Metadata();
        parser.parse(is, handler, metadata);
        parser.parse(isPages, pageHandler, metadataThrowaway);
        pages = pageHandler.getPages();
        return handler.toString();
    }

    private byte[] storeStream(InputStream is) throws IOException {
        return IOUtils.toByteArray(is);
    }

    private InputStream recallStream(byte[] bytes) {
        return new ByteArrayInputStream(bytes);
    }
    
    String extractSection(org.jsoup.nodes.Document xmlDom, String sectionName) {
        Element section = xmlDom.select("p[name=" + sectionName + "]").first();
        return section==null? "" : section.text();
    }
    
    String removeReferences(String str) {
        BufferedReader br = new BufferedReader(new StringReader(str));
        String line;
        StringBuffer buff = new StringBuffer();

        try {
            while ((line = br.readLine()) != null) {
                if (line.toLowerCase().startsWith("references")) {
                    break;
                }
                buff.append(line).append("\n");
            }
        } catch (IOException e) {
            throw new RuntimeException("IOException shouldn't happen on StringReader", e);
        }
        return buff.toString();
    }

    // Remove lines that are too short.
    // In 2015 EARS outcome paper, the background text and the margin text
    // appears as short lines.
    String removeLinesWithTooFewWords(String text, int threshold) throws IOException {
        StringBuffer buff = new StringBuffer();
        BufferedReader br = new BufferedReader(new StringReader(text));
        String line;
        while ((line = br.readLine()) != null) {
            int numSpaces = line.split("\\s+").length;
            if (numSpaces < threshold)
                continue;
            buff.append(line).append("\n");
        }
        return buff.toString();
    }
    
    // This is required for handling the EOS constraint, in order not to
    // propagate the word window across sentences.
    String preProcessPlainText(String content) {
        String pp = content.replaceAll("\\. ", EOS);
        return pp;
    }

    /**
     * Constructs a Lucene 'Document' object given a document id (iterated while
     * traversing the collection) and a file name.
     * @param docId Incrementing document id (starting at 0).
     * @param fileName Name of a pdf file
     * @return A Lucene 'Document' object.
    */    
    public Document constructDoc(int docId, String fileName) throws IOException {
        
        ppText = removeLinesWithTooFewWords(ppText, 5);
        ppText = preProcessPlainText(ppText);  // replace . with EOS
        
        // Write out the document
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
        doc.add(new Field(FIELD_ID, String.valueOf(docId), LuceneField.STORED_NOT_ANALYZED.getType()));
        doc.add(new Field(FIELD_UNJUDGED, String.valueOf(this.unjudged), LuceneField.STORED_NOT_ANALYZED.getType()));
        doc.add(new Field(FIELD_NAME, fileName, LuceneField.STORED_NOT_ANALYZED.getType()));
        
        // Meta-data
        doc.add(new Field(FIELD_TITLE, title.equals("")? fileName : title,
                new LuceneField().stored(true).analyzed(true).with(ft -> ft.setStoreTermVectors(false)).getType()));
        doc.add(new Field(FIELD_AUTHORS, authors,
                new LuceneField().stored(true).analyzed(true).with(ft -> ft.setStoreTermVectors(false)).getType()));
        doc.add(new Field(FIELD_INTRO, intro,
                new LuceneField().stored(true).analyzed(true).with(ft -> ft.setStoreTermVectors(false)).getType()));
        
        // Main content (without references)
        doc.add(new Field(FIELD_CONTENT, plainText,
                new LuceneField().stored(true).analyzed(true).with(ft -> ft.setStoreTermVectors(false)).getType()));

        // Pages
        doc.add(new Field(FIELD_PAGE_COUNT, String.valueOf(pages.size()), LuceneField.STORED_NOT_ANALYZED.getType()));
        for (int pageNumber = 0; pageNumber < pages.size(); pageNumber++) {
            doc.add(new Field(FIELD_PAGE(pageNumber), pages.get(pageNumber),
                    new LuceneField().stored(true).analyzed(true).with(ft -> ft.setStoreTermVectors(false)).getType()));
        }
        
        return doc;
    }
    
    public String getPlainText() { return plainText; }
    public String getPPText() { return ppText; }
    public String getFileName() { return file.getName(); }
    public String getTitle() { return title; }
    
    static public String leadingWords(String text, int nwords) {
        String[] tokens = text.split("\\s+");
        StringBuffer buff = new StringBuffer();
        
        nwords = Math.min(nwords, tokens.length);
        for (int i=0; i<nwords; i++) {
            buff.append(tokens[i]).append(" ");
        }
        buff.deleteCharAt(buff.length()-1);
        buff.append("...");
        
        return buff.toString();
    }

    public static void main(String[] args) throws IOException, TikaException, SAXException {
        ResearchDoc doc = new ResearchDoc(new File("./data/pdfs/judged/Bize 2010.pdf"));
        doc.extractInfoFromDOM();
    }
}
