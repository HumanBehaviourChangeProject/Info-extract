package com.ibm.drl.hbcp.predictor.data;

import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

// Returns a 5-fold split of the data

public class CVSplitter implements DataSplitter {
    private final int numFolds;
    private final Random random;
    int foldIndex;

    public CVSplitter(int numFolds, long seed) {
        this.numFolds = numFolds;
        random = new Random(seed);
        foldIndex = 0;
    }

    public CVSplitter(int numFolds) {
        this(numFolds, 0);
    }

    @Override
    // This function is called numFolds time. This is a stateful call (foldIndex),
    // which keeps track of how many times this function has been called.
    // This resets to zero after numFold calls.
    public <E> List<Pair<List<E>, List<E>>> getTrainTestSplits(List<E> all) {
        if (foldIndex==0) {
            Collections.shuffle(all, random);
        }
        
        int numInstances = all.size();
        int testIndexStart = foldIndex * numInstances/numFolds;
        int testIndexEnd = (foldIndex+1) * numInstances/numFolds;
        List<E> test = new ArrayList<>(), train = new ArrayList<>();
        
        for (int i=0; i < numInstances; i++) {
            if (testIndexStart<=i && i < testIndexEnd) {
                test.add(all.get(i));
            }
            else {
                train.add(all.get(i));
            }
        }
        
        foldIndex = (foldIndex + 1)%numFolds;
        System.out.println(String.format("Train-size = %d, Test-size = %d", train.size(), test.size()));
        System.out.println(String.format("Test-Index = (%d, %d)", testIndexStart, testIndexEnd));
        
        return Lists.newArrayList(Pair.of(train, test));
    }
}
