/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswer;
import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoIndexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.Paragraph;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.inforetrieval.indexer.SentenceBasedParagraphBuilder;
import com.ibm.drl.hbcp.inforetrieval.indexer.SlindingWindowParagraphBuilder;

import java.io.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import com.ibm.drl.hbcp.nb.DPAttribNBClassifier;
import com.ibm.drl.hbcp.inforetrieval.normrsv.NormalizedRSVRetriever;
import org.apache.commons.io.FileUtils;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.classification.SimpleNaiveBayesClassifier;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.flexible.core.QueryNodeException;
import org.apache.lucene.queryparser.flexible.standard.StandardQueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.RAMDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.JSONRefParser4Armification;
import com.ibm.drl.hbcp.core.wvec.WordVecs;

/**
 * This class extracts information from unstructured text indexed as fixed length word
 * window based paragraphs.
 * The extraction logic allows looping through a list of abstract information unit objects,
 * where each object 'knows' how to extract its candidate values from a given pdf document.
 *
 * @author Debasis, Yufang, Martin, Charles
 *
 */
public class InformationExtractor implements Closeable {

    IndexReader reader;
    Properties prop;
    Analyzer analyzer;
    WordVecs wvecs;

    JSONRefParser refBuilder;
    Map<String, Set<String>> docBySprint;
    Map<String, Set<String>> armInfo;
    Boolean armification;
    private String propFile;

    HashMap<Integer, IndexReader> paraReadersMap;
    DPAttribNBClassifier classifier;
    

    static Logger logger = LoggerFactory.getLogger(InformationExtractor.class);

    /**
     * Constructs an InformationExtractor object. This constructor is called
     * to initialize an extractor from the web interface back-end.
     */
    public InformationExtractor() throws Exception {
        analyzer = PaperIndexer.constructAnalyzer(this.getClass().getClassLoader().getResource("stop.txt").getPath());
        this.armification = Boolean.parseBoolean(prop.getProperty("armification"));
        prop = new Properties();
        prop.load(new FileReader(this.getClass().getClassLoader().getResource("init.properties").getPath()));

        if (Integer.parseInt(prop.getProperty("qe.nn", "0")) > 0)
            wvecs = new WordVecs(prop);

        paraReadersMap = new HashMap<>();
        armInfo = new HashMap();

        // Load the ground-truth
        loadGroundTruth();
    }

    public WordVecs getWordVecs() { return wvecs; }

    /**
     * Constructs an InformationExtractor object. This constructor is called
     * to initialize an extractor from the stand-alone application.
     *
     * @param propFile Path to the properties file.
     */    
    public InformationExtractor(String propFile) throws IOException {
        this.propFile = propFile;
        prop = new Properties();
        prop.load(new FileReader(propFile));
        this.armification = Boolean.parseBoolean(prop.getProperty("armification"));

        if (Integer.parseInt(prop.getProperty("qe.nn", "0")) > 0)
            wvecs = new WordVecs(prop);

        String indexPath = prop.getProperty("index");
        File indexDir = new File(indexPath);

        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        indexDir = new File(indexPath + "/para/");

        analyzer = PaperIndexer.constructAnalyzer(prop.getProperty("stopfile"));

        paraReadersMap = new HashMap<>();

        armInfo = new HashMap();
        // Load the ground-truth
        loadGroundTruth();
    }
    
    /**
     * Constructs an InformationExtractor object. This constructor is called
     * to initialize an extractor from the stand-alone application with a given properties object.
     *
     * @param prop Properties
     */
    public InformationExtractor(Properties prop) throws IOException {
        this.prop = prop;
        this.armification = Boolean.parseBoolean(prop.getProperty("armification"));
        
        if (Integer.parseInt(prop.getProperty("qe.nn", "0")) > 0)
            wvecs = new WordVecs(prop);

        String indexPath = prop.getProperty("index");
        File indexDir = new File(indexPath);
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        indexDir = new File(indexPath + "/para/");

        analyzer = PaperIndexer.constructAnalyzer(prop.getProperty("stopfile"));

        paraReadersMap = new HashMap<>();

        // Load the ground-truth
        armInfo = new HashMap();
        if (Boolean.parseBoolean(prop.getProperty("ie.buildgt", "true")))
            loadGroundTruth();
    }
 
 
     /**
     * Constructs an InformationExtractor object. This constructor is called
     * from the Swagger API interface initialization flow.
     *
     * @param reader IndexReader object of the Lucene index which stores the documents in the collection.
     */
    public InformationExtractor(IndexReader reader) throws IOException {
        this.reader = reader;
        analyzer = PaperIndexer.constructAnalyzer(this.getClass().getClassLoader().getResource("stop.txt").getPath());
        prop = new Properties();
        prop.load(new FileReader(this.getClass().getClassLoader().getResource("init.properties").getPath()));
        paraReadersMap = new HashMap<>();
    }

    // Get document id from document name
    int getDocIdFromName(String docName) throws IOException, QueryNodeException {

        StandardQueryParser qp = new StandardQueryParser(new KeywordAnalyzer());
        Query docNameQuery = qp.parse("\"" + docName + "\"", ResearchDoc.FIELD_NAME);        
        //TermQuery docNameQuery = new TermQuery(new Term(ResearchDoc.FIELD_ID, docName));

        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(docNameQuery, 1);
        return topDocs.scoreDocs.length>0? topDocs.scoreDocs[0].doc : -1;
    }

    void loadGroundTruth() throws IOException {
        if (armification){
            refBuilder = new JSONRefParser4Armification(prop);
            armInfo = ((JSONRefParser4Armification)refBuilder).getArmsInfo();
        }else{
            refBuilder = new JSONRefParser(prop);
        }

        refBuilder.buildAll();
        docBySprint = refBuilder.docsBySprint();
    }

    /**
     * Builds up a paragraph index during negative sampling of learning from pseudo-negative examples.
     * @param refDocId A given document id.
     * @param windowsSizes Window sizes
     * @return An in-memory index of passages which are similar to a query but not annotated.
     */    
    Directory buildParaIndex(int refDocId, String[] windowsSizes) throws IOException {
        Directory ramdir = new RAMDirectory();                
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);

        // Form multiple variable length windows
        for (String ws : windowsSizes) {
            getSubsetToSearch(refDocId, writer, Integer.parseInt(ws));
        }

        writer.commit();
        writer.close();
        return writer.getDirectory();
    }

    // To be called during negative sampling.
    public Directory buildParaIndex(int refDocId, int ws) throws IOException {

        Directory ramdir = new RAMDirectory();                
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        IndexWriter writer = new IndexWriter(ramdir, iwcfg);

        // Form multiple variable length windows
        getSubsetToSearch(refDocId, writer, ws);

        writer.commit();
        writer.close();
        return writer.getDirectory();
    }

    void getSubsetToSearch(int refDocId, IndexWriter writer, int paraSize) throws IOException {

        String srcDocName = reader.document(refDocId).get(ResearchDoc.FIELD_ID);

        // construct a range query
        TermQuery docNameQuery = new TermQuery(new Term(ResearchDoc.FIELD_ID, srcDocName));
        IndexSearcher searcher = new IndexSearcher(reader);
        TopDocs topDocs = searcher.search(docNameQuery, 1000);

        ScoreDoc[] hits = topDocs.scoreDocs;
        for (ScoreDoc hit : hits) {
            addParagraphs(refDocId, writer, paraSize);
        }
    }

    void addParagraphs(int docId, IndexWriter paraWriter, int windowSize) throws IOException {

        //check whether we want to use sentenceBased paragraphs
        boolean useSentenceBased= Boolean.parseBoolean(prop.getProperty("use.sentence.based", "false"));
        int paraNumberOfSentences= Integer.parseInt(prop.getProperty("para.number.of.sentences", "1"));

        SentenceBasedParagraphBuilder builderSB= new SentenceBasedParagraphBuilder(paraNumberOfSentences, analyzer);
        SlindingWindowParagraphBuilder builder= new SlindingWindowParagraphBuilder(windowSize, analyzer);

        String content = reader.document(docId).get(ResearchDoc.FIELD_CONTENT);
        // Write out paragraphs...
        List<Paragraph> paragraphs = builder.constructParagraphs(docId, content);

        //change here in a less consuming way
        if (useSentenceBased)
            paragraphs = builderSB.constructParagraphs(docId, content);


        for (Paragraph p : paragraphs) {
            Document doc = new Document();
            doc.add(new Field(ResearchDoc.FIELD_ID, p.id, Field.Store.YES, Field.Index.NOT_ANALYZED));
            doc.add(new Field(ResearchDoc.FIELD_CONTENT, p.content,
                    Field.Store.YES, Field.Index.ANALYZED, Field.TermVector.WITH_POSITIONS));

            logger.debug(p.content);

            paraWriter.addDocument(doc);
        }        
    }

    /**
     * Extract information pertaining to the specified information unit object 'iu'.
     * @param docId Document from which to extract information.
     * @param iu Contains information o which attribute to extract.
     * @param wsizes A comma separated list of different window sizes to employ for passage retrieval.
     * @return An 'InformationUnit' object populated with the extracted value.
    */    
    public InformationUnit extractInformationFromDoc(int docId, InformationUnit iu, String[] wsizes) throws ParseException, IOException {
        IndexReader paraReader = getParaReader(docId, wsizes);
        return extractInformationFromDoc(paraReader, docId, iu, false);
    }

    /**
     * Extract information pertaining to the specified information unit object 'iu'. Reads the window
     * sizes to be used from the properties file.
     * @param docId Document from which to extract information.
     * @param iu Contains information o which attribute to extract.
     * @return An 'InformationUnit' object populated with the extracted value.
     */    
    public InformationUnit extractInformationFromDoc(int docId, InformationUnit iu) throws ParseException, IOException {
        String[] windowsSizes = prop.getProperty("window.sizes").split(",");        
        return extractInformationFromDoc(docId, iu, windowsSizes);
    }

    /**
     * Extract information pertaining to the specified information unit object 'iu'. Reads the window
     * sizes to be used from the properties file.
     *
     * Performs, in general, the following actions.
     *
     * <ol>
     * <li> Constructs a query from the 'iu' attribute. Whether or not the query is learned
     * from the annotations or simply read from the configuration file, depends on the property values
     * and also on the way the 'iu' parameter is configured. </li>
     * <li>Constructs a Lucene 'IndexSearcher' object. Uses 'LMJelinekMercerSimilarity' retrieval model for extracting top-ranked passages.  </li>
     * <li> Normalize the retrieval model scores for a consistent threshold computation.</li>
     * <li> For value extraction type attributes, reranks top ranked passages based on the distances of the candidate answers from the query terms. </li>
     * <li> Aggregates the similarity scores across multiple retrieved passages. </li>
     * </ol>
     *
     * @param paraReader An IndexReader Lucene object pointing to the index where the passages are stored.
     * @param docId Document from which to extract information.
     * @param iu Contains information o which attribute to extract.
     * @param applyTextClassifier Whether to apply a trained text classifier (Naive Bayes) to predict the output (Note:
     * This only applies for BCT extraction and the calling function has to make sure that the value of the 'iu' parameter is
     * of type 'AbstractDetectPresenceAttribute'.
     * @return An 'InformationUnit' object populated with the extracted value.
    */    
    public InformationUnit extractInformationFromDoc(IndexReader paraReader, int docId, InformationUnit iu, boolean applyTextClassifier) throws ParseException, IOException {
        String docName = reader.document(docId).get(ResearchDoc.FIELD_NAME);                    
        iu.reInit(docName);

        Query q = iu.constructQuery();

        IndexSearcher paraSearcher = new IndexSearcher(paraReader);
        Similarity sim = new LMJelinekMercerSimilarity(0.6f);
        paraSearcher.setSimilarity(sim);

        TopDocs topDocs = paraSearcher.search(q, applyTextClassifier? 1 : iu.numWanted());  // for classification flow retrieve just the top-most
        if (topDocs.scoreDocs.length == 0) {
            logger.info("No information found in document " + docName + "(" + docId + ") for query " + q.toString());
            return null;
        }

        // Normalize the retrieval model scores to ensure that we don't need to
        // compute cosine similarity inside the construct() function
        // of AbstractDetectPresenceAttribute.
        NormalizedRSVRetriever normalizer = new NormalizedRSVRetriever(paraSearcher, ResearchDoc.FIELD_CONTENT);
        topDocs = normalizer.rerank(topDocs);

        // variable aggregation flow depending on the type of attribute
        InformationUnits iunits = new InformationUnits();

        for (int i=0; i < topDocs.scoreDocs.length; i++) {
            ScoreDoc sd = topDocs.scoreDocs[i];

            Document retrievedWindow = paraReader.document(sd.doc);
            String content = retrievedWindow.get(ResearchDoc.FIELD_CONTENT);

            iu.construct(content, q, sd.score);

            if (classifier!=null && applyTextClassifier) {  // to apply NB classifier - override threshold-based classification
                String retrievedText = PaperIndexer.analyze(analyzer, content);
                int rel = classifier.getPrediction(retrievedText);  // result of binary classification
                iu.mostLikelyAnswer = rel>0? new CandidateAnswer(retrievedText, 0, content) : null;
            }

            iunits.aggregate(iu, i, sd.score);                
        }

        InformationUnit predicted = null;
        if(this.armification){
            predicted = iunits.getArmsPrediction();
        }else{
            predicted = iunits.getPredicted();
        }

        return predicted;
    }

    /**
     * Used in the stand-alone default flow. Values are taken from the properties file.
     * @param docId Document from which to extract information.
     * @param iu Contains information o which attribute to extract.
     * @param applyTextClassifier Whether to apply a trained text classifier (Naive Bayes) to predict the output (Note:
     * This only applies for BCT extraction and the calling function has to make sure that the value of the 'iu' parameter is
     * of type 'AbstractDetectPresenceAttribute'.
     * @return An 'InformationUnit' object populated with the extracted value.
     */    
    public InformationUnit extractInformationFromDoc(int docId, InformationUnit iu, boolean applyTextClassifier) throws ParseException, IOException {
        String[] windowsSizes = prop.getProperty("window.sizes").split(",");        
        return extractInformationFromDoc(docId, iu, windowsSizes, applyTextClassifier);
    }

    /**
     * Used in the API flow, where one wishes to override the values of window sizes from the interface.
     * Other configuration values are taken from the properties file.
     * @param docId Document from which to extract information.
     * @param iu Contains information o which attribute to extract.
     * @param wsizes A comma separated list of different window sizes to employ for passage retrieval.
     * @param applyTextClassifier Whether to apply a trained text classifier (Naive Bayes) to predict the output (Note:
     * This only applies for BCT extraction and the calling function has to make sure that the value of the 'iu' parameter is
     * of type 'AbstractDetectPresenceAttribute'.
     * @return An 'InformationUnit' object populated with the extracted value.
     */    
    public InformationUnit extractInformationFromDoc(int docId, InformationUnit iu, String[] wsizes, boolean applyTextClassifier) throws ParseException, IOException {
        IndexReader paraReader = getParaReader(docId, wsizes);
        return extractInformationFromDoc(paraReader, docId, iu, applyTextClassifier);
    }

    /**
     * Loops through all the documents and performs information extraction.
     * @param toExtract Contains information o which attribute to extract.
     * @param ieIndexer Used to save the extracted information as part of a Lucene index (functionality implemented in this object passed as parameter).
     */
    public void extractInformationIU(InformationUnit toExtract, ExtractedInfoIndexer ieIndexer) throws ParseException, IOException {

        boolean includedInSprint = false;
        int numDocs = reader.numDocs();
        String sprints = prop.getProperty("sprints", "1234");

        RefComparison aggregatedRC = new RefComparison();  // this is passed by reference and incremented in the called functions.
        logger.info("Extracting info for " + toExtract.getName());

        for (int i = 0; i < numDocs; i++) {
            String docName = reader.document(i).get(ResearchDoc.FIELD_NAME);

            includedInSprint = false;
            for (int j = 0; j < sprints.length(); j++) {
                // whether this doc is present in jth sprint
                String num = String.valueOf(sprints.charAt(j));
                Set<String> docsOfThisSprint = docBySprint.get("Sprint" + num );
                if (docsOfThisSprint.contains(docName) && sprints.contains(num)) {
                    includedInSprint = true;
                    logger.info(docName +"- SPRINT " + num);
                    break;
                }
            }
            if (!includedInSprint)
                continue;

            InformationUnit predicted = extractInformationFromDoc(i, toExtract, false);
            if (predicted!=null) {
                predicted.setProperties();                
                logger.info(predicted.docName + ": " + predicted.mostLikelyAnswer.getKey() +"----with weight "+predicted.weight);
            } else {
                logger.info(reader.document(i).get(ResearchDoc.FIELD_NAME)+": NO PREDICTION");
            }

            if (!armification)
                toExtract.compareWithRef(
                    this.refBuilder.getGroundTruths(toExtract.type.code()),
                    predicted, aggregatedRC, this.armification);
            else
                ((ArmsIdentifier)toExtract).compareWithRef1(
                    this.refBuilder.getGroundTruths(toExtract.type.code()),
                    predicted, aggregatedRC, this.armification, this.armInfo);
            
            // Mark to be saved
            if (predicted!=null && predicted.mostLikelyAnswer.getKey()!=null) {
                ieIndexer.addRecord(predicted);
            }
        }

        if (aggregatedRC.tp > 0)
            aggregatedRC.meteor /= (float)aggregatedRC.tp;  // normalize by true-positives

        toExtract.setEvalMetric(aggregatedRC);   
    }


    /**
     * Walk through the list of information units, extract and evaluate each in turn
     * @throws ParseException
     * @throws IOException 
     */
    void extractInformation() throws ParseException, IOException {

        InformationExtractorFactory ieFactory = new InformationExtractorFactory(this);
        List<InformationUnit> listToExtract = ieFactory.createIUnits();
        
        // For saving the extracted information in an index...
        // To be used by the web interface and also to be viewed
        // independently of this application.
        ExtractedInfoIndexer ieIndexer = new ExtractedInfoIndexer(getPropFile());

        for (InformationUnit toExtract : listToExtract) { 
            extractInformationIU(toExtract, ieIndexer);                        
        }

        printSummaryEval(listToExtract, false);

        ieIndexer.close();
    }

    void printSummaryEval(List<InformationUnit> iuList, boolean supervised) {
        String msg = supervised? "(supervised)" : "(unsupervised)";
        boolean useSentenceBased= Boolean.parseBoolean(prop.getProperty("use.sentence.based", "false"));
        int paraNumberOfSentences= Integer.parseInt(prop.getProperty("para.number.of.sentences", "1"));
        String ids = prop.getProperty("attributes.typedetect.ids");

        logger.info("Summary stats " + msg);
        logger.info("Parameters \n"
                + "use sentence based = "+ useSentenceBased+"\n"
                + "number Sentences= "+paraNumberOfSentences +"\n"
                + "attributes.typedetect.ids= "+ids);


        RefComparison meanAvgValuesDP = new RefComparison();  // mean (over attributes) of averages (over docs)  
        RefComparison meanAvgValuesVE = new RefComparison();  // mean (over attributes) of averages (over docs) 

        int numDP =0;
        int numVE =0;

        for (InformationUnit toExtract : iuList) {
            toExtract.printMetricValues();

            if (toExtract.typePresenceDetect) {
                numDP+=1;
                meanAvgValuesDP.accuracy += toExtract.getEval().accuracy;
                meanAvgValuesDP.precision += toExtract.getEval().precision;
                meanAvgValuesDP.recall += toExtract.getEval().recall;
                meanAvgValuesDP.fscore += toExtract.getEval().fscore;
                meanAvgValuesDP.meteor += toExtract.getEval().meteor;
            } else {
                numVE+=1;
                meanAvgValuesVE.precision += toExtract.getEval().precision;
                meanAvgValuesVE.recall += toExtract.getEval().recall;
                meanAvgValuesVE.fscore += toExtract.getEval().fscore;
            }
        }

        if (numVE>0) {
            System.out.println("Mean average values " + msg + " for VE Attributes : ");
            System.out.println("Precision = " + meanAvgValuesVE.precision/(float)numVE);
            System.out.println("Recall = " + meanAvgValuesVE.recall/(float)numVE);
            System.out.println("F-score = " + meanAvgValuesVE.fscore/(float)numVE);
        }

        if (numDP>0) {
            System.out.println("Mean average values " + msg + " for DP Attributes : ");
            System.out.println("Precision = " + meanAvgValuesDP.precision/(float)numDP +";");
            System.out.println("Recall = " + meanAvgValuesDP.recall/(float)numDP+";");
            System.out.println("F-score = " + meanAvgValuesDP.fscore/(float)numDP+";");
            System.out.println("Accuracy = " + meanAvgValuesDP.accuracy/(float)numDP+";");
            System.out.println("Avg-METEOR = " + meanAvgValuesDP.meteor/(float)numDP);
        }  
    }

    IndexReader getParaReader(int docId, String[] wsizes) throws IOException {
        IndexReader paraReader = paraReadersMap.get(docId);
        if (paraReader!=null)
            return paraReader;

        Directory inMemParaIndex = buildParaIndex(docId, wsizes);
        paraReader = DirectoryReader.open(inMemParaIndex);
        paraReadersMap.put(docId, paraReader);

        return paraReader;
    }

    void extractAll() throws Exception {  
        
        extractInformation();
        //extractInformationSupervised();
        // close all the indexreader objects
        for (IndexReader ir: paraReadersMap.values())
            ir.close();
    }

    // To be called from the REST API flow
    public void setClassifier(SimpleNaiveBayesClassifier classifier) {
        this.classifier = new DPAttribNBClassifier(classifier);
    }

    public RefComparison trainTest(InformationUnit toExtract, CVSplit cvsplit) throws Exception {

        RefComparison rc = new RefComparison();  // this is passed by reference and incremented in the called functions.

        int nTopTerms = Integer.parseInt(prop.getProperty("attributes.typedetect.supervised.ntopterms", "10"));
        float lambda = Float.parseFloat(prop.getProperty("attributes.typedetect.supervised.lambda", "0.5"));
        boolean varThresh = Boolean.parseBoolean(prop.getProperty("learn_threshold", "false"));

        // Learn query representation.
        toExtract.updateFromTrainingData(cvsplit, nTopTerms, lambda);

        // Check whether we want to train by Naive Bayes.
        boolean applyNB = Boolean.parseBoolean(prop.getProperty("bct.classifier", "false"));

        if (varThresh) {
            toExtract.learnThreshold(cvsplit, this);
        }
        else {
            ((AbstractDetectPresenceAttribute)toExtract).setThreshold();
        }

        if (applyNB) {
            // prepare for training by inputting the current fold
            classifier = new DPAttribNBClassifier(prop, cvsplit, toExtract);
            classifier.train();
        }        

        if (toExtract.query==null) {
            // couldn't learn the query representation...
            return rc;
        }

        logger.info("Testing with threshold value of " + ((AbstractDetectPresenceAttribute)toExtract).simThreshold);
        rc.threshold = ((AbstractDetectPresenceAttribute)toExtract).simThreshold;

        for (DocIdNamePair docIdNamePair: cvsplit.testDocs) {  // iterate over docs in test set
            int docId = docIdNamePair.id;
            InformationUnit predicted = extractInformationFromDoc(docId, toExtract, applyNB);
            if (predicted!=null) {
                predicted.setProperties();                
                logger.info(predicted.docName + ": " + predicted.mostLikelyAnswer + "(" + predicted.weight + ")");
            }

            toExtract.compareWithRef(
                    this.refBuilder.getGroundTruths(toExtract.type.code()),
                    predicted, rc, this.armification);
        }

        return rc;

    }

    /**
     * Runs cross-validation treating a part of the dataset for training and the other for testing.
     *
     * @param toExtract The attribute to extract. Note that the folds are different for each attribute depending on the number
     * of documents which are annotate with this attribute.
     * @param fg A 'CVFoldGenerator' object.
     * @param k The value of k for k-fold cross-validation (typically set to 5 or 10).
     * @return A 'RefComparison' object which accumulates the evaluation metrics.
     */    
    public RefComparison doFold(InformationUnit toExtract, CVFoldGenerator fg, int k) throws Exception {

        //Generates the associated train and test sets
        CVSplit cvsplit = new CVSplit(toExtract, fg, k);

        logger.info(k + "-th iteration in CV: Training with " + cvsplit.trainDocs.size() + ", testing with " + cvsplit.testDocs.size());

        RefComparison cvRC = trainTest(toExtract, cvsplit);

        // values will be aggregated here over the tp,fp,fn,tn counts
        logger.info("Results for CV-fold " + k + ": " +
                cvRC.computeAccuracyVal() + ", " +
                cvRC.computePrecisionVal()+ ", " +
                cvRC.computeRecallVal()+ ", " +
                cvRC.computeFscoreVal() + ", " +
                cvRC.computeMeteorWithGT() + ", (threshold: " +
                cvRC.threshold + ")"
                );

        return cvRC;
    }

    /**
     * Perform supervised extraction with cross-validations.
     *
     * @param toExtract Attribute to extract
     * @throws Exception
     */    
    public void ieSupervised(InformationUnit toExtract) throws Exception {

        logger.info("Extracting info for " + toExtract.getName());

        //START BY CHECKING THAT THERE IS AT LEAST TWO POSITIVE ANNOTATIONS (IF NOT WE ARE DOOMED)
        int numNotNullAnnotations = this.getNumNotNullAnnotations(toExtract);
        logger.info("There are "+ numNotNullAnnotations +" non null annotations for this attribute.");

        if (numNotNullAnnotations<2) {
            logger.info("There is not enough non null annotations to do supervised training - needs to be more than 2");
            return;
        }

        boolean doTrainTest = Boolean.valueOf(prop.getProperty("traintest.activate", "false"));

        if (doTrainTest) {
            //Read which Sprints to Use for Training and Testing
            String trainSprints = prop.getProperty("train.sprints", "12");
            String testSprints = prop.getProperty("test.sprints", "34");
            
            //Generate the cvSplit
            CVSplit cvsplit = new CVSplit(toExtract, trainSprints,testSprints);
            RefComparison ttRC = trainTest(toExtract,cvsplit);
            ttRC.compute(); // Calculates Precision Recall and so on based on tp. tn ./..

            toExtract.setEvalMetric(ttRC);
        } else {
            crossValidation(toExtract);
        }
    }

    void crossValidation(InformationUnit toExtract) throws Exception {
        int numDocs = reader.numDocs();
        // Parameters for (semi-)supervised classification...
        int foldSize = Integer.parseInt(prop.getProperty("attributes.typedetect.supervised.cv", "5"));  // cvfold size is the size of a fold

        RefComparison aggregatedRC = new RefComparison();  // this is passed by reference and incremented in the called functions.

        //Generate the Folds
        CVFoldGenerator fg = new CVFoldGenerator(foldSize);
        fg.generateFolds(numDocs, toExtract);

        for (int k=0; k < foldSize; k++) {  // iterate k Cross Validation
            RefComparison cvRC = doFold(toExtract, fg, k);

            aggregatedRC.accuracy += cvRC.computeAccuracyVal();
            aggregatedRC.precision += cvRC.computePrecisionVal();
            aggregatedRC.recall += cvRC.computeRecallVal();
            aggregatedRC.fscore += cvRC.computeFscoreVal();
            aggregatedRC.meteor += cvRC.computeMeteorWithGT();
            aggregatedRC.threshold += cvRC.threshold; 
        }

        // Average over folds
        aggregatedRC.accuracy /= (float)foldSize;
        aggregatedRC.precision /= (float)foldSize;
        aggregatedRC.recall /= (float)foldSize;
        aggregatedRC.fscore /= (float)foldSize;
        aggregatedRC.meteor /= (float)foldSize;
        aggregatedRC.threshold /= (float)foldSize;

        toExtract.setEvalMetric(aggregatedRC);
    }

    /**
     * Walk through the list of information units, extract and evaluate each in turn.
     * We don't save the results for the GUI. This is only for internal use.        
     * @throws Exception 
     */
    void extractInformationSupervised() throws Exception {

        InformationExtractorFactory ieFactory = new InformationExtractorFactory(this);
        List<InformationUnit> listToExtract = ieFactory.createSupervisedIUnits();

        for (InformationUnit toExtract : listToExtract) {
            ieSupervised(toExtract);
        }

        printSummaryEval(listToExtract, true);        
    }


    public int getNumNotNullAnnotations(InformationUnit iu) throws IOException {
        int numNotNullAnnotations = 0;

        for (int i=0;i<reader.numDocs();++i) {
            String docName = reader.document(i).get(ResearchDoc.FIELD_NAME);                    
            iu.reInit(docName);
            if (iu.hasPositiveAnnotation()) {
                numNotNullAnnotations+=1;
            }

        }

        return numNotNullAnnotations ;

    }

    public void close() throws IOException {
        reader.close();
    }

    public void removeIEIndexFiles() throws IOException {
        File indexDirDocs = new File(prop.getProperty("ie.index"));
        if (indexDirDocs.exists())
            FileUtils.deleteDirectory(indexDirDocs);
    }

    public static void main(String[] args) {

        if (args.length == 0) {
            System.err.println("Usage: java InformationExtractor <prop-file>");
            args = new String[1];
            args[0] = "init.properties";
        }

        try {
            InformationExtractor ie = new InformationExtractor(args[0]);            
            ie.extractAll();            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public String getPropFile() {
        return propFile;
    }
}
