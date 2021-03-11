package com.ibm.drl.hbcp.util;

import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
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
                        return avp.withArm(new Arm(Arm.CONTROL.getId(), avp.getArm().getStandardName()));
                    } else {
                        return avp.withArm(new Arm(Arm.MAIN.getId(), avp.getArm().getStandardName()));
                    }
                })
                .collect(Collectors.toList());
        return new AttributeValueCollection<>(filtered);
    }

    private static Map<String, Arm> loadExistingAnnotations(File annotationFile) {
        try {
            ControlGroupArmNames previous = new ControlGroupArmNames(annotationFile);
            Map<String, Arm> res = new HashMap<>();
            for (Map.Entry<String, String> entry : previous.getDocnameToControlGroupArmName().entrySet()) {
                res.put(entry.getKey(), new Arm(entry.getValue()));
            }
            return res;
        } catch (IOException e) {
            return new HashMap<>();
        }
    }

    public static void annotate(String[] args) throws IOException {
        Scanner console = new Scanner(System.in);
        List<String> filesToParse = Lists.newArrayList(
                Props.loadProperties().getProperty("ref.json")
        );
        File annotationFile = new File("output/ControlGroupArmNames.txt");
        Map<String, Arm> controlArms = loadExistingAnnotations(annotationFile);
        System.out.println("Loaded " + controlArms.size() + " previous annotations.");
        for (String path : filesToParse) {
            System.out.println("Parsing " + path);
            JSONRefParser refParser = new JSONRefParser(new File(path));
            AttributeValueCollection<AnnotatedAttributeValuePair> avps = refParser.getAttributeValuePairs();
            double totalPapers = avps.getDocNames().size();
            int twoArmPapers = 0;
            for (String docname : avps.getDocNames()) {
                // check if the annotation was done for this one
                if (controlArms.containsKey(docname))
                    continue;
                // proceed with new annotation
                Map<Arm, Multiset<AnnotatedAttributeValuePair>> armifiedAvps = avps.getArmifiedPairsInDoc(docname);
                List<Arm> arms = new ArrayList<>(armifiedAvps.keySet());
                arms.remove(Arm.EMPTY);
                int armCount = arms.size();
                if (armCount == 2) {
                    twoArmPapers++;
                    System.out.println("=====TWO-ARM PAPER: " + docname + " ====");
                    int i = 0;
                    for (Arm arm : arms) {
                        System.out.println((i++) + ") " + arm.getStandardName() + " : " + arm.getAllNames());
                    }
                    int stopIndex = i;
                    System.out.println(stopIndex + ") Stop.");
                    boolean annotated = false;
                    while (!annotated) {
                        try {
                            System.out.println("Enter the number of the control group:");
                            String choice = console.nextLine();
                            int index = Integer.parseInt(choice);
                            if (index == stopIndex) {
                                System.out.println("Stopping early. Annotations will be written.");
                                writeAnnotations(controlArms, annotationFile);
                                return;
                            } else {
                                controlArms.put(docname, arms.get(index));
                                annotated = true;
                            }
                        } catch (Exception e) {

                        }
                    }
                }
            }
            System.out.println(twoArmPapers / totalPapers);
            console.close();
            System.out.println("Done.");
            writeAnnotations(controlArms, annotationFile);
        }
    }

    private static void writeAnnotations(Map<String, Arm> docnameToControlArmName, File output) throws IOException {
        output.getParentFile().mkdirs();
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(output))) {
            for (Map.Entry<String, Arm> entry : docnameToControlArmName.entrySet()) {
                bw.write(entry.getKey());
                bw.newLine();
                bw.write(entry.getValue().getStandardName());
                bw.newLine();
            }
        }
    }

    public static void run() throws IOException {
        ControlGroupArmNames controlGroupNames = new ControlGroupArmNames(new File("output/ControlGroupArmNames.txt"));
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = new JSONRefParser(Props.loadProperties()).getAttributeValuePairs();
        AttributeValueCollection<AnnotatedAttributeValuePair> restricted = controlGroupNames.restrictToTwoArmPapers(avps);
        for (ArmifiedAttributeValuePair avp : restricted.getAllPairs()) {
            if (avp.getAttribute().getType() == AttributeType.INTERVENTION) {
                System.out.println("==== BCT ====");
                System.out.println("Value: " + avp.getValue());
                System.out.println("Context: " + avp.getContext());
                System.out.println(avp.getArm());
            }
        }
        System.out.println(restricted.byDoc().size());
    }

    public static void main(String[] args) throws IOException {
        run();
    }
}
