/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.api;

import java.util.ArrayList;
import java.util.List;

/**
 * A container class for the IUnitPojo instances.
 *
 * @author dganguly
 */
public class IUnitPOJOs {
    List<IUnitPOJO> ius;
    
    public IUnitPOJOs() {
        ius = new ArrayList<>();
    }

    public IUnitPOJOs(IUnitPOJO iu) {
        ius = new ArrayList<>();
        ius.add(iu);
    }
    
    public void add(IUnitPOJO iu) { ius.add(iu); }
    
    public List<IUnitPOJO> getIUs() {
        return ius;
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff
            .append("{")
            .append("\n")
            .append("\"_id\": \"")
            .append(ius.get(0).docName)
            .append("\",")
            .append("\n")
            .append("\"extractedValues\": [")
            .append("\n")
        ;

        int i = 0;
        int numIUs = ius.size();
        for (IUnitPOJO iu: ius) {
            buff.append(iu.toString());
            if (++i != numIUs)
                buff.append(",\n");
        }
        buff
            .append("\n")
            .append("]")
            .append("}")
        ;
        
        return buff.toString();
    }
}
