/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import static indexer.PaperIndexer.EOS;
import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.tika.exception.TikaException;
import org.apache.tika.metadata.Metadata;
import org.apache.tika.parser.AutoDetectParser;
import org.apache.tika.sax.BodyContentHandler;
import org.apache.tika.sax.ToXMLContentHandler;
import org.apache.tika.sax.XHTMLContentHandler;
import org.apache.tika.sax.xpath.Matcher;
import org.apache.tika.sax.xpath.MatchingContentHandler;
import org.apache.tika.sax.xpath.XPathParser;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.xml.sax.ContentHandler;
import org.xml.sax.SAXException;

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
        unjudged = path.indexOf(UNJUDGED_FOLDER_NAME)>=0? 1:0;
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
    public ResearchDoc(Document doc) throws Exception {
        title = doc.get(FIELD_TITLE);
        authors = doc.get(FIELD_AUTHORS);
        intro = leadingWords(doc.get(FIELD_INTRO), 30); // first 30 words
        unjudged = Integer.parseInt(doc.get(FIELD_UNJUDGED));
    }
    
    /**
     * Uses Jsoup to extract the content from each tag of the text extracted through
     * the Tika API.
     */
    public void extractInfoFromDOM() throws Exception {
        String extractedText = file != null? parseToPlainText(file) : parseToPlainText(fstream);
        ppText = removeReferences(extractedText);
        String xml = file!=null? parseToHTML(file) : parseToHTML(fstream);
        
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
        
    }
    
    String preProcess(String content) throws Exception {
        // Changes <p>Section-name to <p name='Section-name>"... 
        // This is done so that the DOM can be traveresed easily afterwards.
        BufferedReader br = new BufferedReader(new StringReader(content));
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
    
    String parseToHTML(File file) throws IOException, TikaException, SAXException {
        return parseToHTML(new FileInputStream(file));
    }
    
    String parseToHTML(InputStream fstream) throws IOException, TikaException, SAXException {
        ContentHandler handler = new ToXMLContentHandler();

        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        parser.parse(fstream, handler, metadata);
        return handler.toString();
    }
        
    String parseToPlainText(File file) throws IOException, SAXException, TikaException {
        return parseToPlainText(new FileInputStream(file));
    }
    
    String parseToPlainText(InputStream fstream) throws IOException, SAXException, TikaException {
        BodyContentHandler handler = new BodyContentHandler();
        AutoDetectParser parser = new AutoDetectParser();
        Metadata metadata = new Metadata();
        parser.parse(fstream, handler, metadata);
        return handler.toString();
    }
    
    String extractSection(org.jsoup.nodes.Document xmlDom, String sectionName) {
        Element section = xmlDom.select("p[name=" + sectionName + "]").first();
        return section==null? "" : section.text();
    }
    
    String removeReferences(String str) throws Exception {
        BufferedReader br = new BufferedReader(new StringReader(str));
        String line;
        StringBuffer buff = new StringBuffer();
        
        while ((line = br.readLine()) != null) {
            if (line.toLowerCase().startsWith("references")) {
                break;
            }
            buff.append(line).append("\n");
        }
        return buff.toString();
    }

    // Remove lines that are too short.
    // In 2015 EARS outcome paper, the background text and the margin text
    // appears as short lines.
    String removeLinesWithTooFewWords(String text, int threshold) throws Exception {
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
    public Document constructDoc(int docId, String fileName) throws Exception {
        
        plainText = ppText;
        ppText = removeLinesWithTooFewWords(ppText, 5);
        ppText = preProcessPlainText(ppText);  // replace . with EOS
        
        // Write out the document
        org.apache.lucene.document.Document doc = new org.apache.lucene.document.Document();
        doc.add(new Field(FIELD_ID, String.valueOf(docId), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_UNJUDGED, String.valueOf(this.unjudged), Field.Store.YES, Field.Index.NOT_ANALYZED));
        doc.add(new Field(FIELD_NAME, fileName, Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        // Meta-data
        doc.add(new Field(FIELD_TITLE, title.equals("")? fileName : title,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_AUTHORS, authors,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
        doc.add(new Field(FIELD_INTRO, intro,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
        
        // Main content (without references)
        doc.add(new Field(FIELD_CONTENT, plainText,
                Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.NO));
        
        return doc;
    }
    
    public String getPlainText() { return plainText; }
    public String getPPText() { return ppText; }
    public String getFileName() { return file.getName(); }
    public String getTitle() { return title; }
    
    /**
     * Returns the first 'nwords' word from a given text 'text'.
     * 
     * @param text A given piece of text.
     * @param nwords An integer specifying the number of words to extract.
     * @return 
     */
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
}
