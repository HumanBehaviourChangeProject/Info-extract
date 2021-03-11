package com.ibm.drl.hbcp.parser.sandbox;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class MultiArmBctStatistics {

    public static void main(String[] args) throws IOException {
        String jsonPath = "data/jsons/Smoking_AllAnnotations_01Apr19.json";
        int totalPapers = 0;
        int totalBCTCases = 0;
        int totalBCTInstances = 0;
        int papersWithMultiBCT = 0;
        int totalMultiBCTCases = 0;
        int multiBCTWithSameContext = 0;
        System.out.println("Parsing " + jsonPath);
        JSONRefParser refParser = new JSONRefParser(new File(jsonPath));
        Set<String> distinctContexts = new HashSet<>();
        for (String docname : refParser.getAttributeValuePairs().getDocNames()) {
            System.out.println("=========================");
            System.out.println("Doc: " + docname);
            Map<Attribute, Set<Context>> multiBctToContexts = new HashMap<>();
            Set<Attribute> bcts = new HashSet<>();
            for (AnnotatedAttributeValuePair avp : refParser.getAttributeValuePairs().byDoc().get(docname)) {
                // only checks BCTs
                if (avp.getAttribute().getType() == AttributeType.INTERVENTION) {
                    bcts.add(avp.getAttribute());
                    List<AnnotatedAttributeValuePair> dualBCTs = refParser.getAttributeValuePairs().byDoc().get(docname).stream()
                            // same attribute
                            .filter(otherAvp -> otherAvp.getAttribute().equals(avp.getAttribute()))
                            // other AVPs
                            .filter(otherAvp -> !otherAvp.equals(avp))
                            // not belonging to empty arm
                            .filter(otherAvp -> !otherAvp.getArm().equals(Arm.EMPTY))
                            .collect(Collectors.toList());
                    if (!dualBCTs.isEmpty()) {
                        System.out.println("BCT with multi-instances!");
                        System.out.println("\t\t" + avp.getAttribute().getName());
                        System.out.println("\t\tArm: " + avp.getArm());
                        System.out.println("\t" + avp.getContext());
                        System.out.println("\tHighlighted: " + avp.getHighlightedText());
                        multiBctToContexts.putIfAbsent(avp.getAttribute(), new HashSet<>());
                        multiBctToContexts.get(avp.getAttribute()).add(new Context(avp.getContext(), avp.getHighlightedText()));
                    }
                    totalBCTInstances++;
                    distinctContexts.add(avp.getContext());
                }
            }
            if (!multiBctToContexts.isEmpty()) {
                papersWithMultiBCT++;
                totalMultiBCTCases += multiBctToContexts.keySet().size();
                multiBCTWithSameContext += multiBctToContexts.values().stream().filter(contexts -> contexts.size() == 1).count();
            }
            totalBCTCases += bcts.size();
            totalPapers++;
        }
        System.out.println("=====================");
        System.out.println("Total papers: " + totalPapers);
        System.out.println("Papers with a 'multi-arm' BCT: " + papersWithMultiBCT + " (" + ((double)papersWithMultiBCT / totalPapers) + ")");
        System.out.println("Total BCT instances: " + totalBCTInstances);
        System.out.println("Total BCT cases: " + totalBCTCases);
        System.out.println("Total 'multi-arm' BCT cases: " + totalMultiBCTCases);
        System.out.println("Multi-arm BCT cases where all instances have the same context (aka: same annotation span): " + multiBCTWithSameContext + " (" + ((double)multiBCTWithSameContext / totalMultiBCTCases) + ")");
        System.out.println("Contexts: " + distinctContexts.size());
    }

    @Data
    public static class Context {
        private final String context;
        private final String highlightedText;
    }
}
