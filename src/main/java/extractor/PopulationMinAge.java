/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import static extractor.PopulationMaxAge.ATTRIB_ID;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import ref.CodeSetTree;

/**
 *
 * @author dganguly
 */
public class PopulationMinAge extends InformationUnit {

    public static final String ATTRIB_ID = "4507433";
    static final int[] MIN_AGE_VALID_RANGE = { 12, 60 };
    
    String min;
    
    public PopulationMinAge(InformationExtractor extractor, String contentFieldName, int code) {
        super(extractor, contentFieldName, code);
//        query = "(participant OR smoker OR people OR person) AND (age OR year OR old)";
        query = "(participant OR smoker OR people OR person OR patient OR client OR respondent) AND (age OR year OR old)";
        keepUnAnalyzedToken = false;
    }

    @Override
    boolean isValidCandidate(String word) {
        float value = 0;
        try {
            value = Float.parseFloat(word);
        }
        catch (NumberFormatException ex) { return false; }
        return value > MIN_AGE_VALID_RANGE[0] && value < MIN_AGE_VALID_RANGE[1];
    }
    
    @Override
    String preProcess(String window) {
        window = window.replaceAll("-", ".");
        return window;
    }
    
    boolean isInteger(String word) {
        int len = word.length();
        for (int i=0; i < len; i++) {
            if (!Character.isDigit(word.charAt(i)))
                return false;
        }
        return true;
    }

    @Override
    public void setProperties() {
        String x = this.getBestAnswer().getKey();
        String[] tokens = x.split("\\.");
        min = tokens[0];
    }

    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc) {
        String ageMin = gt.getNode(ATTRIB_ID).getAnnotatedText(docName);        
        
        PopulationMinAge predictedMinAgeObj = (PopulationMinAge)predicted;

        
        if (predicted==null) {
            if (ageMin!=null) {
                rc.fn++; // annotated but we aren't able to predict
                logger.info("AgeMin (predicted, reference): " + docName + ", " + "NULL" + ", " + ageMin);
            }
            return;  // no point of going further.. since there's nothing we predicted
        }
        else if (ageMin==null) {
            rc.fp++;
            logger.info("AgeMin (predicted, reference): " + docName + ", " + predictedMinAgeObj.min + ", " + "NULL");
            return;
        }
        
        trace(ATTRIB_ID, docName, predictedMinAgeObj.min, ageMin); // Question from Lea: What is this for?
            
        boolean correctMin = predictedMinAgeObj.min.equals(ageMin);
        logger.info("AgeMin (predicted, reference): " + docName + ", " + predictedMinAgeObj.min + ", " + ageMin);
        
        if (correctMin)
            rc.tp++;
        else {
            rc.fp++;
            rc.fn++;
        }
    }
    
    @Override
    public String getName() { return "Min age"; }

    @Override
    public void appendFields(Document doc) {
        doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
            String.valueOf(ATTRIB_ID),
            Field.Store.YES, Field.Index.NOT_ANALYZED));
    }
    
    @Override
    public boolean matches(String attribId) {
        return attribId.equals(ATTRIB_ID);
    }
}
