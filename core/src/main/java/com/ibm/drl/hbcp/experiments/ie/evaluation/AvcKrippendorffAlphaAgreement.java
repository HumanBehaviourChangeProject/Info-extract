package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.*;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

/**
 * This implementation of Krippendorff's alpha is specific to measuring agreement between AttributeValueCollections,
 * usually for a specific attribute.  Alpha is generally a flexible agreement metric https://en.wikipedia.org/wiki/Krippendorff%27s_alpha
 * but here we limit the annotators/coders to two (i.e., two AVCs).  We also assume the 'difference function' is *nominal*
 * and that annotation for a 'unit' is binary, i.e., present or not.
 * The 'units' of annotation differ depending on the annotation type of the attribute.  'Presence type' attributes have
 * document units (e.g., the annotation is present or not for a given attribute in a given document).  The 'units' of annotation for the
 * remaining attributes, 'value type' and 'complex name-value type', are entities.  Each annotated entity from either
 * annotators is treated as a unit.  Entities annotated by both annotators are consider the same unit, and labeled as present/true
 * for both annotators. Entities only labeled by one annotator are marked as present/true only for that annotator and
 * marked as 'not present'/false for the other annotator.
 *
 *
 * @author charlesj
 */
public class AvcKrippendorffAlphaAgreement extends Metric {

    @Override
    public List<Double> evaluate(Attribute attribute, AttributeValueCollection<? extends ArmifiedAttributeValuePair> extracted, AttributeValueCollection<? extends ArmifiedAttributeValuePair> reference, Set<String> referenceDocNames) {
        // AVCs should already be filtered by attribute
        List<Boolean> extractedUnits = new ArrayList<>();
        List<Boolean> referenceUnits = new ArrayList<>();
        for (String docName : referenceDocNames) {
            Multiset<? extends ArmifiedAttributeValuePair> extractedPairs = extracted.getPairsInDoc(docName);
            Multiset<? extends ArmifiedAttributeValuePair> referencePairs = reference.getPairsInDoc(docName);
            if (ValueType.isPresenceType(attribute)) {
                // document unit label 'true' if annotation is present
                extractedUnits.add(extractedPairs != null && !extractedPairs.isEmpty());
                referenceUnits.add(referencePairs != null && !referencePairs.isEmpty());
            } else {
                // collect annotation across entity units
                Pair<List<Boolean>, List<Boolean>> docEntityUnits = getEntityUnits(extractedPairs, referencePairs);
                extractedUnits.addAll(docEntityUnits.getLeft());
                referenceUnits.addAll(docEntityUnits.getRight());
            }
        }
        double alpha = calculateKrippendorffAlpha(extractedUnits, referenceUnits);
        return Lists.newArrayList(alpha);
    }

    private double calculateKrippendorffAlpha(List<Boolean> extractedUnits, List<Boolean> referenceUnits) {
        Boolean[] extUnits = extractedUnits.toArray(new Boolean[1]);
        Boolean[] refUnits = referenceUnits.toArray(new Boolean[1]);
        double[][] coincidenceMatrix = constructCoincidenceMatrix(extUnits, refUnits);

        // Krippendorff's alpha is typically defined over multiple units and multiple labels.  If we find only one label we'll just consider it perfect agreement
        if (hasPerfectAgreementWithOneLabel(coincidenceMatrix)) {
            return 1.0;
        }
        double observed = calculateObservedDisagreement(coincidenceMatrix);
        double expected = calculateExpectedDisagreement(coincidenceMatrix);
        return 1 - (observed / expected);
    }

    private boolean hasPerfectAgreementWithOneLabel(double[][] coincidenceMatrix) {
        return (coincidenceMatrix[0][0] + coincidenceMatrix[0][1] + coincidenceMatrix[1][0] == 0);
    }

    private double[][] constructCoincidenceMatrix(Boolean[] extUnits, Boolean[] refUnits) {
        // only for two annotators, and only binary annotation decisions
        double[][] coincidenceMatrix = new double[2][2];
        for (int i = 0; i < extUnits.length; i++) {
            if (extUnits[i]) {
                if (refUnits[i]) {
                    coincidenceMatrix[1][1] += 2;  // (1 + 1)/2-1
                } else {
                    coincidenceMatrix[0][1] += 1;  // 1/2-1
                    coincidenceMatrix[1][0] += 1;  // 1/2-1
                }
            } else {
                if (refUnits[i]) {
                    coincidenceMatrix[1][0] += 1;  // (1)/2-1
                    coincidenceMatrix[0][1] += 1;  // (1)/2-1
                } else {
                    coincidenceMatrix[0][0] += 2;  // (1 + 1)/2-1
                }
            }
        }
        return coincidenceMatrix;
    }

    private double calculateObservedDisagreement(double[][] coincidenceMatrix) {
        double observed = 0.0;
        for (int v = 0; v < coincidenceMatrix.length-1; v++) {
            for (int w = v+1; w < coincidenceMatrix.length; w++) {
                observed += coincidenceMatrix[v][w];
            }
        }
        return observed;
    }

    private double calculateExpectedDisagreement(double[][] coincidenceMatrix) {
        double[] vFrequencies = getVfrequencies(coincidenceMatrix);
        double n = Arrays.stream(vFrequencies).sum();

        double expected = 0.0;
        for (int v = 0; v < vFrequencies.length-1; v++) {
            for (int w = v+1; w < vFrequencies.length; w++) {
                expected += vFrequencies[v] * vFrequencies[w];
            }
        }
        return expected * (1 / (n - 1));
    }

    private double[] getVfrequencies(double[][] coincidenceMatrix) {
        double[] frequencies = new double[coincidenceMatrix.length];
        for (int i = 0; i < coincidenceMatrix.length; i++) {
            frequencies[i] = Arrays.stream(coincidenceMatrix[i]).sum();
        }
        return frequencies;
    }

    private Pair<List<Boolean>, List<Boolean>> getEntityUnits(Multiset<? extends ArmifiedAttributeValuePair> extAvps, Multiset<? extends ArmifiedAttributeValuePair> refAvps) {
        List<Boolean> extractedEntityUnits = new ArrayList<>();
        List<Boolean> referenceEntityUnits = new ArrayList<>();
        Set<String> extValues;
        if (extAvps != null) {
            extValues = extAvps.stream().map(AttributeValuePair::getValue).collect(Collectors.toSet());
        } else {
            extValues = new HashSet<>(0);
        }
        Set<String> refValues;
        if (refAvps != null) {
            refValues = refAvps.stream().map(AttributeValuePair::getValue).collect(Collectors.toSet());
        } else {
            refValues = new HashSet<>(0);
        }
        for (Iterator<String> extIter = extValues.iterator(); extIter.hasNext();) {
            String extValue = extIter.next();
            for (Iterator<String> refIter = refValues.iterator(); refIter.hasNext();) {
                String refValue = refIter.next();
                if (isMatch(extValue, refValue)) {
                    // matching values consider one 'unit' with same annotation
                    extIter.remove();
                    refIter.remove();
                    extractedEntityUnits.add(true);
                    referenceEntityUnits.add(true);
                    break;
                }
            }
        }
        // treat remaining annotation as not matching annotation
        for (int i = 0; i < extValues.size(); i++) {
            extractedEntityUnits.add(true);
            referenceEntityUnits.add(false);
        }
        for (int i = 0; i < refValues.size(); i++) {
            extractedEntityUnits.add(false);
            referenceEntityUnits.add(true);
        }
        return Pair.of(extractedEntityUnits, referenceEntityUnits);
    }

    private boolean isMatch(String extractedValue, String referenceValue) {
        return extractedValue.contains(referenceValue) || referenceValue.contains(extractedValue);
    }

    @Override
    public boolean isRelevantAttribute(Attribute attribute) {
        return ValueType.isPresenceType(attribute) || ValueType.isValueType(attribute);
    }

    public static void main(String[] args) throws IOException {
        File file1 = new File("../data/jsons/IRR_Automation_Jsons/Prioritised Entities_Rank3/Group10_CM.json");
        File file2 = new File("../data/jsons/IRR_Automation_Jsons/Prioritised Entities_Rank3/Group10_EN.json");

        AttributeValueCollection<AnnotatedAttributeValuePair> jsonavc1 = new JSONRefParser(file1).getAttributeValuePairs();
        AttributeValueCollection<AnnotatedAttributeValuePair> jsonavc2 = new JSONRefParser(file2).getAttributeValuePairs();

        AvcKrippendorffAlphaAgreement avcKrippendorffAlphaAgreement = new AvcKrippendorffAlphaAgreement();
        Map<Attribute, List<Double>> alphas = avcKrippendorffAlphaAgreement.evaluate(jsonavc1, jsonavc2);

        System.out.println("Group 10 Krippendorff alpha: " + alphas);

    }
}
