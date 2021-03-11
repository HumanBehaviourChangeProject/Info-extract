/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.evaluation.metrics;

import java.util.List;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;

/**
 *
 * @author debforit
 */
public class MedianBased7ClassesAccuracyMetric implements MeanMetric {

    List<PredictionTuple> predictions;
    float[] classTransitionMarks;
    
    static final int NUM_CLASSES = 7;
    
    public MedianBased7ClassesAccuracyMetric(List<PredictionTuple> predictions) {
        this.predictions = predictions;
        classTransitionMarks = new float[NUM_CLASSES-1];
        
        Percentile p = new Percentile();
        double[] refs = ArrayUtils.toPrimitive(
                predictions.stream().map(
                    prediction -> prediction.getRef())
                .toArray(Double[]::new)
        );
        p.setData(refs);
        
        int percentile_point = 0;        
        for (int i=0; i < NUM_CLASSES-1; i++) {
            percentile_point += (int)100/NUM_CLASSES;
            classTransitionMarks[i] = (float)p.evaluate(percentile_point);
        }
        
        // print out the class distributions
        int counts[] = new int[NUM_CLASSES];
        for (double ref: refs) {
            counts[getClass(ref)]++;
        }
        System.out.println("Class distribution: ");
        for (int i=0; i < counts.length; i++)
            System.out.println(i + ", " + counts[i]);
    }
    
    // the class definition depends on the entire distribution (not just on the training fold)
    int getClass(double value) {
        for (int i=0; i < classTransitionMarks.length; i++) {
            if (value < classTransitionMarks[i])
                return i;
        }
        return classTransitionMarks.length;
    }
    
    @Override
    public double compute(double predicted, double ref) {
        int predicted_discrete = getClass(predicted);
        int ref_discrete = getClass(ref);
        return predicted_discrete==ref_discrete? 1: 0;
    }
    
    @Override
    public String toString() {
        return "7-class (based on percentiles) Accuracy";
    }    
}
