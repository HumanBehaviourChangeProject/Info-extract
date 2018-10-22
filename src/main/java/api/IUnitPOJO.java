/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

/**
 * This class encodes the format of the JSON object returned via the REST APIs
 * for information extraction. The returned JSON has three fields - i)
 * the code of the attribute that is to be extracted, ii) the name of the pdf file,
 * and iii) the extracted value.
 * 
 * @author dganguly
 */
public class IUnitPOJO {
    String code;
    String docName;
    String extractedValue;

    public IUnitPOJO(String docName, String value) {
        this.docName = docName;
        this.extractedValue = value;
        this.code = "";
    }
    
    public IUnitPOJO(String docName, String value, String code) {
        this.docName = docName;
        this.extractedValue = value;
        this.code = code;
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
}
