package indexer;

/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 * This class is used to construct an in-memory index from a pdf document.
 * It's used both in the stand-alone application flow and also in the REST API
 * based flow.
 * 
 * @author Debasis
 */

import java.io.*;
import java.util.*;
import org.apache.commons.io.FilenameUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.core.StopFilter;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.tokenattributes.CharTermAttribute;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class PaperIndexer extends AbstractIndexer {
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
    
    static Logger logger = LoggerFactory.getLogger(PaperIndexer.class);
    
    /**
     * Constructs the object by taking as an argument the relative path of a 
     * properties file.
     * @param propFile Relative path (from the project base) to the a properties file.
     */
    public PaperIndexer(String propFile) throws Exception {
        super(propFile);
        indexDir = BaseDirInfo.getPath(prop.getProperty("index"));
        paraIndexDir = BaseDirInfo.getPath(prop.getProperty("para.index.all"));
        analyzer = constructAnalyzer(BaseDirInfo.getPath(prop.getProperty("stopfile")));
        
        createIndexDirs();
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
    static protected List<String> buildStopwordList(String stopwordFileName) {
        List<String> stopwords = new ArrayList<>();
        String line;

        try (FileReader fr = new FileReader(stopwordFileName);
            BufferedReader br = new BufferedReader(fr)) {
            while ( (line = br.readLine()) != null ) {
                stopwords.add(line.trim());
            }
            br.close();
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
     * @param stopwordFileName The relative path name to the stopword file.
     * @return An EnglishAnalyzer object.
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
     * @param text The string that is to be analyzed.
     * @return A pre-processed string. 
     */
    public static String analyze(Analyzer analyzer, String text) {
        
        StringBuffer buff = new StringBuffer();        
        try {
            TokenStream stream = analyzer.tokenStream("dummy", new StringReader(text));
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
     */
    public void createIndexDirs() throws Exception {
        createIndexDir(indexDir);
        createIndexDir(paraIndexDir);
        createIndexDir(prop.getProperty("para.index"));
    }

    /**
     * Removes the index folders for clean up.
     */
    public void removeIndexDirs() throws Exception {
        removeIndexDir(indexDir);
        removeIndexDir(paraIndexDir);
        removeIndexDir(prop.getProperty("para.index"));
    }
    
    /**
     * Recursively traverses the file system specified by the relative path
     * value of the property name 'coll'.
     */
    public void processAll() throws Exception {
        logger.info("Indexing PubMed collection...");
        
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        File indexDirDocs = new File(indexDir);        
        writer = new IndexWriter(FSDirectory.open(indexDirDocs.toPath()), iwcfg);
        
        iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);        
        indexDirDocs = new File(paraIndexDir);
        paraWriter = new IndexWriter(FSDirectory.open(indexDirDocs.toPath()), iwcfg);

        indexAll();        
        
        writer.close();
        paraWriter.close();
    }

    void indexAll() throws Exception {
        if (writer == null) {
            logger.info("Skipping indexing... Index already exists at " + indexDir + "!!");
            return;
        }
        
        docId = 1;
        String filePath = BaseDirInfo.getBaseDir();
        File topDir = new File(filePath.concat(prop.getProperty("coll")));
        indexDirectory(topDir);
    }

    private void indexDirectory(File dir) throws Exception {
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
    void indexFile(File file) throws Exception {

        String extension = FilenameUtils.getExtension(file.getName());
        if (!extension.equalsIgnoreCase("pdf"))
            return;
        
        logger.info("Indexing file: " + file.getName());        
        ResearchDoc researchDoc = new ResearchDoc(file);
        researchDoc.extractInfoFromDOM();
        
        Document doc = researchDoc.constructDoc(docId, file.getName());
        writer.addDocument(doc);
        
        indexPara(researchDoc, docId, WINDOW_SIZE);
        
        docId++;
    }

    /**
     * Builds a paragraph index from one particular document. This paragraph index
     * is stored in the path specified by the property 'para.index'. The reason paragraphs
     * are also stored as a part of the file system is mainly for debugging purposes.
     * @param researchDoc Document to process.
     * @param docId id of the current document (starts at 0).
     * @param windowSize size of a paragraph (retrievable unit) in terms of number of words. 
     */
    void indexPara(ResearchDoc researchDoc, int docId, int windowSize) throws Exception {
        
        // Paragraph index writer - one folder for each document
        String paraIndex = BaseDirInfo.getPath(prop.getProperty("para.index") + "/" + docId);
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);        
        File indexDirPara = new File(paraIndex);
        IndexWriter paraWriter = new IndexWriter(FSDirectory.open(indexDirPara.toPath()), iwcfg);
        
        //check whether we want to use sentenceBased paragraphs
    	boolean useSentenceBased= Boolean.parseBoolean(prop.getProperty("use.sentence.based", "false"));
    	int paraNumberOfSentences= Integer.parseInt(prop.getProperty("para.number.of.sentences", "1"));

    	
    	SentenceBasedParagraphBuilder builderSB= new SentenceBasedParagraphBuilder(paraNumberOfSentences, analyzer);
    	SlindingWindowParagraphBuilder builder= new SlindingWindowParagraphBuilder(windowSize, analyzer);
    	        
    	List<Paragraph> paragraphs = builder.constructParagraphs(docId, researchDoc.ppText);
    	//change here in a less consuming way
        if (useSentenceBased)
            paragraphs = builderSB.constructParagraphs(docId, researchDoc.ppText);
        
        for (Paragraph p : paragraphs) {
            Document doc = new Document();
            doc.add(new Field(ResearchDoc.FIELD_ID, p.id, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(ResearchDoc.FIELD_NAME, researchDoc.file.getName(), Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(ResearchDoc.FIELD_TITLE, researchDoc.title, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(ResearchDoc.FIELD_CONTENT, p.content,
                    Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));
            
            paraWriter.addDocument(doc);
            this.paraWriter.addDocument(doc); // store in the global para index
        }
        paraWriter.close();
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
