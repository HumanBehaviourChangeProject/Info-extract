package com.ibm.drl.hbcp.parser.pdf.abbyy;

import com.ibm.drl.hbcp.parser.pdf.PdfAnalysisOutput;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.Document;
import com.ibm.drl.hbcp.parser.pdf.abbyy.structure.FormattingConverter;
import com.thoughtworks.xstream.XStream;
import com.thoughtworks.xstream.converters.reflection.PureJavaReflectionProvider;
import com.thoughtworks.xstream.io.xml.StaxDriver;
import lombok.Getter;

import java.io.*;

public class AbbyyXmlParser implements PdfAnalysisOutput {

    @Getter
    private final Document document;

    private static final XStream xstream = new XStream(new PureJavaReflectionProvider(), new StaxDriver());

    static {
        // setup XStream security (so that the code may not be infiltrated with malicious XML outputs)
        XStream.setupDefaultSecurity(xstream);
        xstream.allowTypesByWildcard(new String[] {
                Document.class.getPackage().getName()+".*"
        });
        // process all XStream annotations of the root class and all dependent classes
        xstream.processAnnotations(Document.class);
        // ignore things we didn't map
        xstream.ignoreUnknownElements();
        xstream.registerConverter(new FormattingConverter());
    }

    public AbbyyXmlParser(InputStream input) throws IOException {
        document = getDocument(input);
    }

    public AbbyyXmlParser(File abbyyXmlOutput) throws IOException {
        if (!abbyyXmlOutput.exists()) throw new FileNotFoundException(abbyyXmlOutput.getAbsolutePath());
        document = getDocument(new FileInputStream(abbyyXmlOutput));
    }

    private static Document getDocument(InputStream abbyyXmlOutput) throws IOException {
        return (Document)xstream.fromXML(abbyyXmlOutput);
    }

    public static File getAbbyyFile(File abbyyFolder, String docname) {
        return new File(abbyyFolder, docname + ".xml");
    }
}
