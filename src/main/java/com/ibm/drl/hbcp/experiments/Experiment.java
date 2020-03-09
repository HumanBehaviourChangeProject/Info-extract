/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extractor.AbstractDetectPresenceAttribute;
import com.ibm.drl.hbcp.extractor.InformationExtractor;
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoIndexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;

/**
 *
 * @author francesca
 */
class Experiment {

    private final String mAttribId;
    private final String mAttributeName;
    RefComparison res;
    private final AbstractDetectPresenceAttribute mIU;
    InformationExtractor mExtractor;
    String mPropFile;
    
    
        public Experiment(String attribId, String propFile) throws Exception {
           this.mPropFile=propFile;
           this.mAttribId = attribId;
           //Need to create an informationExtrac
           mExtractor = new InformationExtractor(mPropFile);
           mIU = new AbstractDetectPresenceAttribute(mExtractor, ResearchDoc.FIELD_CONTENT, AttributeType.INTERVENTION, this.mAttribId);
           this.mAttributeName=mIU.getName();
        }

    void runExperiment(boolean supervised, boolean withNB, double threshold) throws Exception {
        
        
        if (supervised){
          mExtractor.ieSupervised(mIU);
          this.res = mIU.getEval();
        }
        else {
            ExtractedInfoIndexer ieIndexer = new ExtractedInfoIndexer(mExtractor.getPropFile());
           
            //performs the evaluation and stores the results
            mExtractor.extractInformationIU(mIU, ieIndexer);
            this.res = mIU.getEval();
            ieIndexer.close(); 
         }
     }
    
    
    void printAttributeResults(){
    //print results
        String prec = String.valueOf(res.getPrec1());
        String recall = String.valueOf(res.getRecall1());
        String meteor= String.valueOf(res.getMeteor1());
        String acc= String.valueOf(res.getAccuracy1());
        String fscore = String.valueOf(res.getFscore1());
        
        System.out.print(this.mAttribId+"("+ mAttributeName +")\t"+prec+"\t"+recall+"\t"+fscore+"\t"+acc+"\t"+meteor+"\n");
        
        
        
    }

    public String exportAttributeResults() {
        String results="";
        String prec = String.valueOf(res.getPrec1());
        String recall = String.valueOf(res.getRecall1());
        String meteor= String.valueOf(res.getMeteor1());
        String acc= String.valueOf(res.getAccuracy1());
        String fscore = String.valueOf(res.getFscore1());
        
        results= this.mAttribId+"("+ mAttributeName +")\t"+prec+"\t"+recall+"\t"+fscore+"\t"+acc+"\t"+meteor+"\n";
        
        return results;
    }

    
    
    
  }
 



