package com.ibm.drl.hbcp.predictor.data;

import org.apache.commons.lang3.tuple.Pair;
import java.util.List;

public interface DataSplitter {

    <E> List<Pair<List<E>, List<E>>> getTrainTestSplits(List<E> all);
}
