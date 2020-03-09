package com.ibm.drl.hbcp.util;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedWriter;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;

/**
 * Basic script for checking the quality of the reference annotation after JSON parsing.
 * 
 * @author charlesj
 *
 */
public class AnnotationCheck {

    static Logger logger = LoggerFactory.getLogger(AnnotationCheck.class);
    private AttributeValueCollection<AnnotatedAttributeValuePair> attributeValuePairs;

    public AnnotationCheck(String propFilename) {
        try {
            JSONRefParser parser = new JSONRefParser(propFilename);
            parser.buildAll();
            attributeValuePairs = parser.getAttributeValuePairs();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static void printUsage() {
        System.out.println("USAGE: AnnotationCheck (JSON annotation file)");
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
            int[] counts = attributeCounts.get(aavp.getAttribute());
            if (counts == null) {
                counts = new int[1];
                attributeCounts.put(aavp.getAttribute(), counts);
            }
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
            if (verbosity > 1 && highlightedText.matches(".*[^\\w%\\)]")) {
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

    public static void main(String[] args) {
        String propFilename;
        if (args.length < 1) {
            propFilename = "init.properties";
            System.err.println("Default to " + propFilename);
        } else {
            propFilename = args[0];
        }
        AnnotationCheck annotationCheck = new AnnotationCheck(propFilename);
        annotationCheck.check(4);
    }

}
