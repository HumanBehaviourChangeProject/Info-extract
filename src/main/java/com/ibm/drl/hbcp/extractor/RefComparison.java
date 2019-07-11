/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extractor;

/**
 * Defines the necessary variables used to compute precision and recall,
 * namely the 'true and false' positives and negatives.
 * The extractor flow uses an instance of this class to aggregate the values
 * over a set of documents and a set of attributes.
 * 
 * @author dganguly
 */
public class RefComparison {
    int tp;
    int fp;
    int fn;
    int tn;
    
    float threshold;
    
    float accuracy;
    float recall;
    float precision;
    float fscore;
    float meteor;  // sim between GT and retrieved, only for text
    
    String getFscore() { // +ve class
        computeFscoreVal();
        return String.valueOf(fscore);
    }
    
    String getRecall() { // +ve class
        computeRecallVal();
        return recall+ " ("+ tp + "/" + (tp+fn) + ")";    
    }
     
    
    String getPrec() { // +ve class
        computePrecisionVal();
        return precision +" (" + tp + "/" + (tp+fp) + ")";
    }
    
    public String getMeteor() {
        meteor = this.computeMeteorWithGT();
        return String.valueOf(meteor);       
    }
   
    
    public float getPrec1() { // +ve class
        computePrecisionVal();
        return precision;
    }
    
    public float getRecall1() { // +ve class
        computeRecallVal();
        return recall;
        
    }
    public float getFscore1() { // +ve class
        computeFscoreVal();
        return fscore;
    }
    
     public float getAccuracy1(){
        return computeAccuracyVal();      
    }
     
     public float getMeteor1() {
         meteor = this.computeMeteorWithGT();
         return meteor;       
     }
    
     

    
    float computeAccuracyVal() { accuracy = tp+fp+fn+tn==0? 0 : (tp+tn)/(float)(tp+fp+fn+tn); return accuracy; } 
    float computePrecisionVal() { precision = tp+fp==0? 0 : tp/(float)(tp+fp); return precision; } 
    float computeRecallVal() { recall = tp+fn==0? 0 : tp/(float)(tp+fn); return recall; } 
    
    float computeFscoreVal() {
        if (precision==0)
            computePrecisionVal();
        if (recall==0)
            computeRecallVal();
        this.fscore = precision+recall==0? 0 : 2*precision*recall/(precision+recall);
        return fscore;
    } 
    
    float computeMeteorWithGT() {
        return tp==0? 0 : meteor/(float)tp;
    }
    
    String getAccuracy() {
        
        computeAccuracyVal();
        
        StringBuffer buff = new StringBuffer();
        buff.append(accuracy);
        
        buff.append("\nConfusion Matrix:\n");
        buff.append(tp).append("\t").append(fp).append("\n")
                .append(fn).append("\t").append(tn).append("\n");
        
        return buff.toString();
    }
    
    public void compute() {
        computeAccuracyVal();
        computePrecisionVal();
        computeRecallVal();
        computeFscoreVal();
        computeMeteorWithGT();
    }
    
    /**
     * Computes and returns a formatted string of the precision. recall and accuracy
     * measures of this object, which represents an aggregate measure over a set
     * of documents and attributes.
     * @param isDP
     * @param detailed
     * @param needCompute whether to recompute precision, recall etc. from scratch
     * @return 
     */
    public String toString(boolean isDP, boolean detailed, boolean needCompute) {
        
        if (needCompute) {
            this.compute();    
        }
        
        StringBuffer buff = new StringBuffer();
        if (detailed) {
            buff.append("Precision =").append(this.getPrec()).append("\t");
            buff.append("Recall =").append(this.getRecall()).append("\t");
            buff.append("F-score =").append(this.getFscore()).append("\t");
            if (isDP) {
                buff.append("Accuracy =").append(this.getAccuracy());
                buff.append("Avg METEOR =").append(this.getMeteor());
            }    
        } else {
            buff.append("Precision =").append(this.precision).append("\t");
            buff.append("Recall =").append(this.recall).append("\t");
            buff.append("F-score =").append(this.fscore).append("\t");
            if (isDP) {
                buff.append("Accuracy =").append(this.accuracy).append("\t");
                buff.append("Avg METEOR =").append(this.getMeteor());
            }
        }
        
        return buff.toString();
        
    }
}




