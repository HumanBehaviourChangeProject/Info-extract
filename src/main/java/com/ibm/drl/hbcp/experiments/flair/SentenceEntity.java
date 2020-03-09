/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;

import java.util.List;

/**
 *
 * @author yhou
 */
public class SentenceEntity {
        String text;
        List<Label> labels;
        List<Entity> entities;
    
    public SentenceEntity() {
    }
    
    public class Label{
        
    }
    
    public class Entity{
        String text;
        String start_pos;
        String end_pos;
        String type;
        String confidence;
    }
    
}
