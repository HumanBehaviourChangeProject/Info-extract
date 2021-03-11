package com.ibm.drl.hbcp.predictor.data;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class CrossValidationSplitter implements DataSplitter {

    private final int k;

    private final Random random = new Random(0);

    /** Creates a k-fold cross-validation splitter */
    public CrossValidationSplitter(int k) {
        this.k = k;
    }

    @Override
    public <E> List<Pair<List<E>, List<E>>> getTrainTestSplits(List<E> all) {
        List<Pair<List<E>, List<E>>> res = new ArrayList<>();
        all = new ArrayList<>(all);
        Collections.shuffle(all, random);
        for (int i = 0; i < k; i++) {
            int testStartIndex = i * (all.size() / k);
            int testEndIndex = testStartIndex + (all.size() / k);
            List<E> test = new ArrayList<>(all.subList(testStartIndex, testEndIndex));
            List<E> train = new ArrayList<>(all.subList(0, testStartIndex));
            train.addAll(new ArrayList<>(all.subList(testEndIndex, all.size())));
            res.add(Pair.of(train, test));
        }
        return res;
    }
}
