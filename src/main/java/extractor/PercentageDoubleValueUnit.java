/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package extractor;

import java.util.HashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import ref.CodeSetTree;
import ref.CodeSetTreeNode;

/**
 *
 * @author yufang
 */
public class PercentageDoubleValueUnit extends InformationUnit {
    String attribId;
    String value;
    
    
    public PercentageDoubleValueUnit(InformationExtractor extractor, String contentFieldName, int code, String attributeID) {
        super(extractor, contentFieldName, code);
        attribId = attributeID;
        setQuery();
        keepUnAnalyzedToken = true;
    }
    
    void setQuery() {
        this.query = extractor.prop.getProperty("attributes.typedetect.query." + this.attribId);
    }

    
    @Override
    boolean isValidCandidate(String word) {
        if(word.matches("(\\d+\\%|\\d+\\.\\d+\\%)")){
            return true;
        }else if(word.matches("(\\d+\\.\\d+|\\d+)")){
            Double value = Double.valueOf(word);
            if(value>0 && value < 100)
                return true;
        }
        return false;
        
    }

    public int numWanted() {
        return 10;
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
    
    Set<String> getRefValue(String refStr) {
        Set<String> refs = new HashSet();
        String[] tokens = refStr.split("\\s+");
        Pattern pattern1 = Pattern.compile("(\\d+\\%|\\d+\\.\\d+\\%)");
        Pattern pattern2 = Pattern.compile("(\\d+\\.\\d+|\\d+|\\d+\\%|\\d+\\.\\d+\\%|\\d+\\.)");
        for (String token : tokens) {
            Matcher matcher1 = pattern1.matcher(token); 
            Matcher matcher2 = pattern2.matcher(token); 
            if(matcher1.find()){
                refs.add(matcher1.group(1));
            }else{
                if(matcher2.find()){
                   refs.add(matcher2.group(1)); 
                }
            }
       }
        return refs;
    }
    
    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc) {
        String refValueStr = gt.getNode(attribId).getAnnotatedText(docName);
        Set<String> refValue = new HashSet();
        
        // In some annotations, there're multiple values (with standard deviation)
        // Remove these.
        if (refValueStr != null) {
            refValue = getRefValue(refValueStr);
            if(refValue.isEmpty())
               logger.error("Problem parsing percentage number from JSON:" + docName + ":" + refValueStr);
        }

        if (predicted==null) {
            if (refValueStr!=null) {
                rc.fn++; // annotated but we aren't able to predict
            }
            return;  // no point of going further.. sine there's nothing we predicted
        }
        else if (refValueStr==null) {
            rc.fp++; // we predicted null when there was a value
            return;
        }
        
        PercentageDoubleValueUnit predictedObj = (PercentageDoubleValueUnit)predicted;
        
        String predictedValue = (String)(predictedObj.value);
//        boolean correct = predictedValue.equalsIgnoreCase(refValue);  
        boolean correct = refValue.contains(predictedValue);  
        
        logger.info(getName() + ": (predicted, reference): " +
                predictedObj.value + ", " + refValue + " (" + correct + ")");
        if (correct)
            rc.tp++;
        else {
            rc.fp++;
            rc.fn++;
        }
    }
    
    @Override
    public String getName() { 
        String name = "";
        if (this.extractor.refBuilder != null) {
            CodeSetTree gt = this.extractor.refBuilder.getGroundTruths(code);
            CodeSetTreeNode refNode = gt.getNode(attribId);
            name = refNode.getName();
        }
        return name + "(" + attribId + ")"; }

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
