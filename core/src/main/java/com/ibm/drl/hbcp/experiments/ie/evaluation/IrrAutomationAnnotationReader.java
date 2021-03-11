package com.ibm.drl.hbcp.experiments.ie.evaluation;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Reads annotations from a folder of single-annotator un-reconciliated JSONs for "machine vs human" evaluation.
 *
 * @author mgleize
 */
public class IrrAutomationAnnotationReader {

    public static final File DEFAULT_FOLDER = new File("../data/jsons/IRR_Automation_Jsons/");

    public static Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>> getAnnotationsPerAnnotator(File folder) throws IOException {
        Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>> res = new TreeMap<>();
        for (File jsonFile : getAllJsons(folder)) {
            String annotator = getAnnotatorsName(jsonFile);
            Collection<AnnotatedAttributeValuePair> updatedAnnotations = new ArrayList<>();
            AttributeValueCollection<AnnotatedAttributeValuePair> oldAnnotations = res.get(annotator);
            if (oldAnnotations != null) updatedAnnotations.addAll(oldAnnotations);
            updatedAnnotations.addAll(new JSONRefParser(jsonFile).getAttributeValuePairs());
            res.put(annotator, new AttributeValueCollection<>(updatedAnnotations));
        }
        return res;
    }

    public static Map<String, Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>>> getAnnotationsPerGroupPerAnnotator(File folder) throws IOException {
        Map<String, Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>>> res = new TreeMap<>();
        for (File jsonFile : getAllJsons(folder)) {
            String group = getGroup(jsonFile);
            String annotator = getAnnotatorsName(jsonFile);
            res.putIfAbsent(group, new HashMap<>());
            Collection<AnnotatedAttributeValuePair> updatedAnnotations = new ArrayList<>();
            AttributeValueCollection<AnnotatedAttributeValuePair> oldAnnotations = res.get(group).get(annotator);
            if (oldAnnotations != null) updatedAnnotations.addAll(oldAnnotations);
            updatedAnnotations.addAll(new JSONRefParser(jsonFile).getAttributeValuePairs());
            res.get(group).put(annotator, new AttributeValueCollection<>(updatedAnnotations));
        }
        return res;
    }

    private static List<File> getAllJsons(File folder) {
        List<File> res = new ArrayList<>();
        for (File child : folder.listFiles()) {
            if (child.getName().endsWith(".json")) {
                res.add(child);
            } else if (child.isDirectory() && !child.getName().equals(".") && !child.getName().equals("..")) {
                res.addAll(getAllJsons(child));
            }
        }
        return res;
    }

    private static String getAnnotatorsName(File file) {
        String name = file.getName().replaceAll("\\.json", "");
        name = StringUtils.replace(name, " Not included paper 5", "");
        return name.substring(name.length() - 2);
    }

    private static String getGroup(File file) {
        Pattern groupRegex = Pattern.compile("Group[0-9]+");
        Matcher matcher = groupRegex.matcher(file.getName());
        if (matcher.find()) {
            String folder = file.getParentFile().getName();
            return folder + "_" + matcher.group();
        } else {
            return "N/A";
        }
    }

    public static void main(String[] args) throws IOException {
        Collection<AnnotatedAttributeValuePair> fullAnnotations = new ArrayList<>();
        for (File jsonFile : getAllJsons(DEFAULT_FOLDER)) {
            System.out.println("Parsing: " + jsonFile);
            JSONRefParser refParser = new JSONRefParser(jsonFile);
            fullAnnotations.addAll(refParser.getAttributeValuePairs());
        }
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = new AttributeValueCollection<>(fullAnnotations);
        Set<String> docnames = annotations.getDocNames();
        System.out.println("Doc count: " + docnames.size());
        AttributeValueCollection<AnnotatedAttributeValuePair> gold = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        Set<String> goldDocnames = gold.getDocNames();
        docnames.removeAll(goldDocnames);
        System.out.println("Docs not found in current docs: " + docnames);
        Set<Attribute> attributes = new HashSet<>(annotations.getAttributesById().values());
        System.out.println("Attribute count: " + attributes.size());
        Map<String, Map<String, AttributeValueCollection<AnnotatedAttributeValuePair>>> annotationsGrouped = getAnnotationsPerGroupPerAnnotator(DEFAULT_FOLDER);
        System.out.println(annotationsGrouped.keySet());
    }
}
