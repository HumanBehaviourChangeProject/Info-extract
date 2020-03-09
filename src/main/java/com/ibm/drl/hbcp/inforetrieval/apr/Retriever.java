/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.extractor.InformationExtractor;
import com.ibm.drl.hbcp.extractor.InformationExtractorFactory;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;

/**
 *
 * @author dganguly
 */
public class Retriever {

    Properties prop;
    IndexSearcher searcher;
    IndexReader reader;
    
    InformationExtractor ie;
    List<InformationUnit> iuList;
    String runName;
    String resultFile;
    Analyzer analyzer;
    
    GroundTruthBuilder gtb;
    
    int mode;
    int ntopterms;
    int nwanted;
    
    static final String[] modeNames = {
        "content",
        "ie",
        "supervised"
    };
    
    static final int NONE = -1;
    static final int CONTENT = 0;
    static final int IE = 1;
    static final int SUPERVISED = 2;
    static Logger logger = LoggerFactory.getLogger(Retriever.class);
    
    int getMode() {
        int modetype;
        String mode = prop.getProperty("apr.mode");
        modetype = Arrays.binarySearch(modeNames, mode);
        if (!(0 <= modetype && modetype < modeNames.length))
            modetype = NONE;
        
        return modetype;
    }
    
    public Analyzer getAnalyzer() { return analyzer; }
    public IndexReader getReader() { return reader; }
    public int numTopTerms() { return ntopterms; }
    
    final void createExtractor() throws Exception {
        prop.setProperty("extract.context", "false");
        prop.setProperty("extract.outcome", "false");
        
        prop.setProperty("ie.buildgt", "false");
        ie = new InformationExtractor(prop);
        InformationExtractorFactory ieFactory = new InformationExtractorFactory(ie);
        iuList = ieFactory.createIUnits();
    }
    
    public List<InformationUnit> getIUList() { return iuList; }
    public InformationExtractor getExtractor() { return ie; }
    
    public Retriever(String propFileName) throws Exception {

        prop = new Properties();
        prop.load(new FileReader(new File(propFileName)));
        
        reader = DirectoryReader.open(FSDirectory.open(new File(prop.getProperty("apr.index")).toPath()));
        searcher = new IndexSearcher(reader);
        searcher.setSimilarity(new LMJelinekMercerSimilarity(0.5f));
        
        this.mode = getMode();
        //analyzer = PaperIndexer.constructAnalyzer(prop);        
        analyzer = new ParagraphAnalyzer();
        ntopterms = Integer.parseInt(prop.getProperty("apr.content.ntopterms"));
        runName = prop.getProperty("runname", "thisrun"); // a place-holder for TREC formatted runs

        // create the com.ibm.drl.hbcp.extractor and the list of attributes to extract
        // only extract interventions
        createExtractor();
        
        nwanted = Integer.parseInt(prop.getProperty("apr.numwanted", "10"));
        resultFile = prop.getProperty("apr.result.file");
        logger.info("Writing results file at " + resultFile);
        
        gtb = new GroundTruthBuilder(propFileName);
        gtb.build();
        if (Boolean.parseBoolean(prop.getProperty("apr.buildqrels", "false")))
            gtb.saveQrels();
    }
    
    String getText(int docId) throws Exception {
        String text = reader.document(docId).get(ParagraphIndexer.APR_PARA_CONTENT_FIELD);
        text = PaperIndexer.analyze(analyzer, text);
        return text;
    }
    
    // Generate TREC formatted o/p
    String reportTopDocs(int qid, TopDocs topDocs) throws Exception {
        StringBuffer buff = new StringBuffer();
        Document query = reader.document(qid);
    
        int rank = 0;
        
        for (ScoreDoc sd: topDocs.scoreDocs) {
            if (rank == 0) {
                rank++;
                continue; // skip the first doc which is the query itself
            }
            
            Document doc = reader.document(sd.doc);
            buff
                .append(query.get(ParagraphIndexer.APR_ID_FIELD))
                .append("\tQ0\t")
                .append(doc.get(ParagraphIndexer.APR_ID_FIELD))
                .append("\t")
                .append(rank)
                .append("\t")
                .append(sd.score)
                .append("\t")
                .append(runName)
                .append("\n")
            ;
            rank++;
        }
        return buff.toString();
    }
    
    public String retrieve(int queryId) throws Exception {
        Query bq;
        QueryFormulator qf;
        
        switch (mode) {
            case CONTENT:
                qf = new BoWQueryFormulator(getText(queryId), this);
                break;
            case IE:
                qf = new ExtractedInfoQueryFormulator(this, queryId);
                break;
            default:
                return null;
        }
        bq = qf.constructQuery();
        logger.debug("Query: " + bq);
        
        TopDocs topdocs = searcher.search(bq, nwanted);
        return reportTopDocs(queryId, topdocs);
    }

    public void retrieveAll() throws Exception {
        FileWriter fw = new FileWriter(resultFile);
        BufferedWriter bw = new BufferedWriter(fw);
        
        HashSet<ParagraphVec> qvecs = gtb.getQueries();
        for (ParagraphVec qvec: qvecs) {
            logger.info("Retrieving similar passages for query: " + qvec.toString());
            String results = retrieve(qvec.id); // global unique identifier for this passage (document) in the index
            bw.write(results);
        }
        bw.close();
        fw.close();
    }
    
    public static void main(String[] args) throws Exception {
        String propFileName = "init.properties";
        if (args.length > 0) propFileName = args[0];
        Retriever retriever = new Retriever(propFileName);
        retriever.retrieveAll();
    }
    
}
