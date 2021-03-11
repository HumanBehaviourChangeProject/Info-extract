package com.ibm.drl.hbcp.inforetrieval.indexer;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;

import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.document.Document;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyXmlParser;

/**
 * Builds an index of papers, not starting from PDFs, but from ABBYY XML output.
 *
 * Will not work for papers in subfolders (like the original PaperIndexer does).
 * Also likely incompatible with the page indexing (pages are parsed from the original PDFs, not from ABBYY XML).
 *
 * @author marting
 */
public class PaperIndexerAbbyy extends PaperIndexer {

    private final File extractedFilesFolder;
	private static final Logger logger = LoggerFactory.getLogger(PaperIndexerAbbyy.class);

    /**
     * Constructs the object by taking as an argument the relative path of a
     * properties file.
     *
     * @param propFile Relative path (from the project base) to the a properties file.
     */
    public PaperIndexerAbbyy(String propFile) throws IOException {
        super(propFile);
        extractedFilesFolder = new File(prop.getProperty("coll.extracted.abbyy"));
    }


    /**
     * Indexes a particular ABBYY XML file.
     * @param file The relative path name of the ABBYY XML file.
     */
    @Override
    public void indexFile(File file) throws TikaException, SAXException, IOException {
        String extension = FilenameUtils.getExtension(file.getName());
        if (!extension.equalsIgnoreCase("pdf"))
            return;
        File abbyyXmlOutput = getAbbyyXmlOutput(file);
        if (!abbyyXmlOutput.exists()) {
            logger.warn("No ABBYY XML output for file: " + file.getName());
            return;
        }
        logger.info("Indexing file: " + file.getName());
        ResearchDoc researchDoc = new ResearchDoc(file);
        researchDoc.extractInfoFromDOM();

        // here, modify the Document's preprocessed content with the parsed ABBYY output
        AbbyyXmlParser parsedAbbyy = new AbbyyXmlParser(abbyyXmlOutput);
        researchDoc.ppText = parsedAbbyy.toText();
        researchDoc.plainText = researchDoc.ppText;

        // proceed as usual for the indexation of paragraphs
        Document doc = researchDoc.constructDoc(docId, file.getName());

        writer.addDocument(doc);
        indexPara(researchDoc, docId, WINDOW_SIZE);

        docId++;
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

    private File getAbbyyXmlOutput(File pdfFile) {
        String xmlName = pdfFile.getName() + ".xml";
        return new File(extractedFilesFolder, xmlName);
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java PaperIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            PaperIndexer indexer = new PaperIndexerAbbyy(args[0]);
            indexer.processAll();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
}
