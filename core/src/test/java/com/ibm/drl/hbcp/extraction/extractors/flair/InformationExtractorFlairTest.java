package com.ibm.drl.hbcp.extraction.extractors.flair;

import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.List;

import static org.junit.Assert.assertTrue;

public class InformationExtractorFlairTest {

    private static final String testDocName = "Zhu 2012.pdf";
    private static final File testMockJsonOutput = FileUtils.potentiallyGetAsResource(new File("jsons/Zhu 2012.pdf_Flairoutput.json"));

    @Test
    public void testExtractorOnMockOutput() throws IOException {
        String docName = testDocName;
        List<String> mockOutput = Files.readAllLines(testMockJsonOutput.toPath());
        InformationExtractorFlair flairExtractor = new InformationExtractorFlair() {
            @Override
            protected List<String> getFlairJsonOutput(List<String> sentences) {
                return mockOutput;
            }
        };
        // still parse the doc even though we're not going to use that (to avoid unforeseen NPEs)
        PdfToDocumentFunction pdfParser = new ReparsePdfToDocument(Props.loadProperties());
        Document doc = pdfParser.getDocument(new File(docName));
        // just check that it returns a non-empty set of AVPs without throwing an exception
        assertTrue(flairExtractor.extract(new NamedParsedDocument(docName, doc)).size() > 0);
    }

}
