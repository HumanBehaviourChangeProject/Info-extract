/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import java.util.List;

/**
 * Defines an arbitrary block of text (indexable unit) specified with an id and a content.
 * 
 * @author dganguly
 */
public class Paragraph {
    public String id;
    public String content;

    public Paragraph(String id, String content) {
        this.id = id;
        this.content = content;
    }
    
    public Paragraph(String id, List<String> tokens) {
        this.id = id;
        StringBuffer buff = new StringBuffer();
        
        for (String token : tokens) {
            buff.append(token).append(" ");
        }
        content = buff.toString();
    }
}


