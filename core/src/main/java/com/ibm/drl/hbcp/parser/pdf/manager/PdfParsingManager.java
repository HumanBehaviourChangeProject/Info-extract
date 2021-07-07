package com.ibm.drl.hbcp.parser.pdf.manager;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.tuple.Pair;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.Page;
import com.ibm.drl.hbcp.util.Props;

public class PdfParsingManager implements PdfToDocumentFunction {

    private final PdfParsingConfig config;
    private final Properties props;
    private final Map<RegisteredPdfParsers.PdfParser, PdfToDocumentFunction> parsers;

    private final static Set<PdfParsingConfig.ContentToUse> MIXED_DOC = Sets.newHashSet(PdfParsingConfig.ContentToUse.ONLY_TEXT, PdfParsingConfig.ContentToUse.ONLY_TABLES);

    public PdfParsingManager(PdfParsingConfig config, Properties props) {
        this.config = config;
        this.props = props;
        parsers = RegisteredPdfParsers.getParsers(props);
    }

    public PdfParsingManager(Properties props) {
        this(
                new PdfParsingConfig() {
                    @Override
                    public List<Pair<RegisteredPdfParsers.PdfParser, ContentToUse>> getParsingOrder() {
                        List<Pair<RegisteredPdfParsers.PdfParser, ContentToUse>> res = new ArrayList<>();
                        res.add(Pair.of(RegisteredPdfParsers.PdfParser.ABBYY, ContentToUse.ALL));
                        res.add(Pair.of(RegisteredPdfParsers.PdfParser.TEXTRACT, ContentToUse.ONLY_TABLES));
                        return res;
                    }
                },
                props
        );
    }

    @Override
    public Document getDocument(File pdf) throws IOException {
        List<Pair<Document, PdfParsingConfig.ContentToUse>> parses = new ArrayList<>();
        Iterator<Pair<RegisteredPdfParsers.PdfParser, PdfParsingConfig.ContentToUse>> pdfParsingMethods = config.getParsingOrder().iterator();
        List<IOException> errors = new ArrayList<>();
        while (!canBeCompleted(parses) && pdfParsingMethods.hasNext()) {
            Pair<RegisteredPdfParsers.PdfParser, PdfParsingConfig.ContentToUse> parsingMethod = pdfParsingMethods.next();
            RegisteredPdfParsers.PdfParser parserIdentifier = parsingMethod.getLeft();
            PdfToDocumentFunction parser = parsers.get(parserIdentifier);
            try {
                Document doc = parser.getDocument(pdf);
                parses.add(Pair.of(doc, parsingMethod.getRight()));
            } catch (IOException e) {
                errors.add(e);
                // this parsing method cannot be used for this doc, just skip it
                // this is a good place to put a message in case too many of the exceptions below are thrown
                System.err.println(parser + " failed for " + pdf.getName());
            }
        }
        if (!canBeCompleted(parses)) {
            throw new IOException(pdf + " cannot be analyzed: ", errors.get(errors.size() - 1));
        }
        // parses now reliably contains the parts of a complete analysis of the PDF, we just have to put them together
        return buildDocument(parses);
    }

    private Document buildDocument(List<Pair<Document, PdfParsingConfig.ContentToUse>> parses) {
        if (parses.get(0).getRight() == PdfParsingConfig.ContentToUse.ALL) {
            // if the first method has everything you need, just return the document as is
            return parses.get(0).getLeft();
        } else {
            // this means the tables and text are coming from different sources
            Pair<Document, Document> textAndTable = getTextAndTableComponents(parses);
            // first the pages of text (minus the tables)
            List<Page> pages = new ArrayList<>(textAndTable.getLeft().getPages(b -> b.getType() != Block.Type.TABLE));
            // add an extra page of tables
            pages.add(() -> textAndTable.getRight().getTables());
            return () -> pages;
        }
    }

    private Pair<Document, Document> getTextAndTableComponents(List<Pair<Document, PdfParsingConfig.ContentToUse>> parses) {
        if (parses.get(0).getRight() == PdfParsingConfig.ContentToUse.ONLY_TEXT) {
            return Pair.of(parses.get(0).getLeft(), parses.get(1).getLeft());
        } else {
            return Pair.of(parses.get(1).getLeft(), parses.get(0).getLeft());
        }
    }

    private boolean canBeCompleted(List<Pair<Document, PdfParsingConfig.ContentToUse>> parses) {
        Set<PdfParsingConfig.ContentToUse> contentToUse = parses.stream()
                .map(Pair::getRight)
                .collect(Collectors.toSet());
        return contentToUse.contains(PdfParsingConfig.ContentToUse.ALL)
                || contentToUse.equals(MIXED_DOC);
    }

    public static void writeAllParsesToJsonFiles(File pdfFolder, File jsonFolder) throws IOException {
        PdfParsingManager parser = new PdfParsingManager(Props.loadProperties());
        List<File> skipped = new ArrayList<>();
        for (File pdf : pdfFolder.listFiles()) {
            if (pdf.getName().endsWith(".pdf")) {
                System.out.print(pdf.getName() + "... ");
                try {
                    // parse
                    Document doc = parser.getDocument(pdf);
                    // write the JSON in another file
                    File jsonOutput = new File(jsonFolder, pdf.getName() + ".json");
                    jsonOutput.getParentFile().mkdirs();
                    try (BufferedWriter bw = new BufferedWriter(new FileWriter(jsonOutput))) {
                        bw.write(doc.toPrettyString());
                    }
                } catch (IOException e) {
                    skipped.add(pdf);
                    System.err.println("This pdf will be skipped.");
                    e.printStackTrace();
                }
                System.out.println("Done.");
            }
        }
        // print skipped PDFs:
        System.out.println("========= SKIPPED ==============");
        for (File skippedPdf : skipped) {
            System.out.println(skippedPdf.getName());
        }
        System.out.println(skipped.size() + " skipped.");
    }

    public static void main(String[] args) throws IOException {
        writeAllParsesToJsonFiles(new File("data/All_330_PDFs"), new File("data/All_330_PDFs_extracted"));
    }

    /*
    public static void main(String[] args) throws Exception {
        GrobidParser parser = new GrobidParser(new File("data/All_330_PDFs/Harris 2010.pdf"));
        Document doc = parser.getDocument();
        System.out.println(doc.getPages().size());
    }
    */
}
