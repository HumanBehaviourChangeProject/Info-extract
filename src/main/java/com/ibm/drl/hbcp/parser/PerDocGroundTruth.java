/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.parser;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import java.util.HashMap;
import java.util.List;

/**
 *
 * @author dganguly
 */
public class PerDocGroundTruth {
    HashMap<String, List<Attribute>> gt;

    public PerDocGroundTruth(HashMap<String, List<Attribute>> gt) {
        this.gt = gt;
    }

    
}
