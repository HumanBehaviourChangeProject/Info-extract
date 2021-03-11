package com.ibm.drl.hbcp.experiments.tapas;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.pdf.Block;
import com.ibm.drl.hbcp.parser.pdf.TableValue;
import lombok.Value;

import java.util.*;

public class FlairTableInstancesGenerator {

    private final List<TapasInstanceBuilder> tapasInstances;

    public FlairTableInstancesGenerator() throws Exception {
        TapasTableInstancesGenerator tapasGenerator = new TapasTableInstancesGenerator();
        tapasInstances = tapasGenerator.getInstances();
    }

    public Map<String, Map<String, List<AnnotatedTable>>> getAnnotatedTablesSplits() throws Exception {
        Map<String, List<TapasInstanceBuilder>> trainDevTestSplit = QAEntities.split(tapasInstances);
        Map<String, Map<String, List<AnnotatedTable>>> res = new HashMap<>();
        for (Map.Entry<String, List<TapasInstanceBuilder>> instancesSplit : trainDevTestSplit.entrySet()) {
            res.put(instancesSplit.getKey(), getAnnotatedTablesPerDocName(instancesSplit.getValue()));
        }
        return res;
    }

    private Map<String, List<AnnotatedTable>> getAnnotatedTablesPerDocName(List<TapasInstanceBuilder> tapasInstances) {
        Map<String, List<AnnotatedTable>> res = new HashMap<>();
        for (Map.Entry<Block, List<TapasInstanceBuilder>> tableAnnotations : clusterInstancesByTable(tapasInstances).entrySet()) {
            String docName = tableAnnotations.getValue().get(0).getDocName();
            Set<CellWithAttribute> uniquePositives = new HashSet<>();
            for (TapasInstanceBuilder instance : tableAnnotations.getValue()) {
                // add arms
                for (TapasTableInstancesGenerator.Answer answer : instance.getArmAnswers().values()) {
                    uniquePositives.add(new CellWithAttribute(answer.getCell(), Attributes.get().getFromName("Arm name"), null));
                }
                // add followup
                if (instance.getLongestFollowUpCell().isPresent()) {
                    uniquePositives.add(new CellWithAttribute(instance.getLongestFollowUpCell().get().getCell(), Attributes.get().getFromName("Longest follow up"), null));
                }
                // add answers
                for (TapasTableInstancesGenerator.QuestionData question : instance.getQuestions()) {
                    for (TapasTableInstancesGenerator.AnnotatedCell cell : question.getAnnotatedCells()) {
                        uniquePositives.add(new CellWithAttribute(cell.getCell(), cell.getAnnotation().getAttribute(), cell.getAnnotation()));
                    }
                }
            }
            // create the table element
            AnnotatedTable table = new AnnotatedTable(tableAnnotations.getKey().getTable(), new ArrayList<>(uniquePositives));
            res.putIfAbsent(docName, new ArrayList<>());
            res.get(docName).add(table);
        }
        return res;
    }

    private Map<Block, List<TapasInstanceBuilder>> clusterInstancesByTable(List<TapasInstanceBuilder> instances) {
        Map<Block, List<TapasInstanceBuilder>> res = new HashMap<>();
        for (TapasInstanceBuilder instance : instances) {
            res.putIfAbsent(instance.getTableInPdf(), new ArrayList<>());
            res.get(instance.getTableInPdf()).add(instance);
        }
        return res;
    }

    @Value
    public static class AnnotatedTable {
        List<TableValue> cells;
        List<CellWithAttribute> positives;
    }

    @Value
    public static class CellWithAttribute {
        TableValue cell;
        Attribute attribute;
        // can be null if no manual table annotation was available and one had to be automatically constructed (arms, follow-ups)
        AnnotatedAttributeValuePair annotation;
    }

    public static void main(String[] args) throws Exception {
        FlairTableInstancesGenerator gen = new FlairTableInstancesGenerator();
        Map<String, Map<String, List<AnnotatedTable>>> splits = gen.getAnnotatedTablesSplits();
        // print the first doc
        List<AnnotatedTable> tables = splits.get("train").values().iterator().next();
        AnnotatedTable table = tables.get(0);
//        System.out.println("=== TABLE ======");
//        for (TableValue cell : table.getCells()) {
//            System.out.println(cell.toText());
//        }
//        System.out.println("=== POSITIVES =====");
        for (CellWithAttribute positive : table.getPositives()) {
            System.out.println(positive.getAttribute() + " -> " + positive.getAnnotation());
            if(positive.getAnnotation()!=null){
                System.out.println(positive.getAnnotation().getArm().getAllNames());
            }
            System.out.println(positive.getCell().toText());
            System.out.println(positive.getCell().getValue());
        }
    }
}
