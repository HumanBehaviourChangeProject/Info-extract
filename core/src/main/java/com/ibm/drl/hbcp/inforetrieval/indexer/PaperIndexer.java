package com.ibm.drl.hbcp.inforetrieval.indexer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;

import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.tika.exception.TikaException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.xml.sax.SAXException;

import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.LuceneField;
import com.ibm.drl.hbcp.util.Props;

/**
 * This class is used to construct an in-memory index from a pdf document.
 * It's used both in the stand-alone application flow and also in the REST API
 * based flow.
 *
 * @author Debasis
 */
public class PaperIndexer extends AbstractIndexer {
    IndexWriter writer;
    IndexWriter paraWriter;
    Analyzer analyzer;
    String paraIndexDir;
        
    static final String EOS = " eos1 ";  // EOS would be stemmed to EO!
    static int docId = 1;
    
    // Only one paragpah size to be used
    // to retrieve from the web interface. This index is stored in
    // para.index.all.
    static final int WINDOW_SIZE = 15;
    
    static private Logger logger = LoggerFactory.getLogger(PaperIndexer.class);
    
    /**
     * Constructs the object by taking properties.
     * @param props properties.
     */    
    public PaperIndexer(Properties props) throws IOException {
        super(props);
        indexDir = BaseDirInfo.getPath(prop.getProperty("index"));
        paraIndexDir = BaseDirInfo.getPath(prop.getProperty("para.index.all"));
        analyzer = constructAnalyzer(BaseDirInfo.getPath(prop.getProperty("stopfile")));
        
        createIndexDirs();
    }

    /**
     * Constructs the object by taking as an argument the relative path of a
     * properties file.
     * @param propFile Relative path (from the project base) to the a properties file.
     */
    public PaperIndexer(String propFile) throws IOException {
        this(Props.loadProperties(propFile));
    }

    /**
     * Return the Properties object that was used to initialize this instance of
     * the 'PaperIndexer' object.
     * @return
     */    
    public Properties getProperties() { return prop; }
    
    /**
     * Builds a list of stopwords from a specified stop file path (each line being a
     * stopword in the file). These words are not stored in the index and hence are
     * not used for similarity computation.
     * @param stopwordFileName The relative path name to the stopword file.
     * @return A list of the stopword terms read from the file.
     */    
    static public List<String> buildStopwordList(String stopwordFileName) {
        List<String> stopwords = new ArrayList<>();
        String line;

        try (FileReader fr = new FileReader(FileUtils.potentiallyGetAsResource(new File(stopwordFileName)));
             BufferedReader br = new BufferedReader(fr)) {
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return stopwords;
    }
    
    /**
     * Constructs a Lucene 'Analyzer' object. The analyzer object used currently
     * in the code flow is the 'EnglishAnalyzer' that stems words in addition to
     * removing stopwords.
     * @param prop Uses the default stopfile of the project to construct an Analyzer object.
     * @return An EnglishAnalyzer object.
     */    
    public static Analyzer constructAnalyzer(Properties prop) {
        return constructAnalyzer(BaseDirInfo.getPath(prop.getProperty("stopfile")));
    }
    
    /**
     * Constructs a Lucene 'Analyzer' object. The analyzer object used currently
     * in the code flow is the 'EnglishAnalyzer' that stems words in addition to
     * removing stopwords.
     * @param stopFileName The relative path name to the stopword file.
     * @return 
     */
    public static Analyzer constructAnalyzer(String stopFileName) {
        Analyzer eanalyzer = new EnglishAnalyzer(
            StopFilter.makeStopSet(buildStopwordList(stopFileName))); // default analyzer
        return eanalyzer;        
    }
    
    /**
     * Constructs an analyzed (pre-processed) string after removing stopwords and
     * stemming each constituent word of a given string.
     *
     * @param analyzer The analyzer object that is to be used to perform pre-processing.
     * @param query The string that is to be analyzed.
     * @return A pre-processed string.
    */    
    public static String analyze(Analyzer analyzer, String query) {

        StringBuffer buff = new StringBuffer();
        try {
            TokenStream stream = analyzer.tokenStream("dummy", new StringReader(query));
            CharTermAttribute termAtt = stream.addAttribute(CharTermAttribute.class);
            stream.reset();
            while (stream.incrementToken()) {
                String term = termAtt.toString();
                buff.append(term).append(" ");
            }
            stream.end();
            stream.close();

            if (buff.length()>0)
                buff.deleteCharAt(buff.length()-1);
        }
        catch (Exception ex) {
            ex.printStackTrace();
            return null;
        }

        return buff.toString();
    }
    
    /**
     * Create three index directories under the src/main/resources folder of the project.
     * @throws IOException 
     */
    public void createIndexDirs() throws IOException {
        createIndexDir(indexDir);
        createIndexDir(paraIndexDir);
        createIndexDir(prop.getProperty("para.index"));
    }
    
    /**
     * Removes the index folders for clean up.
     * @throws IOException 
     */
    public void removeIndexDirs() throws IOException {
        removeIndexDir(indexDir);
        removeIndexDir(paraIndexDir);
        removeIndexDir(prop.getProperty("para.index"));
    }
    
    /**
     * Recursively traverses the file system specified by the relative path
     * value of the property name 'coll'.
     * @throws TikaException
     * @throws SAXException
     * @throws IOException 
     */
    public void processAll() throws TikaException, SAXException, IOException {
        logger.info("Indexing PubMed collection...");
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        File indexDirDocs = new File(indexDir);
        try (Directory indexDir = FSDirectory.open(indexDirDocs.toPath())) {
            writer = new IndexWriter(indexDir, iwcfg);

            iwcfg = new IndexWriterConfig(analyzer);
            iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
            try (Directory indexParaDir = FSDirectory.open(new File(paraIndexDir).toPath())) {
                paraWriter = new IndexWriter(indexParaDir, iwcfg);

                indexAll();

                paraWriter.close();
            } finally {
                writer.close();
            }
        }
    }

    void indexAll() throws TikaException, SAXException, IOException {
        if (writer == null) {
            logger.info("Skipping indexing... Index already exists at " + indexDir + "!!");
            return;
        }
        
        docId = 1;
        String filePath = BaseDirInfo.getBaseDir();
        File topDir = new File(filePath.concat(prop.getProperty("coll")));
        topDir = FileUtils.potentiallyGetAsResource(topDir);
        indexDirectory(topDir);
    }

    protected void indexDirectory(File dir) throws TikaException, SAXException, IOException {
        logger.info("Indexing directory " + dir);
        File[] files = dir.listFiles();
        Arrays.sort(files);

        for (int i=0; i < files.length; i++) {
            File f = files[i];
            if (f.isDirectory()) {
                logger.info("Indexing directory " + f.getName());
                indexDirectory(f);  // recurse
            }
            else
                indexFile(f);
        }
    }
    
    /**
     * Indexes a particular pdf file.
     * @param file The relative path name of the pdf file.
     */
    public void indexFile(File file) throws TikaException, SAXException, IOException {

        String extension = FilenameUtils.getExtension(file.getName());
        if (!extension.equalsIgnoreCase("pdf"))
            return;

        logger.info("Indexing file: " + file.getName());
        try {
            ResearchDoc researchDoc = new ResearchDoc(file);
            researchDoc.extractInfoFromDOM();

            Document doc = researchDoc.constructDoc(docId, file.getName());
            writer.addDocument(doc);

            indexPara(researchDoc, docId, WINDOW_SIZE);

            docId++;
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    /**
     * Builds a paragraph index from one particular document. This paragraph index
     * is stored in the path specified by the property 'para.index'. The reason paragraphs
     * are also stored as a part of the file system is mainly for debugging purposes.
     * @param researchDoc Document to process.
     * @param docId id of the current document (starts at 0).
     * @param windowSize size of a paragraph (retrievable unit) in terms of number of words.
     */    
    public void indexPara(ResearchDoc researchDoc, int docId, int windowSize) throws IOException {
        // Paragraph index writer - one folder for each document
        String paraIndex = BaseDirInfo.getPath(prop.getProperty("para.index") + "/" + docId);
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);        
        File indexDirPara = new File(paraIndex);
        try (IndexWriter paraWriter = new IndexWriter(FSDirectory.open(indexDirPara.toPath()), iwcfg)) {
            //check whether we want to use sentenceBased paragraphs
            boolean useSentenceBased= Boolean.parseBoolean(prop.getProperty("use.sentence.based", "false"));
            int paraNumberOfSentences= Integer.parseInt(prop.getProperty("para.number.of.sentences", "1"));


            SentenceBasedParagraphBuilder builderSB= new SentenceBasedParagraphBuilder(paraNumberOfSentences, analyzer);
            SlidingWindowParagraphBuilder builder= new SlidingWindowParagraphBuilder(windowSize, analyzer);

            //SlidingWindowParagraphBuilder builder;
            //builder = new SlidingWindowParagraphBuilder(windowSize, analyzer);

            //FB
            //SentenceBasedParagraphBuilder builder = new SentenceBasedParagraphBuilder(windowSize, analyzer);

            // Write out paragraphs...

            List<Paragraph> paragraphs = builder.constructParagraphs(docId, researchDoc.ppText);
            //change here in a less consuming way
            if (useSentenceBased)
                paragraphs = builderSB.constructParagraphs(docId, researchDoc.ppText);

            for (Paragraph p : paragraphs) {
                Document doc = new Document();
                doc.add(new Field(ResearchDoc.FIELD_ID, p.id, LuceneField.STORED_NOT_ANALYZED.getType()));
                doc.add(new Field(ResearchDoc.FIELD_NAME, researchDoc.file.getName(), LuceneField.STORED_NOT_ANALYZED.getType()));
                doc.add(new Field(ResearchDoc.FIELD_TITLE, researchDoc.title, LuceneField.STORED_NOT_ANALYZED.getType()));
                doc.add(new Field(ResearchDoc.FIELD_CONTENT, p.content,
                        new LuceneField().stored(true).analyzed(true).termVectorsWithPositions().getType()));

                paraWriter.addDocument(doc);
                this.paraWriter.addDocument(doc); // store in the global para index
            }
        }
    }

    public static void ensure(Properties props) throws IOException, ParseException, TikaException, SAXException {
        File indexFolder = new File(BaseDirInfo.getPath(props.getProperty("index")));
        File paraIndexFolder = new File(BaseDirInfo.getPath(props.getProperty("para.index.all")));
        if (needsToRun(indexFolder) || needsToRun(paraIndexFolder)) {
            logger.info("Now indexing the papers...");
            PaperIndexer indexer = new PaperIndexer(props);
            indexer.processAll();
            logger.info("Indexing of the papers done.");
        }
    }

    private static boolean needsToRun(File indexFolder) {
        if (!indexFolder.exists()) {
            return true;
        } else {
            File[] contents = indexFolder.listFiles();
            return contents == null || contents.length == 0;
        }
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java PaperIndexer <prop-file>");
            args[0] = "init.properties";
        }

        try {
            PaperIndexer indexer = new PaperIndexer(args[0]);
            indexer.processAll();            
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    
}
