/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.extraction.extractors.flair;

import java.util.List;

/**
 * Used by Gson to parse the output of the Flair REST API calls.
 *
 * @author yhou
 */
public class SentenceEntity {
        public String text;
        public List<Label> labels;
        public List<Entity> entities;
    
    public SentenceEntity() {
    }

    public class Label{
        
    }
    
    public class Entity{
        public String text;
        public int start_pos;
        public int end_pos;
        public String type;
        public String confidence;
    }
    
}
