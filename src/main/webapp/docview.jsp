<%-- 
    Document   : docview
    Created on : Nov 20, 2017, 4:25:39 PM
    Author     : dganguly
--%>

<%@page contentType="text/html" pageEncoding="UTF-8"%>

<!DOCTYPE html>
<html>
    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=UTF-8">
        <title>Document Viewer</title>
    </head>
    <body>
    <div>
        <!--table border="0">
        <tbody>
        <tr>
        <td align="center">
        <img src="images/IBM-Watson-Logo.png" alt="IBM Research Dublin"
             border="0" style="max-height:50%;max-width:20%">
        </td>
        <td align="center">
        <img src="images/hbcp.jpg" alt="HBCP"
             border="0" style="max-height:40%;max-width:40%">                            
        </td>
        </tr>
        </tbody>
        </table-->
        <center>
        <img src="images/hbcp.jpg" alt="HBCP"
             border="0" style="max-height:10%;max-width:10%">
        </center>        
    </div>
    <hr>
    <div>
    <p>
    ${doc_content}    
    </p>
    </div>
    </body>
</html>
