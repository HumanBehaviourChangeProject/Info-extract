/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import api.IUnitPOJO;
import extractor.InformationUnit;
import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import java.util.StringTokenizer;
import javax.json.Json;
import javax.json.JsonBuilderFactory;
import javax.json.JsonObject;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enables searching/loading of extracted values from the web service.
 * 
 * @author dganguly
 */
public class ExtractedInfoRetriever {
    Properties prop;
    IndexReader reader;
    IndexSearcher searcher;
    IndexReader docReader;
    IndexSearcher docSearcher;
    Analyzer analyzer;
    JsonBuilderFactory factory;
    
    static Logger logger = LoggerFactory.getLogger(PaperIndexer.class);
    
    /**
     * Called from the server side. Instead of using files, this constructor
     * uses 'resources' objects.
     */
    public ExtractedInfoRetriever() throws Exception {
        String indexPath = this.getClass().getClassLoader().getResource("indexes/ie.index").getPath();
        String stopFileName = this.getClass().getClassLoader().getResource("stop.txt").getPath();
        
        File indexDir = new File(indexPath);
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        searcher = new IndexSearcher(reader);
        
        // To retrieve per-doc info, e.g. title, authors etc.
        indexPath = this.getClass().getClassLoader().getResource("indexes/index").getPath();
        indexDir = new File(indexPath);
        docReader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        docSearcher = new IndexSearcher(docReader);        
        
        analyzer = PaperIndexer.constructAnalyzer(stopFileName);
        factory = Json.createBuilderFactory(null);
    }

    /**
     * Called from the stand-alone application to save the extracted info objects
     * that could later be loaded from the server side of the web application.
     * 
     * @param propFile A properties file
     */
    public ExtractedInfoRetriever(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));        

        String indexPath = prop.getProperty("ie.index");
        File indexDir = new File(indexPath);
        reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        searcher = new IndexSearcher(reader);
        
        // To retrieve per-doc info, e.g. title, authors etc.
        indexPath = prop.getProperty("index");
        indexDir = new File(indexPath);
        docReader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        docSearcher = new IndexSearcher(docReader);        
        
        String stopFileName = BaseDirInfo.getPath(prop.getProperty("stopfile"));
        analyzer = PaperIndexer.constructAnalyzer(stopFileName);        
        
        factory = Json.createBuilderFactory(null);
    }
    
    /**
     * Constructs a 'ResearchDoc' object from a given file name. The 'ResearchDoc'
     * object replicates the in-memory representation of a Lucene document object
     * comprising of fields, such as title, authors, introduction etc.
     * 
     * @param docName Name of the document
     * @return A 'ResearchDoc' object.
     * 
     */
    public ResearchDoc retrieveDocInfo(String docName) throws Exception {        
        // KeywordAnalyzer with phrase query to match the whole string because
        // this is saved un-analyzed.
        QueryParser parser = new QueryParser(ResearchDoc.FIELD_NAME, new KeywordAnalyzer());
        Query q = parser.parse("\"" + docName + "\"");
        
        ResearchDoc rdoc = null;
        Document doc = null;
        TopDocs topDocs = docSearcher.search(q, 1);
        if (topDocs!=null && topDocs.scoreDocs.length>0) {
            doc = docReader.document(topDocs.scoreDocs[0].doc);
            rdoc = new ResearchDoc(doc);
        }
        
        return rdoc;
    }
    
    /**
     * Retrieves a list of top ranked documents (passages) given an attribute id
     * to extract.
     * 
     * @param attribId
     * @return A 'TopDocs' object.
     */
    public TopDocs retrieve(String attribId) throws Exception {
        Query q = new TermQuery(new Term(InformationUnit.ATTRIB_ID_FIELD, attribId));
        return searcher.search(q, 1000);        
    }
    
    /**
     * Called from the web interface to obtain the HTML representation of a list
     * of extracted values.
     * 
     * @param attribId Attribute id to extract.
     * @return An HTML formatted string that is returned to a servlet implementation.
     */
    public String retrieveHTMLFormatted(String attribId) throws Exception {
        StringBuffer buff = new StringBuffer();
        
        TopDocs topDocs = retrieve(attribId);
        logger.info("Retrieved " + topDocs.scoreDocs.length + " records for query (attribute-id): '" + attribId + "'");
        
        for (ScoreDoc sd : topDocs.scoreDocs) {
            buff.append("<tr>");
            buff.append(formatIURecordHTML(sd.doc));
            buff.append("</tr>");
        }
        
        return buff.toString();
    }
    
    String formatIURecordHTML(int docId) throws Exception {
        StringBuffer buff = new StringBuffer("");
        Document d = reader.document(docId);
        
        buff.append("<td>");
        buff.append(d.get(InformationUnit.ATTRIB_ID_FIELD));
        buff.append("</td>");
        
        buff.append("<td>");
        buff.append(d.get(InformationUnit.ATTRIB_NAME_FIELD));
        buff.append("</td>");
        
        buff.append("<td>");
        String docName = d.get(InformationUnit.DOCNAME_FIELD);
        buff.append(docName);
        buff.append("</td>");
        
        buff.append("<td>");
        buff.append(d.get(InformationUnit.EXTRACTED_VALUE_FIELD));
        buff.append("</td>");

        // Append document specific information
        ResearchDoc rdoc = retrieveDocInfo(docName);
        
        if (rdoc!=null) {        
            buff.append("<td>");
            buff.append(rdoc.title);
            buff.append("</td>");
            buff.append("<td>");
            //buff.append(shortenAuthors(rdoc.authors));
            buff.append(rdoc.authors);
            buff.append("</td>");
            
            buff.append("<td>");
            buff.append(rdoc.unjudged==1? "no" : "yes");
            buff.append("</td>");
            
            /*
            buff.append("<td>");
            buff.append(rdoc.intro);
            buff.append("</td>");
            */
        }
        
        return buff.toString();
    }
    
    String shortenAuthors(String title) {
        int authorCount = 0;
        StringTokenizer st = new StringTokenizer(title, ",;");
        StringBuffer buff = new StringBuffer();
        
        while (st.hasMoreTokens() && authorCount < 1) {
            buff.append(st.nextToken());
            authorCount++;
        }
        
        if (buff.toString().trim().length()>0)
            buff.append(" et.al.");
        return buff.toString();
    }
    
    public void close() throws Exception {
        docReader.close();
        reader.close();
    }
}
