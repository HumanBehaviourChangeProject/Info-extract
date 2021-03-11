package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.experiments.flair.Evaluation_NameAsCategory_NewFlairVersion;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;
import com.ibm.drl.hbcp.util.Props;
import com.opencsv.CSVWriter;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class InterAnnotatorAgreementEvaluation {

    protected final Metric metric;
    private final Map<String, AttributeValueCollection<? extends ArmifiedAttributeValuePair>> results = new TreeMap<>();
    private final Map<Attribute, Long> attributeAnnotationCounts;
    private final Set<Attribute> attributesWithMinimumAnnotationAmount;

    private static final int MIN_ANNOTATIONS_PER_ATTRIBUTE = 10;

    public InterAnnotatorAgreementEvaluation(Metric metric) {
        this.metric = metric;
        Set<Attribute> attributesWithMinimumAnnotationAmount1;
        Map<Attribute, Long> attributeAnnotationCounts1;
        try {
            attributesWithMinimumAnnotationAmount1 = getAttributesWithMoreThanNAnnotations(MIN_ANNOTATIONS_PER_ATTRIBUTE);
            attributeAnnotationCounts1 = getAttributesAnnotationCount();
        } catch (IOException e) {
            attributesWithMinimumAnnotationAmount1 = new HashSet<>();
            attributeAnnotationCounts1 = new HashMap<>();
        }
        attributesWithMinimumAnnotationAmount = attributesWithMinimumAnnotationAmount1;
        attributeAnnotationCounts = attributeAnnotationCounts1;
    }

    public void addResults(String name, AttributeValueCollection<? extends ArmifiedAttributeValuePair> results) {
        this.results.put(name, results);
    }

    public void writeAverageMetricPerAnnotatorPerAttribute(File outputCsv, String[] metricHeaders, int[] metricIndices) throws IOException {
        // compute eval
        Map<String, Map<Attribute, List<Double>>> eval = computeAverageMetricPerAnnotatorPerAttribute();
        // write csv
        try (CSVWriter w = new CSVWriter(new FileWriter(outputCsv))) {
            String[] headers = new String[2 + metricHeaders.length];
            headers[0] = "evaluated";
            headers[1] = "attribute";
            System.arraycopy(metricHeaders, 0, headers, 2, metricHeaders.length);
            w.writeNext(headers);
            for (String annotator : eval.keySet()) {
                for (Attribute attribute : eval.get(annotator).keySet()) {
                    List<Double> results = eval.get(annotator).get(attribute);
                    String[] row = new String[2 + metricIndices.length];
                    row[0] = annotator;
                    row[1] = attribute.getName();
                    for (int i = 0; i < metricIndices.length; i++) {
                        row[2 + i] = String.valueOf(results.get(metricIndices[i]));
                    }
                    w.writeNext(row);
                }
            }
        }
    }

    public void writeAverageSingleMetricPerAttribute(File outputCsv, int metricIndex) throws IOException {
        // compute eval
        Map<String, Map<Attribute, List<Double>>> eval = computeAverageMetricPerAnnotatorPerAttribute();
        Map<Attribute, Map<String, List<Double>>> transposedEval = transposeKeysInMap(eval);
        // reduce individual human annotator agreement to just min, max, and avg agreement
        transposedEval = replaceHumanAnnotatorsWithMaxMinAvg(transposedEval);
        // write csv
        try (CSVWriter w = new CSVWriter(new FileWriter(outputCsv))) {
            List<String> header = new ArrayList<>();
            header.add("attribute");
            header.add("#(annotations)");
            header.addAll(transposedEval.values().stream().flatMap(m -> m.keySet().stream()).distinct().collect(Collectors.toList()));
            String[] rowArray = new String[header.size()];
            w.writeNext(header.toArray(rowArray));
            for (Attribute attribute : transposedEval.keySet()) {
                List<String> row = new ArrayList<>();
                row.add(attribute.getName());
                row.add(String.valueOf(attributeAnnotationCounts.get(attribute)));
                for (int i = 2; i < header.size(); i++) {
                    String annotator = header.get(i);
                    List<Double> metrics = transposedEval.get(attribute).get(annotator);
                    if (metrics != null) {
                        row.add(String.valueOf(metrics.get(metricIndex)));
                    } else {
                        row.add("");
                    }
                }
                w.writeNext(row.toArray(rowArray));
            }
        }
    }


    private Map<String, Map<Attribute, List<Double>>> computeAverageMetricPerAnnotatorPerAttribute() throws IOException {
        // load annotations for each annotator
        Map<String, Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>>> annotations = IrrAutomationAnnotationReader.getAnnotationsPerGroupPerAnnotator(IrrAutomationAnnotationReader.DEFAULT_FOLDER);
        // calculate measures for each annotator within only the groups they annotated
        Map<String, Map<Attribute, List<List<Double>>>> evaluationMetricsPerAnnotator = new TreeMap<>();
        for (String group : annotations.keySet()) {
            Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>> annotationsForGroup = annotations.get(group);
            for (String annotator : annotationsForGroup.keySet()) {
                evaluationMetricsPerAnnotator.putIfAbsent(annotator, new TreeMap<>());
                Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>> others = new TreeMap<>(annotationsForGroup);
                others.remove(annotator);
                for (String otherAnnotator : others.keySet()) {
                    Map<Attribute, List<Double>> agreement = metric.evaluate(annotationsForGroup.get(annotator), others.get(otherAnnotator));
                    // combine all results
                    for (Attribute attribute : agreement.keySet()) {
                        evaluationMetricsPerAnnotator.get(annotator).putIfAbsent(attribute, new ArrayList<>());
                        evaluationMetricsPerAnnotator.get(annotator).get(attribute).add(agreement.get(attribute));
                    }
                }
            }
        }
        // calculate measures for the system(s), with all groups
        for (Map.Entry<String, AttributeValueCollection<? extends ArmifiedAttributeValuePair>> result : results.entrySet()) {
            // List<List<List<Double>>>> because we have potentially several (List) vectors (List<Double>) per group of annotations,
            // and then several (List) groups of annotations where the same attribute is annotated
            Map<Attribute, List<List<List<Double>>>> agreementsForSystem = new TreeMap<>();
            for (String group : annotations.keySet()) {
                Map<Attribute, List<List<Double>>> agreementInGroup = new TreeMap<>();
                Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>> annotationsForGroup = annotations.get(group);
                // normally several annotators (typically 2)
                for (String annotator : annotationsForGroup.keySet()) {
                    Map<Attribute, List<Double>> agreement = metric.evaluate(result.getValue(), annotationsForGroup.get(annotator));
                    // add the agreement results of this annotator
                    agreementInGroup = plusMap(agreementInGroup, agreement);
                }
                // add the agreement results of this group
                agreementsForSystem = plusMap(agreementsForSystem, agreementInGroup);
            }
            // add the worst, best, and average case scenario for this system agreeing with annotators
            Map<String, Map<Attribute, List<List<Double>>>> worstBestAverage = getWorstBestAndAverageSystemAgreement(agreementsForSystem, result.getKey());
            for (String subSystem : worstBestAverage.keySet()) {
                evaluationMetricsPerAnnotator.putIfAbsent(subSystem, worstBestAverage.get(subSystem));
            }
        }
        // average on each attribute
        Map<String, Map<Attribute, List<Double>>> evaluationMetricsPerAnnotatorAveraged = new TreeMap<>();
        for (Map.Entry<String, Map<Attribute, List<List<Double>>>> metricForAnnotator : evaluationMetricsPerAnnotator.entrySet()) {
            Map<Attribute, List<Double>> averages = new TreeMap<>();
            for (Map.Entry<Attribute, List<List<Double>>> allMetrics : metricForAnnotator.getValue().entrySet()) {
                // filter attributes by amount
                if (attributesWithMinimumAnnotationAmount.contains(allMetrics.getKey())) {
                    averages.put(allMetrics.getKey(), getAggregateMetric(allMetrics.getValue()));
                }
            }
            evaluationMetricsPerAnnotatorAveraged.put(metricForAnnotator.getKey(), averages);
        }
        return evaluationMetricsPerAnnotatorAveraged;
    }

    protected List<Double> getAggregateMetric(List<List<Double>> metricVectors) {
        return getAverages(metricVectors);
    }

    private Map<String, Map<Attribute, List<List<Double>>>> getWorstBestAndAverageSystemAgreement(
            Map<Attribute, List<List<List<Double>>>> agreementsForSystem, String systemName) {
        Map<String, Map<Attribute, List<List<Double>>>> res = new TreeMap<>();
        String worstName = systemName + "_min";
        String bestName = systemName + "_max";
        String avgName = systemName + "_avg";
        for (Map.Entry<Attribute, List<List<List<Double>>>> agreementForAttribute : agreementsForSystem.entrySet()) {
            for (List<List<Double>> agreementForGroup : agreementForAttribute.getValue()) { // multiple annotators here
                Attribute attribute = agreementForAttribute.getKey();
                // sort the per-annotator vectors by agreement
                agreementForGroup.sort(metric.getVectorComparator());
                // worst
                res.putIfAbsent(worstName, new TreeMap<>());
                res.get(worstName).putIfAbsent(attribute, new ArrayList<>());
                res.get(worstName).get(attribute).add(agreementForGroup.get(0));
                // best
                res.putIfAbsent(bestName, new TreeMap<>());
                res.get(bestName).putIfAbsent(attribute, new ArrayList<>());
                res.get(bestName).get(attribute).add(agreementForGroup.get(agreementForGroup.size() - 1));
                // avg
                res.putIfAbsent(avgName, new TreeMap<>());
                res.get(avgName).putIfAbsent(attribute, new ArrayList<>());
                res.get(avgName).get(attribute).add(getAggregateMetric(agreementForGroup));
            }
        }
        return res;
    }

    private Map<Attribute, Map<String, List<Double>>> replaceHumanAnnotatorsWithMaxMinAvg(Map<Attribute, Map<String, List<Double>>> agreements) {
        Map<Attribute, Map<String, List<Double>>> res = new TreeMap<>();
        for (Attribute attribute : agreements.keySet()) {
            List<Map.Entry<String, List<Double>>> humanAnnotatorSortedAgreements = agreements.get(attribute).entrySet().stream()
                    .filter(nameAndWhatever -> results.keySet().stream().noneMatch(systemName -> nameAndWhatever.getKey().startsWith(systemName + "_")))
                    .sorted(Comparator.comparing(e -> e.getValue(), metric.getVectorComparator()))
                    .collect(Collectors.toList());
            List<Map.Entry<String, List<Double>>> systemAgreements = agreements.get(attribute).entrySet().stream()
                    .filter(nameAndWhatever -> results.keySet().stream().anyMatch(systemName -> nameAndWhatever.getKey().startsWith(systemName + "_")))
                    .collect(Collectors.toList());
            // add the (transformed) human agreements
            Map<String, List<Double>> agreementsForAttribute = new TreeMap<>();
            if (!humanAnnotatorSortedAgreements.isEmpty()) {
                agreementsForAttribute.put("Human_min", humanAnnotatorSortedAgreements.get(0).getValue());
                agreementsForAttribute.put("Human_max", humanAnnotatorSortedAgreements.get(humanAnnotatorSortedAgreements.size() - 1).getValue());
                agreementsForAttribute.put("Human_avg", getAggregateMetric(humanAnnotatorSortedAgreements.stream().map(e -> e.getValue()).collect(Collectors.toList())));
            }
            // add the system agreements
            for (Map.Entry<String, List<Double>> systemAgreement : systemAgreements) {
                agreementsForAttribute.put(systemAgreement.getKey(), systemAgreement.getValue());
            }
            res.put(attribute, agreementsForAttribute);
        }
        return res;
    }

    private Map<Attribute, Long> getAttributesAnnotationCount() throws IOException {
        AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        Map<Attribute, Long> res = new HashMap<>();
        for (Attribute attribute : Attributes.get().getAttributeSet()) {
            res.put(attribute, annotations.stream().filter(avp -> avp.getAttribute().getName().equals(attribute.getName())).count());
        }
        return res;
    }

    private Set<Attribute> getAttributesWithMoreThanNAnnotations(int n) throws IOException {
        AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        return Attributes.get().getAttributeSet().stream()
                .filter(a -> annotations.stream().filter(avp -> avp.getAttribute().getName().equals(a.getName())).count() >= MIN_ANNOTATIONS_PER_ATTRIBUTE)
                .collect(Collectors.toSet());
    }

    protected static List<Double> getAverages(List<List<Double>> vectors) {
        double[] totals = new double[vectors.get(0).size()];
        for (List<Double> vector : vectors) {
            for (int i = 0; i < vector.size(); i++) {
                totals[i] += vector.get(i);
            }
        }
        List<Double> res = new ArrayList<>();
        for (int i = 0; i < totals.length; i++) {
            res.add(totals[i] / vectors.size());
        }
        return res;
    }

    private static <X, Y, Z> Map<Y, Map<X, Z>> transposeKeysInMap(Map<X, Map<Y, Z>> map) {
        Map<Y, Map<X, Z>> res = new TreeMap<>();
        for (X key1 : map.keySet()) {
            for (Y key2 : map.get(key1).keySet()) {
                res.putIfAbsent(key2, new TreeMap<>());
                res.get(key2).put(key1, map.get(key1).get(key2));
            }
        }
        return res;
    }

    private static <K, V> Map<K, List<V>> plusMap(Map<K, List<V>> mapOfLists, Map<K, V> mapToAdd) {
        Map<K, List<V>> res = new TreeMap<>(mapOfLists);
        // combine all results
        for (K key : mapToAdd.keySet()) {
            res.putIfAbsent(key, new ArrayList<>());
            res.get(key).add(mapToAdd.get(key));
        }
        return res;
    }

    public static AttributeValueCollection<? extends ArmifiedAttributeValuePair> loadExtractedResults() throws IOException {
        Properties props = Props.loadProperties();
        return CrossValidationSplitter.obtainAttribVals(
                props.getProperty("ref.json"),
                false,
                false,
                props
        );
    }

    public static AttributeValueCollection<? extends ArmifiedAttributeValuePair> loadFlairResults(File flairJsonFolder) throws Exception {
        Evaluation_NameAsCategory_NewFlairVersion eval = new Evaluation_NameAsCategory_NewFlairVersion();
        List<ArmifiedAttributeValuePair> res = new ArrayList<>();
        for (File jsonFile : flairJsonFolder.listFiles()) {
            if (jsonFile.getName().endsWith(".pdf.txt.json")) {
                String docName = jsonFile.getName().replaceAll("\\.txt\\.json", "");
                Map<String, List<String>> attributeNameToValues = eval.extractPrediction(jsonFile.getAbsolutePath());
                for (Map.Entry<String, List<String>> attributeNameAndValues : attributeNameToValues.entrySet()) {
                    for (String value : attributeNameAndValues.getValue()) {
                        Attribute attribute = Attributes.get().getFromName(attributeNameAndValues.getKey());
                        if (attribute != null) { // null happens for name-value attributes, because the flair JSON adds "-name"/"-value" suffixes to the normal name
                            ArmifiedAttributeValuePair avp = new ArmifiedAttributeValuePair(
                                    attribute,
                                    value,
                                    docName,
                                    Arm.EMPTY,
                                    value // value is its own context, but Evaluation_NameAsCategory_NewFlairVersion.extractPrediction can be modified to get the whole sentence
                            );
                            res.add(avp);
                        }
                    }
                }
            }
        }
        return new AttributeValueCollection<>(res);
    }
}
