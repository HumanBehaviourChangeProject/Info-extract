/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extraction.extractors.flair;

import java.util.List;

/**
 *
 * @author yhou
 */
public class SentenceEntityNew {
        public String text;
        public List<Label> labels;
        public List<Entity> entities;
    
    public SentenceEntityNew() {
    }

    public class Label{
        public String _value;
        public String _score;
        
    }
    
    public class Entity{
        public String text;
        public int start_pos;
        public int end_pos;
        public List<Label> labels;
    }
    
}
