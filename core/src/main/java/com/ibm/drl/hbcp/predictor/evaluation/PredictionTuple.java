/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.List;
import lombok.Data;

/**
 *
 * @author dganguly
 */
@Data
public class PredictionTuple {
    private final double ref;
    private final double pred;
    
    public PredictionTuple(double ref, double pred) {
        this.ref = ref;
        this.pred = pred;
    }
    
    public double getSquaredError() {
        double error = ref-pred;
        return error*error;
    }
    
    public boolean valid() { return ref >= 0 && pred >= 0; } 
    
    static public void saveTuplesToFile(List<PredictionTuple> rtuples) {
        try {            
            // Print out the result tuples in a file
            FileWriter fw = new FileWriter("res.tsv");
            BufferedWriter bw = new BufferedWriter(fw);
            
            for (PredictionTuple rt: rtuples) {
                bw.write(rt.getRef() + "\t" + rt.getPred());
                bw.newLine();
            }
            bw.close();
            fw.close();
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
}
