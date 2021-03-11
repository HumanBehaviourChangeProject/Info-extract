package com.ibm.drl.hbcp.inforetrieval.indexer;

import com.google.common.collect.Sets;
import org.apache.commons.io.output.NullOutputStream;
import org.xml.sax.Attributes;
import org.xml.sax.SAXException;
import org.xml.sax.helpers.DefaultHandler;

import java.io.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;

public class PageContentHandler extends DefaultHandler {
    private Writer writer;
    private boolean isWritingPage;
    private String previousTag;
    private final List<String> pages = new ArrayList<>();

    private final static String OUTER_TAG = "div";
    private final static String INNER_TAG = "p";
    private final static Set<String> IGNORED_TAGS = Sets.newHashSet("a");

    public PageContentHandler() {
        writer = new OutputStreamWriter(NullOutputStream.NULL_OUTPUT_STREAM); // by default send to /dev/null
        // just before reading a page
        isWritingPage = false;
        previousTag = null;
    }

    @Override
    public void startElement (String uri, String localName, String qName, Attributes atts) throws SAXException  {
        if (!isWritingPage && !isTagIgnored(qName)) {
            if (INNER_TAG.equals(qName) && OUTER_TAG.equals(previousTag)) {
                startPage();
            } else {
                previousTag = qName;
            }
        }
    }

    @Override
    public void endElement (String uri, String localName, String qName) throws SAXException {
        if (isWritingPage && !isTagIgnored(qName)) {
            if (OUTER_TAG.equals(qName) && INNER_TAG.equals(previousTag)) {
                endPage();
            } else {
                previousTag = qName;
            }
        }
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        try {
            this.writer.write(ch, start, length);
        } catch (IOException var5) {
            throw new SAXException("Error writing: " + new String(ch, start, length), var5);
        }
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        this.characters(ch, start, length);
    }

    protected void startPage() {
        isWritingPage = true;
        writer = new StringWriter();
        previousTag = null;
    }

    protected void endPage() throws SAXException {
        isWritingPage = false;
        String preFlushingText = writer.toString();
        try {
            writer.flush();
            pages.add(writer.toString());
            writer.close();
            writer = new OutputStreamWriter(NullOutputStream.NULL_OUTPUT_STREAM);
            previousTag = null;
        } catch (IOException e) {
            throw new SAXException("Error ending page: " + preFlushingText, e);
        }
    }

    @Override
    public void endDocument() throws SAXException {
        super.endDocument();
        //System.out.println(getPages().size() + " pages.");
    }

    private boolean isTagIgnored(String tag) {
        return IGNORED_TAGS.contains(tag);
    }

    public List<String> getPages() {
        return pages;
    }

    public String toString() {
        return this.writer.toString();
    }
}
