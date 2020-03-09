package com.ibm.drl.hbcp.experiments.prediction.noncontrolprediction;

import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class SimpleSplitter {

    private static final Random random = new Random(0);

    public static <E> Pair<List<E>, List<E>> getTrainTest(List<E> all, double ratio) {
        all = new ArrayList<>(all);
        Collections.shuffle(all, random);
        int trainSize = (int) (all.size() * ratio);
        List<E> train = new ArrayList<>(all.subList(0, trainSize));
        List<E> test = new ArrayList<>(all.subList(trainSize, all.size()));
        return Pair.of(train, test);
    }

    public static <E> List<Pair<List<E>, List<E>>> getKFoldCrossValidationSplits(List<E> all, int k) {
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
