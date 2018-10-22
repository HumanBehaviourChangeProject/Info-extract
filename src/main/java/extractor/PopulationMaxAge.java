/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import static extractor.PopulationGender.ATTRIB_IDS;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import indexer.PaperIndexer;
import ref.CodeSetTree;

/**
 *
 * @author dganguly
 */
public class PopulationMaxAge extends InformationUnit {

    public static final String ATTRIB_ID = "4507434";
    static final int[] MAX_AGE_VALID_RANGE = { 18, 100 };
    String currentRetrievedText;
    Set<String> maxAgeCandidatesInCurrentRetrievedText;
    List<Pattern> maxAgePattern;
    String max;
    
    public PopulationMaxAge(InformationExtractor extractor, String contentFieldName, int code) {
        super(extractor, contentFieldName, code);
        //query = "(participant OR smoker OR people OR person) AND (age OR year OR old)";
        query = "(participant OR smoker OR people OR person OR patient OR client OR respondent) AND (age OR year OR old)";
        keepUnAnalyzedToken = false;
        currentRetrievedText = "";
        maxAgePattern = new ArrayList();
        maxAgePattern.add(Pattern.compile("from (\\d+) (to) (\\d+)"));
        maxAgePattern.add(Pattern.compile("between (\\d+) (to) (\\d+)"));
        maxAgePattern.add(Pattern.compile("between (\\d+) (and) (\\d+)"));
        maxAgePattern.add(Pattern.compile("(\\d+)(\\-|–)(\\d+)"));
    }
    
    @Override
    String preProcess(String window) {
	currentRetrievedText = window;
        maxAgeCandidatesInCurrentRetrievedText = new HashSet();
        for(Pattern p: maxAgePattern){
            Matcher m = p.matcher(currentRetrievedText);
            while(m.find()){
        	maxAgeCandidatesInCurrentRetrievedText.add(m.group(3));
            }
        }
	return window;
    }

    @Override
    boolean isValidCandidate(String word) {
        
        //for the token "18-50", currently after PaperIndexer.analyze, it becomes "18 50", so here we keep 50 and check 
        //whether it appeas in the maxAge pattern
        if(word.matches("\\d+ \\d+"))
             word = word.split(" ")[1];
         boolean valid = maxAgeCandidatesInCurrentRetrievedText.contains(word);
         return valid;
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
	max = this.getBestAnswer().getKey();
	//for the token as the form of "18 50" after PaperIndexer.analyze
	if(max.matches("\\d+ \\d+"))
	    max = max.split(" ")[1];
    }


    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc) {
        String ageMax = gt.getNode(ATTRIB_ID).getAnnotatedText(docName);
        logger.info("annotation:" + ageMax );
        if (predicted==null) {
            if (ageMax!=null) {
                rc.fn++; // annotated but we predict null
            }
            return;  // no point of going further.. sine there's nothing we predicted
        }
        else if (ageMax==null) {
            rc.fp++; // we predicted null when there was a value
            return;
        }
        
        PopulationMaxAge predictedObj = (PopulationMaxAge)predicted;
        trace(ATTRIB_ID, docName, predictedObj.max, ageMax);
        
        boolean correctMax = predictedObj.max.equals(ageMax);
        logger.info("AgeMax (predicted, reference): " + docName + ": " + predictedObj.max + ", " + ageMax);

        if (correctMax)
            rc.tp++;
        else {
            rc.fp++;
            rc.fn++;
        }
    }
    
    @Override
    public String getName() { return "Max Age"; }

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
