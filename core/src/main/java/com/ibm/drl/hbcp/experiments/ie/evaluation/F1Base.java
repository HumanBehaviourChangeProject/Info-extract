package com.ibm.drl.hbcp.experiments.ie.evaluation;

import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.common.collect.ConcurrentHashMultiset;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;

import lombok.Value;

public abstract class F1Base extends Metric {

    private static final Comparator<List<Double>> f1Comparator = new Comparator<List<Double>>() {
        @Override
        public int compare(List<Double> o1, List<Double> o2) {
            return Double.compare(
                    getModifiedF1ForOrdering(o1.get(3), o1.get(4), o1.get(5)),
                    getModifiedF1ForOrdering(o2.get(3), o2.get(4), o2.get(5))
            );
        }
    };

    @Override
    public List<Double> evaluate(Attribute attribute, AttributeValueCollection<? extends ArmifiedAttributeValuePair> extracted,
                                 AttributeValueCollection<? extends ArmifiedAttributeValuePair> reference, Set<String> referenceDocNames) {
        Counts confusionMatrix = getConfusionMatrix(extracted, reference, referenceDocNames);
        return evaluate(confusionMatrix);
    }

    private List<Double> evaluate(Counts confusionMatrix) {
        // precision = TP / (TP + FP)
        double precision = (double)confusionMatrix.getTruePositives() / (confusionMatrix.getTruePositives() + confusionMatrix.getFalsePositives());
        // recall = TP / (TP + FN)
        double recall = (double)confusionMatrix.getTruePositives() / (confusionMatrix.getTruePositives() + confusionMatrix.getFalseNegatives());
        // F1 = 2PR/(P+R)
        double f1 = 2 * precision * recall / (precision + recall);
        return Lists.newArrayList(
                precision, recall, f1,
                (double)confusionMatrix.getTruePositives(),
                (double)confusionMatrix.getFalsePositives(),
                (double)confusionMatrix.getFalseNegatives()
        );
    }

    private Counts getConfusionMatrix(AttributeValueCollection<? extends ArmifiedAttributeValuePair> compared,
                                          AttributeValueCollection<? extends ArmifiedAttributeValuePair> comparedWith,
                                          Set<String> docNames) {
        Counts counts = new Counts(0, 0, 0);
        for (String docName : docNames) {
            counts = counts.add(getConfusionMatrixInDocument(docName, compared, comparedWith));
        }
        return counts;
    }

    private Counts getConfusionMatrixInDocument(String docName, AttributeValueCollection<? extends ArmifiedAttributeValuePair> compared,
                                           AttributeValueCollection<? extends ArmifiedAttributeValuePair> reference) {
        // compute the sets of values for both collections
        Set<String> valuesCompared = compared.byDoc().getOrDefault(docName, ConcurrentHashMultiset.create()).stream()
                .map(this::getNormalForm)
                .collect(Collectors.toSet());
        Set<String> valuesReference = reference.byDoc().getOrDefault(docName, ConcurrentHashMultiset.create()).stream()
                .map(this::getNormalForm)
                .collect(Collectors.toSet());
        // compute true positives (intersection)
        Set<String> truePositives = new HashSet<>(valuesCompared).stream()
                .filter(v -> valuesReference.stream().anyMatch(ref -> areValuesMatched(v, ref)))
                .collect(Collectors.toSet());
        // compute false positives (in compared, not in reference)
        Set<String> falsePositives = new HashSet<>(valuesCompared).stream()
                .filter(v -> valuesReference.stream().noneMatch(ref -> areValuesMatched(v, ref)))
                .collect(Collectors.toSet());
        // compute false negatives (in reference, not in compared)
        Set<String> falseNegatives = new HashSet<>(valuesReference).stream()
                .filter(ref -> valuesCompared.stream().noneMatch(v -> areValuesMatched(v, ref)))
                .collect(Collectors.toSet());
        return new Counts(truePositives.size(), falsePositives.size(), falseNegatives.size());
    }

    private boolean areValuesMatched(String value1, String value2) {
        return value1.contains(value2) || value2.contains(value1);
    }

    protected String getNormalForm(ArmifiedAttributeValuePair value) {
        return value.getValue();
    }

    @Value
    private static class Counts {
        int truePositives;
        int falsePositives;
        int falseNegatives;

        public Counts add(Counts toAdd) {
            return new Counts(truePositives + toAdd.truePositives,
                    falsePositives + toAdd.falsePositives,
                    falseNegatives + toAdd.falseNegatives);
        }
    }

    public List<Double> aggregate(List<List<Double>> evaluationVectors) {
        List<Counts> confusionMatrices = evaluationVectors.stream()
                .map(vector -> new Counts(
                        (int)Math.round(vector.get(3)),
                        (int)Math.round(vector.get(4)),
                        (int)Math.round(vector.get(5))))
                .collect(Collectors.toList());
        Counts totals = confusionMatrices.stream().reduce(new Counts(0, 0, 0), Counts::add);
        return evaluate(totals);
    }

    private static double getModifiedF1ForOrdering(double tp, double fp, double fn) {
        double precision = (1 + tp) / (1 + tp + fp);
        double recall = (1 + tp) / (1 + tp + fn);
        return precision * recall / (precision + recall);
    }

    @Override
    public Comparator<List<Double>> getVectorComparator() {
        return f1Comparator;
    }
}
