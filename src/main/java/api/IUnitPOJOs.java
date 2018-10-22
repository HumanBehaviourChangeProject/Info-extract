/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package api;

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
    
    void add(IUnitPOJO iu) { ius.add(iu); }
    
    public List<IUnitPOJO> getIUs() {
        return ius;
    }
}
