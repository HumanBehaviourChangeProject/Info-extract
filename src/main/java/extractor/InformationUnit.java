/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import glm.QueryExpander;
import indexer.PaperIndexer;
import indexer.ResearchDoc;
import java.util.*;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.highlight.QueryTermExtractor;
import org.apache.lucene.search.highlight.WeightedTerm;
import org.apache.lucene.search.similarities.LMJelinekMercerSimilarity;
import org.apache.lucene.search.similarities.Similarity;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import ref.Attribute;
import ref.CodeSetTree;
import ref.CodeSetTreeNode;
import ref.PerDocRefs;

/**
 * The object responsible for performing the basic functionalities for information extraction.
 * Acts as a base class for other more particular extraction objects.
 * * @author dganguly
 */

public abstract class InformationUnit implements Comparable<InformationUnit> {
    
    String attribId;
    InformationExtractor extractor;
    String docName;
    String contentFieldName;
    Analyzer analyzer;    
    Similarity sim;
    
    String query;
    Query queryObj;
    
    QueryTermMatches queryTermMatches;    
    float weight;
    String[] queryTerms;    
    CandidateAnswer mostLikelyAnswer;

    boolean typePresenceDetect;  //  Most BCTs are detect presence type attributes 
    boolean keepUnAnalyzedToken;
    int code;  // one of context, intervention, outcome
    Logger logger;
    int nwanted;
    
    RefComparison rc;
    
    public static final String ATTRIB_ID_FIELD = "ATTRIB_ID";
    public static final String ATTRIB_NAME_FIELD = "ATTRIB_NAME";
    public static final String EXTRACTED_VALUE_FIELD = "EXTRACTED_VALUE";
    public static final String DOCNAME_FIELD = "DOCNAME";
    
    /**
     * Initializes query term matches and the retrieval model.
     * 
     * @param extractor The parent extractor object from which this is invoked.
     * @param contentFieldName The field name (of the Lucene index) from which to extract information.
     * @param code Code of the attribute to extract
     */
    public InformationUnit(InformationExtractor extractor,
                            String contentFieldName,
                            int code) {
        this.extractor = extractor;
        this.contentFieldName = contentFieldName;
        
        if (extractor!=null)
            this.analyzer = extractor.analyzer;
        
        //keys = new CandidateAnswers();
        queryTermMatches = new QueryTermMatches();        
        
        logger = (Logger)LoggerFactory.getLogger(InformationUnit.class);
        this.code = code;
        typePresenceDetect = false;
        keepUnAnalyzedToken = false;
        sim = new LMJelinekMercerSimilarity(0.9f); // default
    }
    
    public RefComparison getEval() { return rc; }
    
    void setSimilarity() { }
    
    public InformationExtractor getExtractor() { return extractor; }

    /**
     * Updates the data member 'queryObj' which represents a Lucene 'Query' object.
     * 
     * @param query The query string
     */
    public void updateQuery(String query) throws Exception {
        this.query = query;
        int nnQE = Integer.parseInt(extractor.prop.getProperty("qe.nn", "0"));
        if (nnQE > 0) {
            Query qObj = constructQuery(query);
            String[] queryTerms = extractQueryTerms(qObj);            
            QueryExpander qexp = new QueryExpander(nnQE, queryTerms, extractor.getWordVecs());
            this.queryObj = qexp.getExpandedQuery();  // set the query object...
            
            logger.info("Expanded query: " + this.queryObj);
        }
    }
    
    /**
     * The sub-classes can add more fields to this for saving the extracted information.
     * @param doc Lucene 'Document' object
     */
    public void appendFields(Document doc) { }
    
    /**
     * Creates a Lucene index to save the extracted values. Allows for key based
     * and content based matches.
     * 
     * @return A Lucene 'Document' object to be stored.
     */
    public Document constructIndexRecord() {
        Document doc = new Document();
        
        //logger.info(docName);
        
        doc.add(new Field(InformationUnit.DOCNAME_FIELD,
            docName,
            Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        doc.add(new Field(InformationUnit.ATTRIB_NAME_FIELD,
            getName(),
            Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        doc.add(new Field(InformationUnit.EXTRACTED_VALUE_FIELD,
            this.mostLikelyAnswer.key,
            Field.Store.YES, Field.Index.NOT_ANALYZED));
        
        // Leave it upto the concrete instances to fill up specific fields,
        // e.g. the attribute id etc.
        appendFields(doc);
        return doc;
    }
    
    boolean isTypePresenceDetect() { return typePresenceDetect; }
    boolean isKeepUnAnalyzedToken() { return keepUnAnalyzedToken; }
    
    String[] extractQueryTerms(Query q) {
        QueryTermExtractor extractor = new QueryTermExtractor();
        WeightedTerm[] wqueryTerms = extractor.getTerms(q);
        
        String[] queryTerms = new String[wqueryTerms.length];
        
        for (int i=0; i < wqueryTerms.length; i++) {
            queryTerms[i] = wqueryTerms[i].getTerm();
        }        
        Arrays.sort(queryTerms);
        return queryTerms;
    }
    
    boolean isValidCandidate(String word) { return false; }
    
    void reInit(String docName) {
        this.docName = docName;
        this.mostLikelyAnswer = null;
        queryTermMatches = new QueryTermMatches();        
    }
    
    // The derived classes should override this function for any
    // pre-processing that may be required to handle the data.
    String preProcess(String content) { return content; }

    /**
     * Provides the generic workflow for the life-cycle of an InformationUnit object.
     * The calling function invokes this for all top ranked passages. This function
     * accumulates the scores over candidate answers. Example: it accumulates the evidences
     * that a feature is of certain value (e.g. min-age being 20) over a number of top ranked passages.
     * The more number of times it gathers the evidences, higher the belief rises.
     * 
     * Overall, this method performs the following actions:
     * <ol>
     * <li> Preprocesses the window to perform stopword removal and stemming and some pdf specific processing </li>
     * <li> Adds to the list of candidate answers if something valid is found </li>
     * <li> Selects the best answer based on the accumulated scores. </li>
     * </ol>
     * 
     * @param window The text retrieved in response to a query.
     * @param q Query object
     * @param sim Similarity of the retrieved text with the query (used for accumulation).
     */
    public void construct(String window, Query q, float sim) {
        this.queryTerms = extractQueryTerms(q);
        
        window = preProcess(window);
        
        CandidateAnswers keys = new CandidateAnswers();
        
        String[] tokens = window.split("\\s+");
        int numTokens = tokens.length;
        int j = 0;
        
        for (int i=0; i < numTokens; i++) {
            String token = tokens[i];
            if(!keepUnAnalyzedToken)
                token = PaperIndexer.analyze(analyzer, token);
            
            if (token == null) {
                logger.error("Could not analyze token " + token);
            }
            
            if (isValidCandidate(token)) {
                keys.addCandidateAnswer(new CandidateAnswer(token, i));
            }
            
            int index = Arrays.binarySearch(this.queryTerms, token);
            if (index > -1) {
                queryTermMatches.add(token, i); // i is the position of the query term in 'window'.
            }
        }
        
        mostLikelyAnswer = queryTermMatches.selectBestAnswer(keys);
        if (mostLikelyAnswer != null) {
            weight = mostLikelyAnswer.avgKernelSim * sim;
            logger.info(window+"->"+ mostLikelyAnswer.key+"- weight:"+weight);
        }
    }
    
    public CandidateAnswer getBestAnswer() {
        return mostLikelyAnswer;
    }
    
    @Override
    public int compareTo(InformationUnit that) {
        return -1*Float.compare(weight, that.weight);
    }

    /**
     * Overrides the number of top ranked passages wanted. The sub classes can override this.
     * 
     * @return The number of top ranked passages wanted.
     */
    public int numWanted() {
        if (nwanted > 0)
            return nwanted;
                    
        if (extractor.prop != null)
            nwanted = Integer.parseInt(extractor.prop.getProperty("numwanted", "1"));
        else
            nwanted = 1;
        
        return nwanted;
    }
    
    public void setNumWanted(int nwanted) {
        this.nwanted = nwanted;
    }
    
    public Query constructQuery() throws ParseException {
        return constructQuery(query);
    }

    public Query constructQuery(String query) throws ParseException {
        if (queryObj == null) {
            QueryParser parser = new QueryParser(this.contentFieldName, analyzer);
            Query q = parser.parse(query);

            return q;
        }
        else {
            return queryObj;
        }
    }
    
    public abstract void setProperties();
    
    void learnThreshold(CVSplit cvsplit,
                        InformationExtractor extractor) throws Exception { }
        
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc) { }
    
    public String getName() { return null; }
    
    void trace(String attribId, String docId, String extractedVal, String refVal) {
        final String DELIM = "\t";
        StringBuffer buff = new StringBuffer();
        buff
            .append(attribId)
            .append(DELIM)
            .append(docId)
            .append(DELIM)
            .append(extractedVal)
            .append(DELIM)
            .append(refVal)
        ;
        logger.debug(buff.toString());
    }
    
    void setEvalMetric(RefComparison rc) { this.rc = rc; }
   
    void printMetricValues() {        
        StringBuffer buff = new StringBuffer(getName())
                .append("\t")
                .append(this.query)
                .append("\t");
        
        String nums = rc.toString(this.typePresenceDetect, false, true);
        buff.append(nums);
        System.out.println(buff.toString());
    }
    
    /**
     * To be retrieved for this attribute id? Sub-classes can override the default behaviour 'false'.
     * @param attribId Given attribute id
     * @return 
     */
    public boolean matches(String attribId) { return false; }
    
    public String getAttribId() { return attribId; }

    public int getNumDocsAnnotated() {
        CodeSetTree refNodeTree = extractor.refBuilder.getGroundTruths(code);
        CodeSetTreeNode refNode = refNodeTree.getNode(attribId);
        PerDocRefs pdmap = refNode.getDocRecordMap();
                
        List<Attribute> attribs = new ArrayList<>(pdmap.getRecords().values());
        
        int numDocsWithThisAttrib = attribs.size();
        return numDocsWithThisAttrib;
    }
    
    /**
     * Checks whether there is a positive annotation associated with attribId and docname
     * @return 
     */
    public boolean hasPositiveAnnotation() {
        boolean foo = false;
        
        CodeSetTreeNode refNode=this.extractor.refBuilder.getGroundTruths(this.code).getNode(attribId);
        if (refNode != null) {
            if (refNode.getAnnotatedText(docName) != null) {
                foo=true;
            }
        }
            
        return foo;
    }
    
    /**
     * Update this information unit from a training set of documents.
     * For now, only update the query from the training set of documents.
     * 
     * @param cvsplit A given split into train:test.
     * @param nTopTerms Number of top terms used to learn the query representation automatically
     * from the positive annotations.
     * @param lambda The parameter to the LM Jelineck Mercer smoothing.
     */
    void updateFromTrainingData(CVSplit cvsplit, int nTopTerms, float lambda) throws Exception {
        
        List<String> docs = new ArrayList<>();
        
        for (Attribute a : cvsplit.attribsToTrain) {
            docs.add(a.getContextText() + " " + a.getAnnotatedText());
        }
        
        if (docs.isEmpty()) {
            this.query = null;  // no docs to train from... can't learn query
            return;
        }
        
        // Extract top 10 terms
        TopTermsExtractor topTermsExtractor = new TopTermsExtractor(
                docs, analyzer, extractor.reader, ResearchDoc.FIELD_CONTENT);
        
        List<WeightedTerm> wtermlist = topTermsExtractor.computeTopTerms(nTopTerms, lambda);
        
        // Formulate query
        StringBuffer buff = new StringBuffer();
        for (WeightedTerm wterm : wtermlist) {
            buff.append(wterm.getTerm()).append(" ");
        }

        updateQuery(buff.toString());
    }    
}
