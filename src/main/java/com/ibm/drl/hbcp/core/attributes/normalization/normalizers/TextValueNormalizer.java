/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.extractor.DocVector;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

/**
 * Normalizes categorical attributes.
 * A restriction here is that both the categories AND the values to normalize have to be at least of length NGRAM_SIZE.
 * <br>
 * For each attribute id:
 * <ol>
 * <li> In the properties file, specify a list of allowable category values for this attribute. </li>
 * <li> Go through the list of all possible values and assign it to the one with maximum cosine similarity, computed by n-grams. </li> 
 * </ol>
 * @author dganguly
 */
public class TextValueNormalizer implements Normalizer<AttributeValuePair> {
    
    Properties prop;
    String attribId;
    List<DocVector> catlist;
    CategoricalAttribute categoryAttrib;
    
    static final int NGRAM_SIZE = 3;
    static Logger logger = LoggerFactory.getLogger(TextValueNormalizer.class);
    
    public TextValueNormalizer(Properties prop, String attribId) {
        this.prop = prop;
        this.attribId = attribId;
        
        categoryAttrib = new CategoricalAttribute(attribId);
        catlist = new ArrayList<>();
        
        /* Load the values and the categories from the properties file */
        String categoryNamesCSV = prop.getProperty("prediction.categories." + attribId);
        if (categoryNamesCSV != null) {
         
            String[] categoryValuesFromProp = categoryNamesCSV.split(",");

            for (String categoryValue: categoryValuesFromProp) {
                DocVector dv = new DocVector(categoryValue, NGRAM_SIZE);
                catlist.add(dv);
                categoryAttrib.add(categoryValue);
            }
        }
    }

    public String getCategoryValues() {
        StringBuffer buff = new StringBuffer("{0,"); // default category
        for (String cval: categoryAttrib.categoryValues)
            buff.append(cval).append(",");
        
        int len = buff.length();
        if (len > 1)
            buff.deleteCharAt(len-1);
        buff.append("}");
        
        return buff.toString();
    }
    
    @Override
    public String getNormalizedValue(AttributeValuePair attribute) {
        return assignCategory(attribute.getValue());
    }

    public boolean isEmpty() { return catlist.isEmpty(); }

    // Given a value, assign it to a category -- one from catlist
    String assignCategory(String text) {
        if (catlist.isEmpty())
            return null;  // no categorization specified in properties file...
        
        DocVector dv = new DocVector(text, 3);  // text == annotated value
        
        // Find the most similar category for each value
        float maxSim = 0, sim;
        DocVector mostSimCat = catlist.get(0);
        
        for (DocVector cat: catlist) {
            sim = dv.cosineSim(cat);
            if (sim > maxSim) {
                maxSim = sim;
                mostSimCat = cat;
            }
        }
        
        // assign most similar category
        String norm = mostSimCat.getText();
        logger.info(text + " --> " + norm);
        return norm;
    }
    
}
