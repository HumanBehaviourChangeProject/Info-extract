package com.ibm.drl.hbcp.extraction.extractors.annotations;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.extraction.candidates.Candidate;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.extraction.extractors.*;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.util.Props;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

public class NonAnnotatedAttributesEnricher {

    public static List<IndexBasedAVPExtractor> getExtractors(Properties props) throws ParseException {
        return Lists.newArrayList(
                new AbstinenceContinuous(IndexingMethod.slidingWindow(50, props), 5),
                new AbstinencePointPrevalent(IndexingMethod.slidingWindow(50, props), 5)
        );
    }

    public static AttributeValueCollection<AnnotatedAttributeValuePair> getExtractedNonAnnotatedAttributes() throws IOException, ParseException {
        Properties props = Props.loadProperties();
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        for (IndexBasedAVPExtractor extractor : getExtractors(props)) {
            IndexManager index = extractor.getDefaultIndexManager(props);
            for (IndexedDocument doc : index.getAllDocuments()) {
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results = extractor.extract(doc);
                for (CandidateInPassage<ArmifiedAttributeValuePair> extractedValue : results) {
                    AnnotatedAttributeValuePair aavp = new AnnotatedAttributeValuePair(
                            extractedValue.getAnswer().getAttribute(),
                            extractedValue.getAnswer().getValue(),
                            doc.getDocName(),
                            Arm.EMPTY,
                            extractedValue.getAnswer().getContext(),
                            extractedValue.getAnswer().getValue(),
                            "extracted",
                            Integer.parseInt(extractedValue.getAnswer().getPageNumber())
                    );
                    res.add(aavp);
                }
            }
        }
        return new AttributeValueCollection<>(res);
    }

    public static void main(String[] args) throws IOException, ParseException {
        AttributeValueCollection<AnnotatedAttributeValuePair> values = getExtractedNonAnnotatedAttributes();
        int docsWithBoth = 0;
        for (String docName : values.getDocNames()) {
            if (values.byDoc().get(docName).stream().map(aavp -> aavp.getAttribute()).distinct().count() >= 2) {
                System.out.println("== " + docName + " ===================");
                for (AnnotatedAttributeValuePair value : values.byDoc().get(docName)) {
                    System.out.println(value.getAttribute());
                    System.out.println("Value: " + value.getValue());
                    System.out.println("Context: " + value.getContext());
                    System.out.println();
                }
                docsWithBoth++;
            }
        }
        System.out.println("Docs with both asbtinence types: " + docsWithBoth);
        System.out.println("Total docs with 1 value retrieved: " + values.getDocNames().size());
    }
}
