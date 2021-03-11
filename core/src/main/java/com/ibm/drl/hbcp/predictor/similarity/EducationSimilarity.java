package com.ibm.drl.hbcp.predictor.similarity;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.tuple.Pair;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class EducationSimilarity {

    // TODO maybe switch this to some enum
    private static final List<String> EDU_ATTRIBUTE_NAMES = Arrays.asList(
            "Aggregate completed education",
            "Aggregate education",
            "Aggregate highest level of formal educational qualification achieved",
            "Aggregate number of years in education completed",
            "Mean number of years in education completed",
            "Median number of years in education completed",
            "Proportion achieved post secondary vocational education",
            "Proportion achieved primary education",
            "Proportion achieved secondary education",
            "Proportion achieved university or college education",
            "Proportion higher education"
    );
    private final List<Attribute> eduAttributes;

    public EducationSimilarity(Attributes attributes) {
        final List<Attribute> unsortedEduAtts = attributes.stream().filter(e -> EDU_ATTRIBUTE_NAMES.contains(e.getName())).collect(Collectors.toList());
        Collections.sort(unsortedEduAtts, Comparator.comparing(Attribute::getName));
        eduAttributes = unsortedEduAtts;
    }

    private double similarity(Multiset<AnnotatedAttributeValuePair> doc1Avps, Multiset<AnnotatedAttributeValuePair> doc2Avps) {
        final Pair<double[], double[]> vectorEducationValues = getVectorEducationValues(doc1Avps, doc2Avps);
        return cosineSimilarity(vectorEducationValues.getLeft(), vectorEducationValues.getRight());
    }

    private double compare(Multiset<AnnotatedAttributeValuePair> doc1Avps, Multiset<AnnotatedAttributeValuePair> doc2Avps) {
        final Pair<double[], double[]> vectorEducationValues = getVectorEducationValues(doc1Avps, doc2Avps);
        // TODO this might be oversimplfied.  Should we weight some attributes highter?
        return Arrays.stream(vectorEducationValues.getLeft()).sum() - Arrays.stream(vectorEducationValues.getRight()).sum();
    }

    private Pair<double[], double[]> getVectorEducationValues(Multiset<AnnotatedAttributeValuePair> doc1Avps, Multiset<AnnotatedAttributeValuePair> doc2Avps) {
        // we create a vector for each document based on the attributes EDU_ATTRIBUTE_NAMES (ordered alphabetically)
        // what's the difference for a value in doc1 and not in doc2?  doc1 - 0?  ignore both?  doc1 - average value?
//        List<Double> doc1Attribs = new ArrayList<>();
//        List<Double> doc2Attribs = new ArrayList<>();
        double[] doc1Attribs = new double[eduAttributes.size()];
        double[] doc2Attribs = new double[eduAttributes.size()];
        for (int i = 0; i < eduAttributes.size(); i++) {
            if (i < 4) { // 'aggregate' value
                // try to decipher aggregate values
            } else {
                // TODO can I assume the data has been cleaned?
                int finalI = i;
                final Optional<AnnotatedAttributeValuePair> att1 = doc1Avps.stream().filter(e -> e.getAttribute().equals(eduAttributes.get(finalI))).findFirst();
                final Optional<AnnotatedAttributeValuePair> att2 = doc2Avps.stream().filter(e -> e.getAttribute().equals(eduAttributes.get(finalI))).findFirst();
                if (att1.isPresent() && att2.isPresent()) {
                    try {
                        double att1val = Double.valueOf(att1.get().getValue());
                        double att2val = Double.valueOf(att2.get().getValue());
                        // TODO is using array, use separate try/catch blocks
//                        doc1Attribs.add(att1val);
//                        doc2Attribs.add(att2val);
                        doc1Attribs[i] = att1val;
                        doc2Attribs[i] = att2val;
                    } catch (NumberFormatException e) {
                        // just don't add
                    }
                }
            }
        }
//        if (doc1Attribs.isEmpty()) return 1;  // TODO this would give similarity to things that match
        return Pair.of(doc1Attribs, doc2Attribs);
    }

    private double cosineSimilarity(double[] vecA, double[] vecB) {
        double numerator = 0.0;
        double aggA = 0.0;
        double aggB = 0.0;
        for (int i = 0; i < vecA.length; i++) {
            numerator += vecA[i] * vecB[i];
            aggA += vecA[i] * vecA[i];
            aggB += vecB[i] * vecB[i];
        }
        final double denominator = Math.sqrt(aggA) * Math.sqrt(aggB);
        return numerator/denominator;
    }

    public static void main(String[] args) throws IOException {
        final Properties props = Props.loadProperties();
        JSONRefParser parser = new JSONRefParser(props);
        parser.buildAll();
        AttributeValueCollection<AnnotatedAttributeValuePair> allAvps = parser.getAttributeValuePairs();
        // first distribute the empty arm (give to each real arm its values)
//        allAvps = allAvps.distributeEmptyArm();
        // apply the cleaners
        if (!allAvps.isEmpty() && allAvps.stream().findFirst().get() instanceof AnnotatedAttributeValuePair) {
            Cleaners cleaners = new Cleaners(props);
            // this warning is checked just before, should be okay
            allAvps = cleaners.clean((AttributeValueCollection<AnnotatedAttributeValuePair>)allAvps);
        }

        final Map<String, Multiset<AnnotatedAttributeValuePair>> docAvpMap = allAvps.byDoc();
        final Multiset<AnnotatedAttributeValuePair> doc1 = docAvpMap.get("Alessi 2014.pdf");
        final Multiset<AnnotatedAttributeValuePair> doc2 = docAvpMap.get("Brown 2001.pdf");
        // TODO I guess this should be arms?
        final EducationSimilarity educationSimilarity = new EducationSimilarity(parser.getAttributes());
        System.out.println("Similarity: " + educationSimilarity.similarity(doc1, doc2));
        System.out.println("Compare (negative means doc1 is 'less than' doc2, etc.): " + educationSimilarity.compare(doc1, doc2));
    }

}
