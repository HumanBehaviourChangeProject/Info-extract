/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.parser;

import java.io.Serializable;
import java.util.*;
import java.util.Map.Entry;

/**
 * This class wraps mappings from document ids to annotated attribute-value pairs.
 * One map keeps track of document to {@link AnnotatedAttributeValuePair} and the 
 * other keeps track of AnnotatedAttributeValuePairs per arm and per document.
 *
 * @author dganguly
 */
public class PerDocRefs implements Serializable {

    private static final long serialVersionUID = 1L;

    // keyed by document name and assume only one annotation per document
    protected final HashMap<String, AnnotatedAttributeValuePair> refs;
    //for armification, keyed by document name, one document could have several annotations for one attribute, each for one arm
    Map<String, Map<String, AnnotatedAttributeValuePair>> armedRefs;

    public PerDocRefs() {
        this.refs = new HashMap<>();
        this.armedRefs = new HashMap<>();
    }

    /**
     * @return number of unique document ids
     */
    public int getNumDocs() {
        return refs.keySet().size();
    }

    /**
     * @return document-annotation map
     */
    public HashMap<String, AnnotatedAttributeValuePair> getRefs() {
        return refs;
    }
    
    /**
     * @return map of arm-annotation maps (per document)
     */
    public Map<String, Map<String, AnnotatedAttributeValuePair>> getArmRecords(){ 
        return armedRefs;
    }

    /**
     * Assign this {@link AnnotatedAttributeValuePair} to the given document.
     * Assume only one AnnotatedAttributeValuePair per document.
     * 
     * @param docName
     * @param aavp {@link AnnotatedAttributeValuePair} instance of attribute found in this document
     */
    public void add(String docName, AnnotatedAttributeValuePair aavp) {
        refs.put(docName, aavp);
    }

    /**
     * Assign this {@link AnnotatedAttributeValuePair} to the given document and arm.
     * Assume only one AnnotatedAttributeValuePair per document per arm.
     *
     * @param docName
     * @param armName
     * @param aavp {@link AnnotatedAttributeValuePair} instance of attribute found in this document and arm
     */
    public void addArm(String docName, String armName, AnnotatedAttributeValuePair aavp) {
        Map<String, AnnotatedAttributeValuePair> docArmMap = armedRefs.get(docName);
        if (docArmMap == null) {
            docArmMap = new HashMap<>();
            armedRefs.put(docName, docArmMap);
        }
        docArmMap.put(armName, aavp);
    }

    /**
     * Get all the arm names
     * 
     * @param docName
     * @return collection of arm names in document
     */
    public Set<String> getArmsInfo(String docName) {
        Set<String> armsInfo = new HashSet<>();
        Map<String, AnnotatedAttributeValuePair> attriAllArms = armedRefs.get(docName);
        if (attriAllArms != null) {
            for (String s : attriAllArms.keySet()) {
                if (!s.equals("general")) {
                    armsInfo.add(s);
                }
            }
        }
        return armsInfo;
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (String docName : refs.keySet()) {
            AnnotatedAttributeValuePair a = refs.get(docName);
            buff.append(docName)
                    .append(":::")
                    .append("'")
                    .append(a.highlightedText)
                    .append("':::'")
                    .append(a.context)
                    .append("' \n");
        }
        return buff.toString();
    }

    /**
     * Get string concatenating information of all arms.
     * Annotated instances ({@link AnnotatedAttributeValuePair}s) are concatenated by newlines.
     * Each instance contains the document name, arm name, annotated/highlighted text, and 
     * surrounding context, all concatenated by ":::".
     * 
     * @return string with annotation information per document and arm
     */
    public String getArmInfo() {
        StringBuffer buff = new StringBuffer();
        for (String docName : armedRefs.keySet()) {
            for (Entry<String, AnnotatedAttributeValuePair> arm : armedRefs.get(docName).entrySet()) {
                String armName = arm.getKey();
                AnnotatedAttributeValuePair a = arm.getValue();
                buff.append(docName)
                        .append(":::")
                        .append(armName)
                        .append(":::")
                        .append("'")
                        .append(a.highlightedText)
                        .append("':::'")
                        .append(a.context)
                        .append("' \n");

            }
        }
        return buff.toString();
    }

    String cleanedHighlightedText(String text) {
        // Return the first token
        String[] tokens = text.split("\\s+");
        float val = -1;
        try {
            val = Float.parseFloat(tokens[0]);
        } catch (NumberFormatException ex) {
        }

        if (val != -1) {
            return text.split("\\s+")[0];
        } else {
            return text;
        }
    }

    String cleanedContextText(String context) {
        int len = context.length();
        StringBuffer buff = new StringBuffer();

        for (int i = 0; i < len; i++) {
            char ch = context.charAt(i);
            if (0 <= ch && ch < 'z') {
                buff.append(ch);
            } else {
                buff.append(' ');
            }
        }

        return buff.toString();
    }

    public String csvFormatted(String nodeId) {
        StringBuffer buff = new StringBuffer();
        for (String docName : refs.keySet()) {
            AnnotatedAttributeValuePair a = refs.get(docName);
            buff
                    .append(nodeId)
                    .append("\t")
                    .append(docName)
                    .append("\t")
                    .append(cleanedHighlightedText(a.getHighlightedText()))
                    .append("\t")
                    .append(cleanedContextText(a.getContext()))
                    .append("\n");
        }
        return buff.toString();
    }

    /**
     * Returns the annotated/highlighted text for this attribute instance in the document.
     * If there is no annotation (of this attribute) for this document it returns null.
     * 
     * @param docName
     * @return string of annotated text or null
     */
    public String getAnnotatedText(String docName) {
        AnnotatedAttributeValuePair refAttrib = refs.get(docName);
        return refAttrib == null ? null : refs.get(docName).highlightedText;
    }

    /**
     * Returns the page number where this attribute instance is annotated in the document.
     * If there is no annotation (of this attribute) for this document it returns null.
     * 
     * @param docName
     * @return page number or null
     */
    public Integer getAnnotationPage(String docName) {
        AnnotatedAttributeValuePair refAttrib = refs.get(docName);
        return refAttrib == null ? null : refs.get(docName).annotationPage;
    }
    
    /**
     * Returns the context surrounding the annotated attribute instance in the document.
     * If there is no annotation (of this attribute) for this document it returns null.
     * 
     * @param docName
     * @return context string or null
     */
    public String getAnnotatedContext(String docName) {
        AnnotatedAttributeValuePair refAttrib = refs.get(docName);
        return refAttrib == null ? null : refs.get(docName).context;
    }

    /**
     * Returns the 'sprint' number for this annotated attribute instance in the document.
     * If there is no annotation (of this attribute) for this document it returns null.
     * 
     * @param docName
     * @return sprint number or null
     */
    public String getSprintNo(String docName) {
        AnnotatedAttributeValuePair refAttrib = refs.get(docName);
        return refAttrib == null ? null : refs.get(docName).sprintNo;
    }

    /**
     * Looks up all arm annotation in the document and returns new mapping
     * from arm name to annotated text (of the attribute in this document).
     * 
     * @param docName
     * @return mapping of arm name to annotated text
     */
    public Map<String, String> getAnnotatedText4AllArms(String docName){ 
        Map<String, String> annotatedTextAllArms = new HashMap<>();
        Map<String, AnnotatedAttributeValuePair> attriAllArms = armedRefs.get(docName);
        if(attriAllArms==null){
            return null;
        }
        for(String armTitle: attriAllArms.keySet()){ 
            annotatedTextAllArms.put(armTitle, attriAllArms.get(armTitle).getHighlightedText());
        }
        return annotatedTextAllArms;
    }
    
    /**
     * Looks up all arm annotation in the document and returns new mapping
     * from arm name to context surrounding annotation.
     * 
     * @param docName
     * @return mapping of arm name to context text
     */
    public Map<String, String> getAnnotatedContext4AllArms(String docName){ 
        Map<String, String> annotatedContextAllArms = new HashMap<>();
        Map<String, AnnotatedAttributeValuePair> attriAllArms = armedRefs.get(docName);
        if(attriAllArms==null){
            return null;
        }
        for(String armTitle : attriAllArms.keySet()){ 
            annotatedContextAllArms.put(armTitle, attriAllArms.get(armTitle).getContext());
        }
        return annotatedContextAllArms;
    }
}
