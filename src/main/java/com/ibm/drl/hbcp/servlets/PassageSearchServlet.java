package com.ibm.drl.hbcp.servlets;

/**
 * Servlet for querying Lucene index
 *
 * @author dganguly
 */
import com.ibm.drl.hbcp.extractor.InformationExtractor;
import com.ibm.drl.hbcp.extractor.InformationExtractorFactory;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import com.ibm.drl.hbcp.inforetrieval.indexer.PassageRetriever;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.List;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.search.highlight.Highlighter;
import org.apache.lucene.search.highlight.QueryScorer;
import org.apache.lucene.search.highlight.SimpleHTMLFormatter;
import org.apache.lucene.search.highlight.TextFragment;
import org.apache.lucene.search.highlight.TokenSources;

public class PassageSearchServlet extends HttpServlet {
    
    PassageRetriever retriever;
    List<InformationUnit> iuList;
    
    static final int NUMDOCS_TO_RETRIEVE = 100;    
    static final int PAGE_SIZE = 10;
    static final int NUM_EVIDENCES_FOR_IE = 5;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            retriever = new PassageRetriever();
            InformationExtractor extractor = new InformationExtractor();
            InformationExtractorFactory factory = new InformationExtractorFactory(extractor);
            iuList = factory.createIUnits();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
    
    public String getSearchResultsDisplayString(Query query, TopDocs topDocs, int page) throws Exception {
        
        ScoreDoc[] hits = topDocs.scoreDocs;
        int start, end;
        start = (page-1)*PAGE_SIZE;  // starting from this index
        end = Math.min(start + PAGE_SIZE, hits.length); // ending before this index
        
        SimpleHTMLFormatter htmlFormatter = new SimpleHTMLFormatter();
        Highlighter highlighter = new Highlighter(htmlFormatter, new QueryScorer(query));
        StringBuffer resultsBuff = new StringBuffer("<ul>");
        IndexSearcher searcher = retriever.getSearcher();
        Analyzer analyzer = retriever.getAnalyzer();
                
        float maxSim = 0f; // hits[0].score + 1;
        for (int i = start; i < end; i++) {
            maxSim += hits[i].score;
        }
        
        for (int i = start; i < end; i++) {
            ScoreDoc hit = hits[i];
            resultsBuff.append("<li>");
            Document doc = searcher.doc(hit.doc);
            String text = doc.get(ResearchDoc.FIELD_CONTENT);
            String docName = doc.get(ResearchDoc.FIELD_NAME);
            resultsBuff.append("<div class=\"ResultURLStyle\">")
                    .append("<a id=\"")
                    .append(hit.doc)
                    .append("\" rev=\"")
                    .append((float)hit.score/maxSim)
                    .append("\" name=\"")
                    .append(docName)
                    .append("\" href=\"DocumentViewerServlet?id=")
                    .append(hit.doc)
                    .append("\" target=\"_blank\">")
                    .append(docName)
                    .append("</a>")
                    .append("</div>");

            resultsBuff.append("<div class=\"ResultSnippetStyle\">");
            TokenStream tokenStream = TokenSources.getTokenStream(
                    searcher.getIndexReader(), hit.doc, ResearchDoc.FIELD_CONTENT,
                    analyzer);
            TextFragment[] frags = highlighter.getBestTextFragments(tokenStream, text, false, 100);
            for (TextFragment frag : frags) {
                if ((frag != null) && (frag.getScore() > 0)) {
                    resultsBuff.append(frag.toString());
                }
            }
            resultsBuff.append("...");
            resultsBuff.append("</div>");
            resultsBuff.append("</li>");                        
        }
        
        resultsBuff.append("</ul>");
        return resultsBuff.toString();
    }
    
    String retrieveForUserSpecifiedQuery(HttpSession session, String query, String pageStr) throws Exception {
        int page = Integer.parseInt(pageStr);
        TopDocs topDocs = retriever.retrieve(query, NUMDOCS_TO_RETRIEVE);
        System.out.println("Retrieved " + topDocs.scoreDocs.length + " documents");

        // Save the analyzed query (to be lated used for highlighting
        // each document content in the viewer
        String analyzedQry = retriever.analyze(query);                
        session.setAttribute("analyzedqry", analyzedQry);

        String responseStr = getSearchResultsDisplayString(retriever.buildQuery(query), topDocs, page);
        return responseStr;
    }
    
    String retrieveForAttrib(String attribId, String docName) throws Exception {
        InformationUnit iunit = InformationExtractorFactory.search(iuList, attribId);
        if (iunit==null) {
            return "";
        }
        
        BooleanQuery docMatchQuery = new BooleanQuery();
        docMatchQuery.add(new TermQuery(new Term(ResearchDoc.FIELD_NAME, docName)), BooleanClause.Occur.MUST);
        
        Query q = iunit.constructQuery();
        docMatchQuery.add(q, BooleanClause.Occur.SHOULD);
        
        TopDocs topDocs = retriever.retrieve(docMatchQuery, NUM_EVIDENCES_FOR_IE);
        
        return getSearchResultsDisplayString(q, topDocs, 1); // only a single page
    }
    
    /**
     * Processes requests for both HTTP <code>GET</code> and <code>POST</code>
     * methods.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    protected void processRequest(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        response.setContentType("text/html;charset=UTF-8");
        
        try (PrintWriter out = response.getWriter()) {            
            
            HttpSession session = request.getSession();
            String responseStr = "";
            
            String pageStr = request.getParameter("page");            
            String query = request.getParameter("query");
            String attribId = request.getParameter("attribid");
            String docName = request.getParameter("docname");
            
            if (query != null && pageStr!=null) {
                // If the user specified a query, i.e. this servlet
                // has been called by hitting on the search button.
                responseStr = retrieveForUserSpecifiedQuery(session, query, pageStr);
            }
            else {
                // Click event from table.
                // Get attribute id and doc name
                responseStr = retrieveForAttrib(attribId, docName);
            }
            out.println(responseStr);
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    // <editor-fold defaultstate="collapsed" desc="HttpServlet methods. Click on the + sign on the left to edit the code.">
    /**
     * Handles the HTTP <code>GET</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Handles the HTTP <code>POST</code> method.
     *
     * @param request servlet request
     * @param response servlet response
     * @throws ServletException if a servlet-specific error occurs
     * @throws IOException if an I/O error occurs
     */
    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {
        processRequest(request, response);
    }

    /**
     * Returns a short description of the servlet.
     *
     * @return a String containing servlet description
     */
    @Override
    public String getServletInfo() {
        return "Short description";
    }// </editor-fold>

    @Override
    public void destroy() {
        try {
            retriever.close();
        } catch (IOException e) {
            // marting: I don't know what to do there :D
            e.printStackTrace();
        }
    }
}

