package com.ibm.drl.hbcp.experiments.lrec20;

import com.ibm.drl.hbcp.core.attributes.ExtractedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.pdf.grobid.OpenAccessDatasetGeneratorForLREC2020;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

public class OpenAccessIndexer extends PaperIndexer {

    private static Logger logger = LoggerFactory.getLogger(OpenAccessIndexer.class);
    private final Collection<String> oaFilenames;

    /**
     * Constructs the object by taking as an argument the relative path of a
     * properties file.
     *
     * @param propFile Relative path (from the project base) to the a properties file.
     * @param oaFilenames Open access filenames
     */
    public OpenAccessIndexer(String propFile, Collection<String> oaFilenames) throws IOException {
        super(propFile);
        this.oaFilenames = oaFilenames;
    }

    @Override
    protected void indexDirectory(File dir) throws SAXException, IOException {
        logger.info("Indexing directory " + dir);
        File[] files = dir.listFiles();
        if (files == null) {
            return;
        }
        Arrays.sort(files);

        for (File f : files) {
            if (f.isDirectory()) {
                logger.info("Indexing directory " + f.getName());
                indexDirectory(f);  // recurse
            }
            else
                if (oaFilenames.contains(f.getName())) {
                    try {
                        indexFile(f);
                    } catch (TikaException e) {
//                        e.printStackTrace();
                        System.err.println(e.getMessage());
                        System.err.println("Skipping " + f.getName() + " (couldn't parse and index PDF)...");
                    }
                }
        }
    }


    public static void main(String[] args) {
        try {
            String openAccessJsonsPath = "data/lrec2020/openaccesspapers_extracted_humanreadable";
            JSONRefParser annotations = new JSONRefParser(new File("data/jsons/SmokingPapers407_19Nov19.json"));
            final AttributeValueCollection<AnnotatedAttributeValuePair> annotatedAttributeValuePairs = OpenAccessDatasetGeneratorForLREC2020.openAccessDataset(openAccessJsonsPath, annotations);
            final Collection<String> oaFilenames = annotatedAttributeValuePairs.stream().map(ExtractedAttributeValuePair::getDocName).collect(Collectors.toSet());
            final OpenAccessIndexer openAccessIndexer = new OpenAccessIndexer("src/main/java/com/ibm/drl/hbcp/experiments/lrec20/lrec.properties", oaFilenames);
            openAccessIndexer.processAll();

        } catch (IOException | TikaException | SAXException e) {
            e.printStackTrace();
        }
    }

}
