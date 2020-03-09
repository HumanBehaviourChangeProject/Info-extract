/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import org.apache.commons.lang3.StringUtils;

import java.util.List;

/**
 * Defines an arbitrary block of text (indexable unit) specified with an id and a content.
 * 
 * @author dganguly
 */
public class Paragraph {
    public String id;
    public String content;
    public IndexingMethod indexingMethod;

    public Paragraph(String id, String content, IndexingMethod indexingMethod) {
        this.id = id;
        this.content = content;
        this.indexingMethod = indexingMethod;
    }

    public Paragraph(String id, String content) {
        this(id, content, IndexingMethod.NONE);
    }
    
    public Paragraph(String id, List<String> tokens, IndexingMethod indexingMethod) {
        this(id, StringUtils.join(tokens, " "), indexingMethod);
    }
}


