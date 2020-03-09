/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import com.ibm.drl.hbcp.api.IUnitPOJO;
import com.ibm.drl.hbcp.api.IUnitPOJOs;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import com.ibm.drl.hbcp.util.Props;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.core.KeywordAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.store.FSDirectory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.json.Json;
import javax.json.JsonBuilderFactory;
import java.io.Closeable;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.*;

class IUnitPOJODocComparator implements Comparator<IUnitPOJO> {

    @Override
    public int compare(IUnitPOJO o1, IUnitPOJO o2) {
        return o1.getDocName().compareTo(o2.getDocName());
    }    
}

class IUnitPOJOAttribComparator implements Comparator<IUnitPOJO> {

    @Override
    public int compare(IUnitPOJO o1, IUnitPOJO o2) {
        return o1.getCode().compareTo(o2.getCode());
    }    
}

/**
 * Enables searching/loading of extracted values from the web service.
 *
 * @author dganguly
 */
public class ExtractedInfoRetriever implements Closeable {
    Properties prop;
    IndexReader reader;
    IndexSearcher searcher;
    IndexReader docReader;
    IndexSearcher docSearcher;
    Analyzer analyzer;
    JsonBuilderFactory factory;
    
    static Logger logger = LoggerFactory.getLogger(ExtractedInfoRetriever.class);

    public ExtractedInfoRetriever(Properties props) throws IOException {
        prop = props;
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
     * This is called from the server side. Instead of using files, this constructor
     * uses 'resources' objects.
     */
    public ExtractedInfoRetriever() throws IOException {
        this(Props.loadProperties());
    }

    /**
     * Called from the stand-alone application to save the extracted info objects
     * that could later be loaded from the server side of the web application.
     *
     * @param propFile A properties file
     */
    public ExtractedInfoRetriever(String propFile) throws IOException {
        this(Props.loadProperties(propFile));
    }

    public Properties getProperties() { return prop; }
    
    /**
     * Constructs a 'ResearchDoc' object from a given file name. The 'ResearchDoc'
     * object replicates the in-memory representation of a Lucene document object
     * comprising of fields, such as title, authors, introduction etc.
     *
     * @param docName Name of the document
     * @return A 'ResearchDoc' object.
     *
     */    
    public ResearchDoc retrieveDocInfo(String docName) throws ParseException, IOException {
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

    @Override
    public void close() throws IOException {
        docReader.close();
        reader.close();
    }
    
    public List<String> exportRecordsAsJSONArray() throws Exception {
        List<String> jsons = new ArrayList<>();
        int n = reader.numDocs();
        for (int i=0; i < n; i++) {
            Document d = reader.document(i);
            String json = d.get(InformationUnit.JSON_FIELD);
            jsons.add(json);
        }
        return jsons;
    }

    /**
     * Used from the part of the code where a graph is to be constructed
     * from the extracted values saved in the index (specified by the
     * property 'ie.index').
     * 
     * @return A list of IUnitPOJO objects which are used by the invoking
     * function to form a list of 'ArmifiedAttributeValuePair' objects.
     * @throws IOException 
     */
    public List<IUnitPOJO> getIUnitPOJOs() throws IOException {
        List<IUnitPOJO> iupojoList = new ArrayList<>();
        int n = reader.numDocs();
        
        for (int i=0; i < n; i++) {
            Document d = reader.document(i);
            
            IUnitPOJO pojo = new IUnitPOJO(
                    d.get(InformationUnit.ATTRIB_TYPE_FIELD),
                    d.get(InformationUnit.DOCNAME_FIELD),
                    d.get(InformationUnit.EXTRACTED_VALUE_FIELD),
                    d.get(InformationUnit.ATTRIB_NAME_FIELD),
                    d.get(InformationUnit.CONTEXT_FIELD),
                    d.get(InformationUnit.ARM_ID),
                    d.get(InformationUnit.ARM_NAME)
            );
            iupojoList.add(pojo);
        }
        return iupojoList;
    }
    
    public List<String> exportPerDocRecordsAsJSONArray() throws Exception {
        List<IUnitPOJO> iupojos = getIUnitPOJOs();
        List<String> jsons = new ArrayList<>();  // each element of this array is a json array of records for a document
        IUnitPOJOs pojos = new IUnitPOJOs();
        
        // sort by document names
        Collections.sort(iupojos, new IUnitPOJODocComparator());
        
        int n = iupojos.size();
        String prevDocName = null, thisDocName = null;
        
        for (int i=0; i < n; i++) {
            IUnitPOJO thisPOJO = iupojos.get(i);
            thisDocName = thisPOJO.getDocName();
            
            if (prevDocName != null && !prevDocName.equals(thisDocName)) {
                // done for this (prev) document
                // add new set to our list and start anew
                jsons.add(pojos.toString());
                pojos = new IUnitPOJOs();
            }
            
            pojos.add(thisPOJO);            
            prevDocName = thisDocName;
        }        
        
        return jsons;
    }
    

    public static void main(String[] args) {
        if (args.length == 0) {
            System.err.println("Usage: java InformationExtractor <prop-file>");
            args[0] = "init.properties";
        }
        
        try {        
            ExtractedInfoRetriever instance = new ExtractedInfoRetriever(args[0]);
            instance.exportRecordsAsJSONArray();
            instance.close();
            
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
}
