/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.CodeSetTree;
import com.ibm.drl.hbcp.parser.CodeSetTreeNode;
import com.ibm.drl.hbcp.util.LuceneField;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;

import java.util.BitSet;

/**
 * This InformationUnit seeks to extract gender values (Male or Female)
 * or a mixed population from a document. The extracted value is comprises 2 bits,
 * where the first bit refers to Male presence and the 2nd one of a female presence,
 * e.g. 11 implies a 'mixed population'.
 * 
 * @author dganguly
 */
public class PopulationGender extends InformationUnit {
    String retrievedText;
    BitSet predictedGenders, refGenders;
    
    static String[] maleKeywords = { "male", "man", "men" };
    static String[] femaleKeywords = { "female", "woman", "women", "pregnant" };
    
    static final int MALE = 0;
    static final int FEMALE = 1;
    static final int MIXED = 2;
    
    public static final String[] ATTRIB_IDS = {
        "4507430", //"4266602", //"3587701", // male only
        "4507426", //"4266589", // 3587702", // female only
        "4507427",//"4269096" // "3587703" // mixed gender
    };

    public static final String[] ATTRIB_NAMES = {
        "All Male", //"4266602", //"3587701", // male only
        "All Female", //"4266589", // 3587702", // female only
        "Mixed genders",//"4269096" // "3587703" // mixed gender
        "Other gender"
    };
    
    public PopulationGender(InformationExtractor extractor,
                            String contentFieldName,
                            AttributeType type) {
        super(extractor, contentFieldName, type);
        query = "male OR man OR men OR female OR woman OR women OR pregnant OR gender";
        //query = "female OR woman OR women OR pregnant OR gender AND NOT male AND NOT man";
        //query = "(male OR man OR men OR gender) AND NOT (female OR woman OR women)";        
        
        refGenders = new BitSet(2);
    }
    
    @Override
    String preProcess(String window) {
        retrievedText = PaperIndexer.analyze(analyzer, window);
        return window;
    }
    
    @Override
    boolean isValidCandidate(String word) {
        for (String w : maleKeywords) {
            String w_pp = PaperIndexer.analyze(analyzer, w);
            if (word.equals(w_pp))
                return true;
        }
        
        for (String w : femaleKeywords) {
            String w_pp = PaperIndexer.analyze(analyzer, w);
            if (word.equals(w_pp))
                return true;
        }
        return false;
    }

    private boolean numbersPresent(String text) {
        float val = 0;
        String[] tokens = text.split("\\s+");
        for (String token: tokens) {
            try {
                val = Float.parseFloat(token);
                return true;  // most likely some percentage... the complement set exists hence M-F
            }
            catch (NumberFormatException ex) {
                continue;
            }
        }
        return false;
    }
    
    // The predicted values
    @Override
    public void setProperties() {
        predictedGenders = new BitSet(2);
        refGenders = new BitSet(2);

        String x = this.mostLikelyAnswer.getKey();

        for (String w : maleKeywords) {
            String w_pp = PaperIndexer.analyze(analyzer, w);
            if (x.equals(w_pp)) {
                predictedGenders.set(MALE);
            }
        }
        for (String w : femaleKeywords) {
            String w_pp = PaperIndexer.analyze(analyzer, w);
            if (x.equals(w_pp)) {
                predictedGenders.set(FEMALE);
            }
        }

        String gender = getGenderName(predictedGenders);
        
        if (numbersPresent(retrievedText))
            gender = "M-F";
        
        this.mostLikelyAnswer.setKey(gender);
    }

    private String getGenderName(BitSet bits) {
        String gender = null;
        if (bits.get(MALE) && bits.get(FEMALE))
            gender = "M-F";
        else if (bits.get(MALE))
            gender = "M";
        else if (bits.get(FEMALE))
            gender = "F";
        else
            gender = "None";
        return gender;
    }

    /**
     * Compares with the reference ground-truth value and checks for exact matches,
     * i.e. a 'male-only' extracted value needs to match with a 'male-only' in
     * the ground-truth.
     * 
     * @param gt
     * @param predicted
     * @param rc
     * @param armification 
     */
    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc, boolean armification) {
        logger.info("Retrieved text (for extracting gender): " + retrievedText);
        
        CodeSetTreeNode maleNode = null, femaleNode = null, mixedNode = null;
        
        // Get node from GT
        maleNode = gt.getNode(ATTRIB_IDS[MALE]);
        femaleNode = gt.getNode(ATTRIB_IDS[FEMALE]);
        mixedNode = gt.getNode(ATTRIB_IDS[MIXED]);
        
        // Get records for this doc only
        PopulationGender refObj = (PopulationGender)this;
        refObj.refGenders = new BitSet(2);
        
        String male = maleNode.getAnnotatedText(refObj.docName);
        if (male!=null) {
            refObj.refGenders.set(MALE);
            logger.info("Ref gender: MALE");            
        }
        String female = femaleNode.getAnnotatedText(refObj.docName);
        if (female!=null) {
            refObj.refGenders.set(FEMALE);
            logger.info("Ref gender: FEMALE");            
        }
        String both = mixedNode.getAnnotatedText(refObj.docName);
        if (both!=null) {
            refObj.refGenders.set(MALE);
            refObj.refGenders.set(FEMALE);
            logger.info("Ref gender: BOTH");            
        }
        
        if (predicted==null) {
            if (refObj.refGenders.get(MALE) || refObj.refGenders.get(FEMALE)) {
                rc.fn++; // annotated but we aren't able to predict
            }
            return;  // no point of going further.. sine there's nothing we predicted
        }
        else if (!(refObj.refGenders.get(MALE) || refObj.refGenders.get(FEMALE))) {
            // we predicted some gender when there was none...
            rc.fp++;
            return;
        }
        
        // Check what annotations does this doc have
        PopulationGender thisObj = (PopulationGender)predicted;        
        String predictedGender = this.mostLikelyAnswer.getKey();
        String refGender = getGenderName(thisObj.refGenders);
        
        logger.info("Predicted gender: " + predictedGender + " (Actual gender: " + refGender + ")");

        if (predictedGender.equals(refGender))
            rc.tp++; // got the value correct...
        else {
            rc.fp++; // a false positive... there was a value which we predicted wrongly
            rc.fn++;
        }
    }

    @Override
    public String getName() {
        int code = 3;
        if (predictedGenders == null) return "Gender";
        if (predictedGenders.get(MALE)) {
            if (predictedGenders.get(FEMALE))
                code = 0; // mixed
            else
                code = 1; // male only
        }
        else if (predictedGenders.get(FEMALE))
            code = 2;
        return ATTRIB_NAMES[code];
    }

    @Override
    public void appendFields(Document doc) {
        if (predictedGenders.get(MALE)) {
            doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
                String.valueOf(ATTRIB_IDS[MALE]),
                    LuceneField.STORED_NOT_ANALYZED.getType()));
        }
        else if (predictedGenders.get(FEMALE)) {
            doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
                String.valueOf(ATTRIB_IDS[FEMALE]),
                    LuceneField.STORED_NOT_ANALYZED.getType()));
        }
    }

    @Override
    public boolean matches(String attribId) {
        for (String id: ATTRIB_IDS) {
            if (id.equals(attribId))
                return true;
        }
        return false;
    }
        
}
