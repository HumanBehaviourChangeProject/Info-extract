package com.ibm.drl.hbcp.inforetrieval.indexer;

import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.document.Document;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Properties;

/**
 * Builds an index of papers, not starting from PDFs directly, but from a parser output (typically either ABBYY XML or "Reparse" JSON).
 *
 * Will not work for papers in subfolders (like the original PaperIndexer does).
 * Also likely incompatible with the page indexing (pages are parsed from the original PDFs, not from parser output).
 *
 * @author marting
 */
public class PaperIndexerWithParser extends PaperIndexer {

    // this should be either Abbyy or Reparse basically
    private final PdfToDocumentFunction outputParser;
	private static final Logger logger = LoggerFactory.getLogger(PaperIndexerWithParser.class);

    /**
     * Constructs the object by taking as an argument the relative path of a
     * properties file.
     *
     * @param propFile Relative path (from the project base) to the a properties file.
     */
    public PaperIndexerWithParser(String propFile, PdfToDocumentFunction outputParser) throws IOException {
        super(propFile);
        this.outputParser = outputParser;
    }


    /**
     * Indexes a particular parser output file.
     * @param file The relative path name of the parser output file.
     */
    @Override
    public void indexFile(File file) throws TikaException, SAXException, IOException {
        if (!isValid(file))
            return;
        logger.info("Indexing file: " + file.getName());
        ResearchDoc researchDoc = new ResearchDoc(file);
        researchDoc.extractInfoFromDOM();

        try {
            // here, replace the Document's preprocessed content with the parsed output
            com.ibm.drl.hbcp.parser.pdf.Document doc = outputParser.getDocument(file);
            researchDoc.ppText = doc.getValue();
            researchDoc.plainText = researchDoc.ppText;
        } catch (IOException e) {
            System.err.println("No parser output for: " + file);
            return;
        }

        // proceed as usual for the indexation of paragraphs
        Document doc = researchDoc.constructDoc(docId, file.getName());

        writer.addDocument(doc);
        indexPara(researchDoc, docId, WINDOW_SIZE);

        docId++;
    }

    protected boolean isValid(File pdf) {
        String extension = FilenameUtils.getExtension(pdf.getName());
        return extension.equalsIgnoreCase("pdf");
    }

    /** Index the papers in a given directory. */
    @Override
    protected void indexDirectory(File dir) throws TikaException, SAXException, IOException {
        logger.info("Indexing directory " + dir);
        File[] files = dir.listFiles();
        Arrays.sort(files);

        for (File file : files) {
            // marting: removed the capability of indexing in subfolders
            indexFile(file);
        }
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java PaperIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            Properties props = Props.loadProperties(args[0]);
            PaperIndexer indexer = new PaperIndexerWithParser(args[0],
                    new ReparsePdfToDocument(FileUtils.potentiallyGetAsResource(new File(props.getProperty("coll.extracted.reparse")))));
            indexer.processAll();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
}
