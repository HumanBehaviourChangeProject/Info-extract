/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.api;

/**
 * This class encodes the format of the JSON object returned via the REST APIs
 * for information extraction. The returned JSON has three fields -
 * <ol>
 * <li> the code of the attribute that is to be extracted, </li>
 * <li> the name of the pdf file, </li>
 * <li> the extracted value. </li>
 * </ol>
 *
 * @author dganguly
 */

public class IUnitPOJO {
    String type;  // one of (C, I, O)
    String code;
    String docName;
    String extractedValue;
    String context;

    public IUnitPOJO(String type, String docName, String value, String code, String context) {
        this.type = type;
        this.docName = docName;
        this.extractedValue = value;
        this.code = code;
        this.context = context;
    }
    
    public IUnitPOJO(String docName, String value, String code, String context) {
        this.docName = docName;
        this.extractedValue = value;
        this.code = code;
        this.context = context;
    }
    
    public String getCode() {
        return this.code;
    }
    
    public void setCode(String code) { this.code = code; }
    
    public String getDocName() {
        return docName;
    }
    
    public String getExtractedValue() {
        return extractedValue;
    }
    
    public String getContext() { return context; }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff
            // id is <docname:attribid>    
            .append("{")
            .append("\n")
            //.append("\"_id\": \"")
            //.append(docName)
            //.append(":")
            //.append(code)
            //.append("\",")
            //.append("\n")
            .append("\"type\": \"")
            .append(type)
            .append("\",")
            .append("\n")
            .append("\"code\": \"")
            .append(code)
            .append("\",")
            .append("\n")
            .append("\"extractedValue\": \"")
            .append(extractedValue)
            .append("\",")
            .append("\n")
            .append("\"context\": \"")
            .append(context)
            .append("\"")
            .append("\n")
            .append("}")
        ;
        
        return buff.toString();
    }
}
