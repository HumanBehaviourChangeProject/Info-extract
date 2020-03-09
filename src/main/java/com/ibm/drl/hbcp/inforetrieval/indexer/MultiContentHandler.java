package com.ibm.drl.hbcp.inforetrieval.indexer;

import org.xml.sax.Attributes;
import org.xml.sax.ContentHandler;
import org.xml.sax.Locator;
import org.xml.sax.SAXException;

import java.util.List;

/**
 * A content handler combining several content handlers. Every method dispatches its implementation to all the
 * sub handlers.
 *
 * @author marting
 */
public class MultiContentHandler implements ContentHandler {

    private final List<ContentHandler> handlers;

    public MultiContentHandler(List<ContentHandler> handlers) {
        this.handlers = handlers;
    }

    @Override
    public void setDocumentLocator(Locator locator) {
        for (ContentHandler h : handlers) h.setDocumentLocator(locator);
    }

    @Override
    public void startDocument() throws SAXException {
        for (ContentHandler h : handlers) h.startDocument();
    }

    @Override
    public void endDocument() throws SAXException {
        for (ContentHandler h : handlers) h.endDocument();
    }

    @Override
    public void startPrefixMapping(String prefix, String uri) throws SAXException {
        for (ContentHandler h : handlers) h.startPrefixMapping(prefix, uri);
    }

    @Override
    public void endPrefixMapping(String prefix) throws SAXException {
        for (ContentHandler h : handlers) h.endPrefixMapping(prefix);
    }

    @Override
    public void startElement(String uri, String localName, String qName, Attributes atts) throws SAXException {
        for (ContentHandler h : handlers) h.startElement(uri, localName, qName, atts);
    }

    @Override
    public void endElement(String uri, String localName, String qName) throws SAXException {
        for (ContentHandler h : handlers) h.endElement(uri, localName, qName);
    }

    @Override
    public void characters(char[] ch, int start, int length) throws SAXException {
        for (ContentHandler h : handlers) h.characters(ch, start, length);
    }

    @Override
    public void ignorableWhitespace(char[] ch, int start, int length) throws SAXException {
        for (ContentHandler h : handlers) h.ignorableWhitespace(ch, start, length);
    }

    @Override
    public void processingInstruction(String target, String data) throws SAXException {
        for (ContentHandler h : handlers) h.processingInstruction(target, data);
    }

    @Override
    public void skippedEntity(String name) throws SAXException {
        for (ContentHandler h : handlers) h.skippedEntity(name);
    }
}
