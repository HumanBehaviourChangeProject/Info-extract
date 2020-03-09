/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;

/**
 *
 * @author yhou
 */
 public class NERToken {
    public String word;
    public String postag;
    public String nertag;

    public NERToken(String word, String postag, String nertag) {
        this.word = word;
        this.postag = postag;
        this.nertag = nertag;
    } 
    
    public void setNER(String tag) {
        this.nertag = tag;
    }
}
