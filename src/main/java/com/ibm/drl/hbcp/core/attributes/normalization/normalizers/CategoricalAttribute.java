/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.attributes.normalization.normalizers;

import java.util.ArrayList;
import java.util.List;

/**
 *
 * @author dganguly
 */
public class CategoricalAttribute {
    String attributeId;
    List<String> categoryValues;

    public CategoricalAttribute(String attributeId) {        
        this.attributeId = attributeId;
        this.categoryValues = new ArrayList<>();
    }
    
    public void add(String value) {
        categoryValues.add(value);
    }    
}
