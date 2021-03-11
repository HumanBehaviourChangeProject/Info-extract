package com.ibm.drl.hbcp.parser.pdf.abbyy;

import com.ibm.drl.hbcp.parser.pdf.Document;
import org.junit.Assert;
import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AbbyyXmlParserTest {

    private static final String HEAD = "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>\n" +
            "<document xmlns=\"http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml\" version=\"1.0\" producer=\"ABBYY FineReader Engine 12\" languages=\"\" xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xsi:schemaLocation=\"http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml http://www.abbyy.com/FineReader_xml/FineReader10-schema-v1.xml\">\n" +
            "<page width=\"2480\" height=\"3260\" resolution=\"300\" originalCoords=\"1\">\n" +
            "<block blockType=\"Text\" blockName=\"\" l=\"261\" t=\"59\" r=\"1886\" b=\"110\">\n" +
            "<text>\n" +
            "<par lineSpacing=\"1150\">\n";
    private static final String TAIL = "</par></text></block></page></document>";

    private static final String OLD = HEAD + "<line><formatting>Hello!</formatting></line>" + TAIL;
    private static final String NEW = HEAD + "<line><formatting>" +
            "<charParams>H</charParams>" +
            "<charParams>e</charParams>" +
            "<charParams>l</charParams>" +
            "<charParams>l</charParams>" +
            "<charParams>o</charParams>" +
            "<charParams>!</charParams>" +
            "</formatting></line>" + TAIL;

    @Test
    public void testAbbyParserSimpleJsonOld() throws IOException {
        Document doc = new AbbyyXmlParser(getInputStream(OLD)).getDocument();
        Assert.assertEquals("String representation of doc", "Hello!", doc.getValue().trim());
    }

    @Test
    public void testAbbyParserSimpleJsonNew() throws IOException {
        Document doc = new AbbyyXmlParser(getInputStream(NEW)).getDocument();
        Assert.assertEquals("String representation of doc", "Hello!", doc.getValue().trim());
    }

    private InputStream getInputStream(String jsonTestCase) {
        return new ByteArrayInputStream(jsonTestCase.getBytes());
    }
}
