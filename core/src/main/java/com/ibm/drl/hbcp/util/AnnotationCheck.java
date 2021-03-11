package com.ibm.drl.hbcp.util;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeNameNumberTriple;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Basic script for checking the quality of the reference annotation after JSON parsing.
 * 
 * @author charlesj
 *
 */
public class AnnotationCheck {

    private static Logger logger = LoggerFactory.getLogger(AnnotationCheck.class);
    private final JSONRefParser parser;
    private AttributeValueCollection<AnnotatedAttributeValuePair> attributeValuePairs;

    public AnnotationCheck(String propFilename) throws IOException {
        parser = new JSONRefParser(propFilename);
        attributeValuePairs = parser.getAttributeValuePairs();
    }

    /**
     * Check presence and quality of some of the annotation from JSON parsing.
     * Verbosity 0 shows only "errors" (missing annotation).  Increasing verbosity shows
     * warnings for probably less critical annotation problems. 
     * 
     * @param verbosity level of severity of problem (0 most severe)
     */
    public void check(int verbosity) {
        Map<Attribute, int[]> attributeCounts = new HashMap<>();
        int[] missingCnts = new int[7];
        int totalPairs = 0;
        for (AnnotatedAttributeValuePair aavp : attributeValuePairs) {
            totalPairs++;
            int[] counts = attributeCounts.computeIfAbsent(aavp.getAttribute(), k -> new int[1]);
            counts[0] += 1;
            String highlightedText = aavp.getHighlightedText();
            if (highlightedText.trim().equals("")) {
                logger.error("There should be no empty annotated values.");
                logger.error(aavp.toString());
                missingCnts[0]++;
            }
            // check for possibly bad annotation spans
            if (verbosity > 1 && highlightedText.matches("\\W.*")) {
                logger.warn("Annotatation starts with non-word character: " + highlightedText);
                logger.warn(aavp.toString());
            }
            if (verbosity > 1 && highlightedText.matches(".*[^\\w%)]")) {
                logger.warn("Annotatation ends with non-word character: " + highlightedText);
                logger.warn(aavp.toString());
            }
            // check attribute vales that could use hyphenization correction
            if (verbosity > 1 && highlightedText.matches(".*-\\s.*")) {
                logger.warn("Annotatation contains hyphenation: " + highlightedText);
                logger.warn(aavp.toString());
                missingCnts[6]++;
            }
            if (verbosity > 0 && aavp.getContext().trim().equals("")) {
                logger.warn("Context is empty.");
                logger.warn(aavp.toString());
                missingCnts[1]++;
            }
            if (verbosity > 0 && aavp.getDocName().trim().equals("")) {
                logger.warn("Missing document name.");
                logger.warn(aavp.toString());
                missingCnts[2]++;
            }
            if (verbosity > 0 && aavp.getSprintNo().trim().equals("")) {
                logger.warn("Missing sprint number.");
                logger.warn(aavp.toString());
                missingCnts[3]++;
            }
            if (verbosity > 0 && (aavp.getAnnotationPage() < 1 || aavp.getAnnotationPage() > 50)) {
                logger.warn("Page number outside of expected range");
                logger.warn(aavp.toString());
                missingCnts[4]++;
            }
//            if (aavp.getArm().trim().equals("")) {
//                logger.warn("Missing arm annotation.");
//            }
            if (verbosity > 0 && (aavp.getAttribute() == null || aavp.getAttribute().getId().equals("") || aavp.getAttribute().getName().equals(""))) {
                logger.error("There was a problem associating annotation to an attribute.");
                logger.warn(aavp.toString());
                missingCnts[5]++;
                Attribute attribute = aavp.getAttribute();
                if (attribute == null) {
                    logger.error("No matching attribute was found (i.e., attributre is null).");
                } else {
                    if (attribute.getId().equals("")) {
                        logger.error("Attribute is missing the id.");
                    }
                    if (attribute.getName().equals("")) {
                        logger.error("Attribute is missing the name.");
                    }
                }
            }
        }
        // print counts for missing values
        logger.info("Missing values: " + missingCnts[0]);
        logger.info("Missing contexts: " + missingCnts[1]);
        logger.info("Missing docs: " + missingCnts[2]);
        logger.info("Missing sprints: " + missingCnts[3]);
        logger.info("Missing pages: " + missingCnts[4]);
        logger.info("Missing attributes: " + missingCnts[5]);
        logger.info("Hyphenation: " + missingCnts[6]);
        logger.info("Total annotations (aavp's): " + totalPairs);
        // write attribute counts
        dumpAttributeCounts(attributeCounts);
    }

    private void dumpAttributeCounts(Map<Attribute, int[]> attributeCounts) {
        String tmpdir = System.getProperty("java.io.tmpdir");
        logger.info("Dumping attribute counts to " + tmpdir + "attributeCounts.tsv");
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(tmpdir + "attributeCounts.tsv"))) {
            for (Entry<Attribute, int[]> entry : attributeCounts.entrySet()) {
                Attribute attribute = entry.getKey();
                bw.write(attribute.getType().getName() + '\t' + attribute.getId() + '\t' + attribute.getName() + '\t' + entry.getValue()[0] + '\n');
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private Map<String, Map<String, Long>> collectAnnotation(List<String> attNames) {
        List<Attribute> atts = parser.getAttributes().stream()
                .filter(e -> attNames.contains(e.getName()))
                .collect(Collectors.toList());
        Map<String, Map<String, Long>> entityAnnotationCounts = new HashMap<>(attNames.size());
        for (Attribute attribute : atts) {
            Multiset<AnnotatedAttributeValuePair> pairsOfId = attributeValuePairs.getPairsOfId(attribute.getId());
            if (pairsOfId == null) {
                System.err.println("Attribute " + attribute.getName() + " has no annotation.  Choose another attribute?");
                continue;
            }
            Map<String, Long> avpCounts = pairsOfId.stream().map(AttributeValuePair::getValue)
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            entityAnnotationCounts.put(attribute.getName(), avpCounts);
        }
        return entityAnnotationCounts;
    }

    private Map<String, Map<String, Long>> collectComplexAnnotation(List<String> attNames) {
        List<Attribute> atts = parser.getAttributes().stream()
                .filter(e -> attNames.contains(e.getName()))
                .collect(Collectors.toList());
        Map<String, Map<String, Long>> entityAnnotationCounts = new HashMap<>(attNames.size());
        for (Attribute attribute : atts) {
            Multiset<AnnotatedAttributeValuePair> pairsOfId = attributeValuePairs.getPairsOfId(attribute.getId());
            if (pairsOfId == null) {
                System.err.println("Attribute " + attribute.getName() + " has no annotation.  Choose another attribute?");
                continue;
            }
            Map<String, Long> avpCounts = pairsOfId.stream().map(e -> ((AnnotatedAttributeNameNumberTriple)e).getValueName())
                    .collect(Collectors.groupingBy(Function.identity(), Collectors.counting()));
            entityAnnotationCounts.put(attribute.getName(), avpCounts);
        }
        return entityAnnotationCounts;
    }

    private void summarizeAnnotation() {
        for (Attribute attribute : parser.getAttributes()) {
            Multiset<AnnotatedAttributeValuePair> pairsOfId = attributeValuePairs.getPairsOfId(attribute.getId());
            if (pairsOfId == null) {
                System.out.println(attribute.getName() + "\t" + attribute.getId() + "\t" + attribute.getType() +
                        "\tMissing set");
            } else {
                System.out.println(attribute.getName() + "\t" + attribute.getId() + "\t" + attribute.getType() + "\t" +
                        pairsOfId.size());
            }
        }

    }

    private void countDocsAndAnnotations() {
        System.out.println("Number of documents: " + attributeValuePairs.getDocNames().size());
        final long numDocsWannotation = attributeValuePairs.byDoc().values().stream().filter(e -> !e.isEmpty()).count();
        System.out.println("Number of docs. with annotation = " + numDocsWannotation);
        System.out.println("Number of annotations: " + attributeValuePairs.getAllPairs().size());
    }

    private static void writeAnnotationMapToExcel(Map<String, Map<String, Long>> entityAnnotationCount) throws IOException {
        Workbook workbook = new XSSFWorkbook();

        for (Entry<String, Map<String, Long>> entityEntry : entityAnnotationCount.entrySet()) {

            Sheet sheet = workbook.createSheet(entityEntry.getKey());
            // header
            Row headerRow = sheet.createRow(0);
            Cell cell = headerRow.createCell(0);
            cell.setCellValue("text");
            cell = headerRow.createCell(1);
            cell.setCellValue("count");

            int rowNum = 1;

            for(Entry<String, Long> annotationCount : entityEntry.getValue().entrySet()) {
                Row row = sheet.createRow(rowNum++);
                row.createCell(0).setCellValue(annotationCount.getKey());
                row.createCell(1).setCellValue(annotationCount.getValue());
            }
        }

        FileOutputStream fileOut = new FileOutputStream("/tmp/annotationCounts.xlsx");
        workbook.write(fileOut);
        fileOut.close();
        workbook.close();
    }

    public static void main(String[] args) throws IOException {
        AnnotationCheck annotationCheck = new AnnotationCheck(Props.getDefaultPropFilename());
        //        annotationCheck.check(4);
//        annotationCheck.countDocsAndAnnotations();
//        annotationCheck.summarizeAnnotation();
        Map<String, Map<String, Long>> stringMapMap = annotationCheck.collectAnnotation(Arrays.asList("11.1 Pharmacological support "));
//        Map<String, Map<String, Long>> stringMapMap = annotationCheck.collectComplexAnnotation(Arrays.asList("Individual reasons for attrition"));
        writeAnnotationMapToExcel(stringMapMap);
    }

}
