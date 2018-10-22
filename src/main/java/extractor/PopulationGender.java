/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import java.util.BitSet;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import ref.CodeSetTree;
import ref.CodeSetTreeNode;
import indexer.PaperIndexer;

/**
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
    
    public PopulationGender(InformationExtractor extractor,
            String contentFieldName,
            int code) {
        super(extractor, contentFieldName, code);
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

    boolean numbersPresent(String text) {
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

        String x = this.mostLikelyAnswer.key;

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
        
        this.mostLikelyAnswer.key = gender;
    }

    String getGenderName(BitSet bits) {
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
    
    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc) {
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
    public String getName() { return "Gender"; }

    @Override
    public void appendFields(Document doc) {
        if (predictedGenders.get(MALE)) {
            doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
                String.valueOf(ATTRIB_IDS[MALE]),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
        }
        else if (predictedGenders.get(FEMALE)) {
            doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
                String.valueOf(ATTRIB_IDS[FEMALE]),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
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
