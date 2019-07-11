/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.servlets;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URL;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import com.ibm.drl.hbcp.parser.JSONRefParser;

/**
 *
 * @author dganguly
 */
public class OntologyTreeServlet extends HttpServlet {

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
        
        HttpSession session = request.getSession();
        JSONRefParser ctree = (JSONRefParser)session.getAttribute("ctree");
                
        try {
            if (ctree == null) {
                URL refJsonUrl = JSONRefParser.class.getClassLoader().getResource("Sprint1_Codeset1.json");
                ctree = new JSONRefParser();
                ctree.buildCodeSetsFromURL(refJsonUrl);
                session.setAttribute("ctree", ctree);
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
        response.setContentType("application/json");
        int code = Integer.parseInt(request.getParameter("code"));
        
        try (PrintWriter out = response.getWriter()) {
            
            /* TODO output your page here. You may use following sample code. */
            //out.println("[{\"id\":1,\"text\":\"Population\",\"children\":[{\"id\":2,\"text\":\"Age\"},{\"id\":3,\"text\":\"Gender\"}]}]");
            
            System.out.println("+++JSON");
            System.out.println(ctree.getJSON(code));
            System.out.println("---JSON");
            
            out.println(ctree.getJSON(code));
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
