package com.ibm.drl.hbcp.sandbox;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.util.Props;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;

public class PhysicalActivityDataLoader {

    public static final List<File> PHYSICAL_ACTIVITY_JSONS = Lists.newArrayList(
            new File("data/jsons/physical_activity/PhysicalActivity Sprint1ArmsAnd Prioritised47Papers.json"),
            new File("data/jsons/physical_activity/Batch2PhysicalActivityPrioritisedCodeset.json")
    );

    public static List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getAnnotations() throws IOException {
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> res = new ArrayList<>();
        for (File annotationFile : PHYSICAL_ACTIVITY_JSONS) {
            res.addAll(getAnnotations(annotationFile));
        }
        return res;
    }

    public static List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getAnnotations(File annotationFile) throws IOException {
        // load default props but override the location of PDF parsing outputs to target the PA ones
        Properties props = Props.loadProperties();
        props.setProperty("coll.extracted.reparse", "data/pdfs_PA_extracted/");
        // load the cleaners with these props
        Cleaners cleaners = new Cleaners(props);
        // load all the annotations (spread over multiple JSON files potentially)
        AttributeValueCollection<AnnotatedAttributeValuePair> avps = getAvps(annotationFile);
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> res = new ArrayList<>();
        for (String docname : avps.byDoc().keySet()) {
            Collection<? extends AnnotatedAttributeValuePair> annotations = avps.byDoc().get(docname);
            // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
            if (annotations == null) {
                System.err.println(docname + " NOT FOUND in the JSON " + annotationFile.getName());
                continue;
            }
            AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(new AttributeValueCollection<>(annotations));
            res.add(Pair.of(docname, new ArrayList<>(cleaned)));
        }
        return res;
    }

    private static AttributeValueCollection<AnnotatedAttributeValuePair> getAvps(File... annotationFiles) throws IOException {
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        for (File annotationFile : annotationFiles) {
            JSONRefParser parser = new JSONRefParser(annotationFile);
            res.addAll(parser.getAttributeValuePairs());
        }
        return new AttributeValueCollection<>(res);
    }

    private static void sanityChecks(List<Pair<String, Collection<AnnotatedAttributeValuePair>>> annotations) {
        // count the values with empty contexts (meaning table values that we haven't managed to find in the PDF)
        Counts emptyContextCounts = count(annotations, avp -> avp.getContext().isEmpty());
        System.out.println("Empty contexts: " + emptyContextCounts.getValues() + " ("
            + emptyContextCounts.getDocs() + " docs)");
    }

    private static Counts count(List<Pair<String, Collection<AnnotatedAttributeValuePair>>> annotations,
                                Predicate<AnnotatedAttributeValuePair> avpPredicate) {
        long docs = 0;
        long values = 0;
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> docAndAnnotations : annotations) {
            long oldValues = values;
            for (AnnotatedAttributeValuePair avp : docAndAnnotations.getValue()) {
                if (avpPredicate.test(avp))
                    values++;
            }
            if (values > oldValues)
                docs++;
        }
        return new Counts(docs, values);
    }

    @Value
    public static class Counts {
        long docs;
        long values;
    }

    public static void main(String[] args) throws IOException {
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> annotations = getAnnotations();
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> docAndAnnotations : annotations) {
            System.out.println(StringUtils.rightPad(docAndAnnotations.getKey(), 50, '='));
            for (AnnotatedAttributeValuePair avp : docAndAnnotations.getValue()) {
                System.out.println(avp);
            }
        }
        sanityChecks(annotations);
    }
}
