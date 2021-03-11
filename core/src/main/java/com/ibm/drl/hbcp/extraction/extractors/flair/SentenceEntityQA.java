/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extraction.extractors.flair;

/**
 *
 * @author yhou
 */
public class SentenceEntityQA {
        public String question;
        public String context;
        public String entity;
        public Answer answer;
    
    public SentenceEntityQA() {
    }

    public class Answer{
        public float score;
        public int start;
        public int end;
        public String answer;
        
    }
    

    
}
