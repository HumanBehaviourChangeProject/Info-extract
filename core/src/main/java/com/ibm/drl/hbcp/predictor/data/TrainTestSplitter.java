package com.ibm.drl.hbcp.predictor.data;

import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Collections;
import org.apache.commons.lang3.tuple.Pair;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public class TrainTestSplitter implements DataSplitter {

    private final double trainTestRatio;

    private final Random random;

    public TrainTestSplitter(double trainTestRatio, long seed) {
        this.trainTestRatio = trainTestRatio;
        random = new Random(seed);
    }

    public TrainTestSplitter(double trainTestRatio) {
        this(trainTestRatio, 0);
    }

    @Override
    public <E> List<Pair<List<E>, List<E>>> getTrainTestSplits(List<E> all) {
        all = new ArrayList<>(all);
        Collections.shuffle(all, random);
        int trainSize = (int) (all.size() * trainTestRatio);
        List<E> train = new ArrayList<>(all.subList(0, trainSize));
        List<E> test = new ArrayList<>(all.subList(trainSize, all.size()));
        return Lists.newArrayList(Pair.of(train, test));
    }
}
