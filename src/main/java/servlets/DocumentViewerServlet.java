/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package servlets;

import indexer.ResearchDoc;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.Properties;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import org.apache.http.HttpRequest;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 *
 * @author Debasis
 */
public class DocumentViewerServlet extends HttpServlet {

    IndexReader reader;
    static final int CONTEXT_SIZE = 15;
    
    @Override
    public void init(ServletConfig config) throws ServletException {
        try {
            String indexDir = this.getClass().getClassLoader().getResource("indexes/para.index.all").getPath();
            reader = DirectoryReader.open(FSDirectory.open(new File(indexDir).toPath()));
        }
        catch (Exception ex) { ex.printStackTrace(); }
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
        
        int maxDocId = reader.maxDoc();
        try (PrintWriter out = response.getWriter()) {
            
            String docId = request.getParameter("id");
            int docIdVal = Integer.parseInt(docId);
            
            StringBuffer buff = new StringBuffer();
            for (int i=-CONTEXT_SIZE; i<=CONTEXT_SIZE; i++) {
                int j = docIdVal + i;
                if (j < 0 || j >= maxDocId)
                    break;
                Document doc = reader.document(j);
                        
                // Return this paragraph with some preceding and succeding context.
                buff.append(doc.get(ResearchDoc.FIELD_CONTENT)).append(" ");                
            }
            
            request.setAttribute("doc_content", buff.toString());
            request.getRequestDispatcher("docview.jsp").forward(request, response);
        }
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

}