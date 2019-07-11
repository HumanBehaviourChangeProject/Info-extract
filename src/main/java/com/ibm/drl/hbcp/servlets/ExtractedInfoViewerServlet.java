/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.servlets;

import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoRetriever;
import java.io.IOException;
import java.io.PrintWriter;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

/**
 *
 * @author dganguly
 */
public class ExtractedInfoViewerServlet extends HttpServlet {

    ExtractedInfoRetriever eiRetriever;
    
    @Override
    public void init() throws ServletException {
        try {
            eiRetriever = new ExtractedInfoRetriever();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    @Override
    public void destroy() {
        try {
            eiRetriever.close();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    String addHeader(String responseData) {
        StringBuffer buff = new StringBuffer();
        buff
            .append("<thead>")
                .append("<tr>")
                
                    .append("<th>")
                    .append("AttribId")
                    .append("</th>")
                
                    .append("<th>")
                    .append("Attribute")
                    .append("</th>")

                    .append("<th>")
                    .append("Paper")
                    .append("</th>")
                
                    .append("<th>")
                    .append("Extracted value")
                    .append("</th>")
                
                    .append("<th>")
                    .append("Title")
                    .append("</th>")
                
                    .append("<th>")
                    .append("Authors")
                    .append("</th>")
                
                    .append("<th>")
                    .append("Annotated")
                    .append("</th>")
    
                    /*
                    .append("<th>")
                    .append("Introduction (30 words)")
                    .append("</th>")
                    */
                
                .append("</tr>")
            .append("</thead>")
        ;
        
        buff.append("<tbody>");
        buff.append(responseData);
        buff.append("</tbody>");
        
        return buff.toString();
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
        
        String attribId = request.getParameter("attribid");
        
        try (PrintWriter out = response.getWriter()) {
            String responseHTML = eiRetriever.retrieveHTMLFormatted(attribId);
            
            responseHTML = addHeader(responseHTML);
            
            System.out.println("+++Table HTML");
            System.out.println(responseHTML);
            System.out.println("---Table HTML");
            
            out.println(responseHTML);
        }
        catch (Exception ex) {
            ex.printStackTrace();
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
