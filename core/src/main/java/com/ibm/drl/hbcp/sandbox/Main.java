package com.ibm.drl.hbcp.sandbox;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

public class Main {

    public static void main(String[] args) throws IOException {
        Map<Set<String>, Integer> freqs = new HashMap<>();
        JSONRefParser ref = new JSONRefParser(Props.loadProperties());
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = ref.getAttributeValuePairs();
        annotations = annotations.distributeEmptyArm();
        for (String docName : annotations.getDocNames()) {
            for (Collection<AnnotatedAttributeValuePair> avpsInArm : annotations.getArmifiedPairsInDoc(docName).values()) {
                Set<String> intervention = avpsInArm.stream()
                        .filter(avp -> avp.getAttribute().getType() == AttributeType.INTERVENTION)
                        .map(avp -> avp.getAttribute().getName())
                        .collect(Collectors.toSet());
                freqs.put(intervention, freqs.getOrDefault(intervention, 0) + 1);
            }
        }
        // sort freqs
        List<Map.Entry<Set<String>, Integer>> list = new ArrayList<>(freqs.entrySet());
        list.sort(Comparator.comparingInt(e -> -e.getValue()));
        for (Map.Entry<Set<String>, Integer> e : list) {
            System.out.println(e);
        }
    }
}
