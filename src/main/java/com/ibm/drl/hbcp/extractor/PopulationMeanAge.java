/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import com.ibm.drl.hbcp.parser.CodeSetTree;

/**
 * This class is used to extract the "average age" from a population.
 * @author dganguly
 */
public class PopulationMeanAge extends InformationUnit {
    String value;
    
    public static final String ATTRIB_ID = "4507435";
    public static final String ATTRIB_NAME = "Mean Age";
    
    static int[] ALLOWABLE_RANGE = { 10, 70 };
    
    public PopulationMeanAge(InformationExtractor extractor, String contentFieldName, AttributeType type) {
        super(extractor, contentFieldName, type);
        attribId = ATTRIB_ID;
        query = "(average OR mean) AND (age OR year OR old)";
        keepUnAnalyzedToken = false;
    }
    
    @Override
    boolean isValidCandidate(String word) {
        float val = 0;
        val = getFloatVal(word);        
        return val > ALLOWABLE_RANGE[0] && val <= ALLOWABLE_RANGE[1];
    }
    
    float getFloatVal(String age) {
        float val = 0;
        try {
            val = Float.parseFloat(age);
        }
        catch (NumberFormatException ex) { }
        return val;
    }
    
    @Override
    String preProcess(String window) {
        window = window.replace("·", ".");
        return window;
    }
    
    @Override
    public void setProperties() {
        value = this.getBestAnswer().getKey();
    }
    
    String getRefValue(String refStr) {
        String[] tokens = refStr.split("\\s+");
        for (String token : tokens) {
            float val = getFloatVal(token);
            if (val > 0)
                return token;
        }
        return null;
    }
    
    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc, boolean armification) {
        String refAgeMean = gt.getNode(attribId).getAnnotatedText(docName);
        int refAgeMeanValue = 0;
        
        // In some annotations, there're multiple values (with standard deviation)
        // Remove these.
        if (refAgeMean != null) {
            refAgeMean = getRefValue(refAgeMean);
            if (refAgeMean != null)
                refAgeMeanValue = (int)(Float.parseFloat(refAgeMean));
            else
                logger.error("Problem parsing mean age from JSON");
        }

        if (predicted==null) {
            if (refAgeMean!=null) {
                rc.fn++; // annotated but we aren't able to predict
            }
            return;  // no point of going further.. sine there's nothing we predicted
        }
        else if (refAgeMean==null) {
            rc.fp++; // we predicted null when there was a value
            return;
        }
        
        PopulationMeanAge predictedObj = (PopulationMeanAge)predicted;
        
        int predictedValue = (int)(Float.parseFloat(predictedObj.value));
        boolean correct = predictedValue==refAgeMeanValue;  // match in the integer part
        
        logger.info(getName() + ": (predicted, reference): " + docName + ", " +
                predictedObj.value + ", " + refAgeMean + " (" + correct + ")");
        if (correct)
            rc.tp++;
        else {
            rc.fp++;
            rc.fn++;
        }
    }
    
    @Override
    public String getName() { return ATTRIB_NAME; }

    @Override
    public void appendFields(Document doc) {
        doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
            String.valueOf(attribId),
            Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    @Override
    public boolean matches(String attribId) {
        return attribId.equals(this.attribId);
    }    
}
