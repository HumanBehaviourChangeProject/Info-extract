/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ref;

import java.io.Serializable;
import java.util.*;

/**
 *
 * @author dganguly
 */

public class PerDocRefs implements Serializable {
    // keyed by document name and assume only one annotation per document
    HashMap<String, Attribute> refs;

    public int getNumDocs() {
        return refs.keySet().size();
    }
    
    public PerDocRefs() {
        this.refs = new HashMap<>();
    }
    
    public HashMap<String, Attribute> getRecords() { return refs; }
    
    public void add(String docName, Attribute a) {
        refs.put(docName, a);
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (String docName : refs.keySet()) {
            Attribute a = refs.get(docName);
            buff.append(docName)
                .append(":::")
                .append("'")
                .append(a.highlightedText)
                .append("':::'")
                .append(a.context)
                .append("' \n")
            ;
        }
        return buff.toString();
    }

    String cleanedHighlightedText(String text) {
        // Return the first token
        String[] tokens = text.split("\\s+");
        float val = -1;
        try {
            val = Float.parseFloat(tokens[0]);
        }
        catch (NumberFormatException ex) {
        }

        if (val!=-1)
            return text.split("\\s+")[0];
        else
            return text;
    }
    
    public String csvFormatted(String nodeId) {
        StringBuffer buff = new StringBuffer();
        for (String docName : refs.keySet()) {
            Attribute a = refs.get(docName);
            buff
                .append(nodeId)
                .append("\t")
                .append(docName)
                .append("\t")
                .append(cleanedHighlightedText(a.highlightedText))
                .append("\n")
            ;
        }
        return buff.toString();
    }
    
    public String getAnnotatedText(String docName) {
        Attribute refAttrib = refs.get(docName);        
        return refAttrib==null? null : refs.get(docName).highlightedText;
    }
    
    
    public String getSprintNo(String docName) {
        Attribute refAttrib = refs.get(docName);        
        return refAttrib==null? null : refs.get(docName).sprintNo;
    }
}
