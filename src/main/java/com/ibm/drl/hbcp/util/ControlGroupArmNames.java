package com.ibm.drl.hbcp.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import lombok.Getter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * A command line program that outputs the arm names for 2-arm papers, and allows you to select the control group arm,
 * and save these annotations to a txt file.
 * Also an API to read those annotations.
 *
 * (more convenient to use in an IDE terminal)
 *
 * @author marting
 */
public class ControlGroupArmNames {

    @Getter
    private final Map<String, String> docnameToControlGroupArmName;

    public ControlGroupArmNames(File annotationFile) throws IOException {
        docnameToControlGroupArmName = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new FileReader(annotationFile))) {
            String docname;
            while ((docname = br.readLine()) != null) {
                String controlGroupStandardName = br.readLine();
                docnameToControlGroupArmName.put(docname, controlGroupStandardName);
            }
        }
    }

    public AttributeValueCollection<AnnotatedAttributeValuePair> restrictToTwoArmPapers(AttributeValueCollection<AnnotatedAttributeValuePair> annotations) {
        Collection<AnnotatedAttributeValuePair> filtered = annotations.getAllPairs().stream()
                // remove papers with more than 2 non-empty arms
                .filter(avp -> docnameToControlGroupArmName.containsKey(avp.getDocName()))
                // replace arms with standard MAIN and CONTROL arms
                .map(avp -> {
                    if (avp.getArm().equals(Arm.EMPTY)) {
                        return avp;
                    } else if (docnameToControlGroupArmName.get(avp.getDocName()).equals(avp.getArm().getStandardName())) {
                        return avp.withArm(Arm.CONTROL);
                    } else {
                        return avp.withArm(Arm.MAIN);
                    }
                })
                .collect(Collectors.toList());
        return new AttributeValueCollection<>(filtered);
    }

    public static void annotate(String[] args) throws IOException {
        Scanner console = new Scanner(System.in);
        List<String> filesToParse = Lists.newArrayList(
                "data/jsons/Smoking_AllAnnotations_01Apr19.json"
        );
        Map<String, Arm> controlArms = new HashMap<>();
        String file = "data/Smoking_AllAnnotations_01Apr19.json_ControlGroupArmNames.txt";
        for (String path : filesToParse) {
            System.out.println("Parsing " + path);
            JSONRefParser refParser = new JSONRefParser(new File(path));
            AttributeValueCollection<AnnotatedAttributeValuePair> avps = refParser.getAttributeValuePairs();
            double totalPapers = avps.getDocNames().size();
            int twoArmPapers = 0;
            for (String docname : avps.getDocNames()) {
                Map<Arm, Multiset<AnnotatedAttributeValuePair>> armifiedAvps = avps.getArmifiedPairsInDoc(docname);
                List<Arm> arms = new ArrayList<>(armifiedAvps.keySet());
                arms.remove(Arm.EMPTY);
                int armCount = arms.size();
                if (armCount == 2) {
                    twoArmPapers++;
                    System.out.println("=====TWO-ARM PAPER=====");
                    int i = 0;
                    for (Arm arm : arms) {
                        System.out.println((i++) + ") " + arm.getStandardName() + " : " + arm.getAllNames());
                    }
                    boolean annotated = false;
                    while (!annotated) {
                        try {
                            System.out.println("Enter the number of the control group:");
                            String choice = console.nextLine();
                            int index = Integer.parseInt(choice);
                            controlArms.put(docname, arms.get(index));
                            annotated = true;
                        } catch (Exception e) {

                        }
                    }
                }
            }
            System.out.println(twoArmPapers / totalPapers);
            System.out.println("Done.");
            try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
                for (Map.Entry<String, Arm> entry : controlArms.entrySet()) {
                    bw.write(entry.getKey());
                    bw.newLine();
                    bw.write(entry.getValue().getStandardName());
                    bw.newLine();
                }
            }
        }
    }

    public static void run() throws IOException {
        ControlGroupArmNames controlGroupNames = new ControlGroupArmNames(new File("data/jsons/Smoking_AllAnnotations_01Apr19.json_ControlGroupArmNames.txt"));
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        AttributeValueCollection<AnnotatedAttributeValuePair> restricted = controlGroupNames.restrictToTwoArmPapers(avps);
        System.out.println(restricted.getAllPairs());
    }

    public static void main(String[] args) throws IOException {
        annotate(args);
    }
}
