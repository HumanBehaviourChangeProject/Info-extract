/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extractor.matcher.CandidateAnswer;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.search.Query;
import com.ibm.drl.hbcp.parser.CodeSetTree;
import com.ibm.drl.hbcp.parser.CodeSetTreeNode;

/**
 * This class defines the functionalities required to extract the BCTs, which are
 * of 'detect presence' type, i.e. one needs to predict if a given BCT applies for a
 * research study or not.
 *
 * @author dganguly
 */
public class AbstractDetectPresenceAttribute extends InformationUnit {

    //String retrievedText; Removed as it was not useful to have it stored here. There is a place in the InformationUnitClasse (MostLikelyAnswer.key)
    float simThreshold;
    String name;

    public AbstractDetectPresenceAttribute(InformationExtractor extractor, String contentFieldName, AttributeType type, String attribId) {
        super(extractor, contentFieldName, type);
        this.attribId = attribId;

        setName();
        setThreshold();
        setQuery();
        typePresenceDetect = true;
    }

    /**
    *    The purpose of this function is to make sure that
    *    the flow for the DP attributes is consistent with the VE ones.
    *    For the DP type attributes, there's no particular candidate
    *    answer for which scores need to be aggregated. Instead, the
    *    whole retrieved passage is a candidate answer.
    *
    *    The job of the overrideen function is hence to set bestAnswer
    *    correctly for consistent aggregation.
    *
    * @param window A supplied text snippet.
    * @param q A query pertaining to a BCT attribute.
    * @param sim Similarity threshold that is used to decide the presence/absence of a BCT.
    */
    
    @Override
    public void construct(String window, Query q, float sim) {

        String retrievedText = PaperIndexer.analyze(analyzer, window);
        weight = sim;

        logger.debug("Inside AbstractDetectPresenceAttribute.construct(): " + window + "(" + weight + ")");

        if (weight > this.simThreshold) {
            this.mostLikelyAnswer = new CandidateAnswer(retrievedText, 0, window); // second parameter is unused  
        } else {
            logger.debug("BUT weight is below threshold of " + this.simThreshold);
            this.mostLikelyAnswer = null;  // clear the previous most likely answer
        }
    }

    /**
     * Defines the number of passages wanted.
     * @return
     */    
    @Override
    public int numWanted() {
        return Integer.parseInt(extractor.prop.getProperty("attributes.typedetect.numwanted", "3"));
    }

    void setName() {
        if (this.extractor.refBuilder != null) {
            CodeSetTree gt = this.extractor.refBuilder.getGroundTruths(type.code());
            CodeSetTreeNode refNode = gt.getNode(attribId);
            this.name = refNode.getName();
        }
    }

    /**
     * 
     */
    void setThreshold() {
        this.simThreshold = Float.parseFloat(extractor.prop.getProperty("attributes.typedetect.threshold", "0.6"));
        // Allow provision for different thresholds for different attributes
        String val = extractor.prop.getProperty("attributes.typedetect.threshold." + this.attribId);
        if (val != null) {
            // override the default threshold
            this.simThreshold = Float.parseFloat(val);
        }
    }

    //Overides both the generic and specific values for com.ibm.drl.hbcp.experiments
    public void setThreshold(double t) {
        this.simThreshold = (float) t;
    }

    void setQuery() {
        this.query = extractor.prop.getProperty("attributes.typedetect.query." + this.attribId);
    }

    @Override
    boolean isValidCandidate(String word) {
        return true;
    }

    // The predicted values
    @Override
    public void setProperties() {
    }

    /**
     * Defines if the extracted value is correct or not (in comparison to the available
     * ground-truth data).
     *
     * @param gt A pointer to the 'CodeSetTree' tree object, each node of which contains the ground-truth value
     * @param predicted 'InformationUnit' object containing the predicted value.
     * @param rc Acts as an output parameter to accumulate the results of comparisons. The metric that is
     * updated i particular for this type of attribute is the 'accuracy'.
     */    
    @Override
    public void compareWithRef(CodeSetTree gt, InformationUnit predicted, RefComparison rc, boolean armification) {
        String ref = null;
        CodeSetTreeNode refNode = gt.getNode(attribId);
        
        if (refNode != null){
            if(armification){
                ref = refNode.getAnnotatedContextAllArmsContatenated(docName);
            }else{
                ref = refNode.getAnnotatedText(docName);
            }
        }
        
        String predictedText = null;
        if (predicted != null) {
            predictedText = predicted.mostLikelyAnswer.getKey();
        }

        // A Boolean check for the time being.
        logger.info("Predicted: '" + predictedText + "', Ref: '" + ref + "'");

        if (predictedText == null && ref == null) {
            rc.tn++;
        } else if (predictedText != null && ref != null) {
            rc.tp++;
            String pp_ref = PaperIndexer.analyze(analyzer, ref);
            DocVector x = new DocVector(extractor.prop, pp_ref);
            DocVector y = new DocVector(extractor.prop, predictedText);
            rc.meteor += y.computeMETEOR(x);
        } else if (predictedText == null && ref != null) {
            rc.fn++;
        } else {
            rc.fp++;
        }
    }

    @Override
    public String getName() {
        return name;
    }

    @Override
    public void appendFields(Document doc) {
        doc.add(new Field(InformationUnit.ATTRIB_ID_FIELD,
                String.valueOf(attribId),
                Field.Store.YES, Field.Index.NOT_ANALYZED));
    }

    @Override
    public boolean matches(String attribId) {
        return this.attribId.equals(attribId);
    }

    @Override
    void learnThreshold(CVSplit cvsplit,
            InformationExtractor extractor) throws Exception {

        logger.info("Grid search for optimal threshold for attribute " + this.getName());

        RefComparison maxRC = new RefComparison();

        float bestThresh = 0.0f;

        // Iterate over batches of grid search
        CodeSetTree gt = extractor.refBuilder.getGroundTruths(this.type.code());

        for (int ithresh = 1; ithresh <= 9; ithresh++) {
            float thresh = ithresh / 10.0f;
            this.setThreshold(thresh);

            RefComparison batchEval = new RefComparison();  // this is passed by reference and incremented in the called functions.

            for (DocIdNamePair dnp : cvsplit.trainDocs) {  // iterate over docs in training set

                InformationUnit predicted = extractor.extractInformationFromDoc(dnp.id, this, false);

                if (predicted != null) {
                    predicted.setProperties();
                }

                this.compareWithRef(gt, predicted, batchEval, extractor.armification);
            }

            batchEval.computeFscoreVal();
            logger.info("IUnit - " + getName()
                    + ": Fscore (" + thresh + ") = " + batchEval.fscore);

            if (batchEval.fscore > maxRC.fscore) {
                maxRC = batchEval;
                bestThresh = thresh;
            }
        }

        logger.info("IUnit - " + getName() + ": learned threshold value = " + bestThresh);
        this.simThreshold = bestThresh;
    }
}
