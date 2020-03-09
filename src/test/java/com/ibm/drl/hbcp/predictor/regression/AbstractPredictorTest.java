package com.ibm.drl.hbcp.predictor.regression;

import com.ibm.drl.hbcp.predictor.evaluation.PredictionTuple;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.Assert.*;

public class AbstractPredictorTest {

    @Test
    public void rmseEvaluator() {
        float[] col1 = new float[]{18.471251f,21.604622f,16.402195f,13.740966f,22.207201f,18.288637f,15.034162f,21.313107f,9.641665f,15.748825f,0,-1.1f,0};
        float[] col2 = new float[]{6.7f,32.799999f,21.200001f,8,28f,13.5f,13.3f,19,6.6f,10,3.4f,4.5f,0};

        List<PredictionTuple> tuples = new ArrayList<>(col1.length);
        for (int i = 0; i < col1.length; i++) {
            final PredictionTuple tuple = new PredictionTuple(col1[i], col2[i]);
            tuples.add(tuple);
        }
        //assertEquals(6.012439097, AbstractPredictorTest., 0.001);
    }
}