package com.ibm.drl.hbcp.extraction.extractors;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.ArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.indexing.SentenceBasedIndexManager;
import com.ibm.drl.hbcp.extraction.indexing.SlidingWindowIndexManager;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.File;
import java.io.IOException;
import java.util.*;

/**
 * A shortcut type for an extractor returning attribute-value pairs with an attached passage.
 *
 * @author marting
 * */
public interface IndexBasedAVPExtractor extends
        Comparable<IndexBasedAVPExtractor>,
        AttributeExtractor<IndexedDocument, ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>> {


    /** Evaluates the extractor based on common index and JSON annotation file settings as defined by properties
     * @return A RefComparison (summary of metrics) for each of the evaluators defined by the extractor
     * */
    default List<RefComparison> evaluate(Properties props, Collection<String> testDocNames) throws IOException {
        try (IndexManager index = getDefaultIndexManager(props)) {
            JSONRefParser refParser = new JSONRefParser(props);
            List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth =
                    getGroundTruthForEvaluation(index, refParser, new Cleaners(props), testDocNames);
            List<RefComparison> res = new ArrayList<>();
            for (Evaluator<IndexedDocument, ArmifiedAttributeValuePair> evaluator : getEvaluators()) {
                RefComparison singleEvaluation = evaluator.evaluate(this, groundTruth);
                res.add(singleEvaluation);
            }
            return res;
        }
    }

    default List<RefComparison> evaluate(Properties props) throws IOException {
        return evaluate(props, null);
    }

    /** Returns an armified evaluation result (RefComparison) for each evaluator run over the whole list of annotated documents */
    default List<RefComparison> evaluate(List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth,
                                         Associator<ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>> armAssociator,
                                         ArmsExtractor armExtractor) throws IOException {
        List<RefComparison> res = new ArrayList<>();
        for (Evaluator<IndexedDocument, ArmifiedAttributeValuePair> evaluator : getEvaluators()) {
            if (evaluator instanceof ArmifiedEvaluator) {
                ArmifiedEvaluator armifiedEvaluator = (ArmifiedEvaluator) evaluator;
                RefComparison evaluation = armifiedEvaluator.evaluate(this, groundTruth, armAssociator, armExtractor);
                res.add(evaluation);
            } else {
                RefComparison evaluation = evaluator.evaluate(this, groundTruth);
                res.add(evaluation);
            }
        }
        return res;
    }

    /** Returns a list of documents paired with their annotations from a doc index and a JSON annotation parser.
     * Useful to quickly evaluate the extractor with a default index/JSON. */
    default List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> getGroundTruthForEvaluation(
            IndexManager index,
            JSONRefParser refParser,
            Cleaners cleaners,
            Collection<String> testDocNames
    ) throws IOException {
        List<IndexedDocument> allDocs = index.getAllDocuments();
        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> res = new ArrayList<>();
        AttributeValueCollection<AnnotatedAttributeValuePair> originals = refParser.getAttributeValuePairs();
        AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(originals);
        try {
            for (IndexedDocument doc : allDocs) {
                String docname = doc.getDocName();
                if (testDocNames == null || testDocNames.isEmpty() || testDocNames.contains(docname)) {
                    Collection<? extends ArmifiedAttributeValuePair> annotations = cleaned.byDoc().get(doc.getDocName());
                    // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
                    if (annotations == null) {
                        System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                        continue;
                    }
                    res.add(Pair.of(doc, new ArrayList<>(annotations)));
                }
            }
            return res;
        } catch (Exception e) {
            throw new IOException(e);
        }
    }

    /** Returns a list of documents paired with their annotations from a doc index and a JSON annotation parser.
     * Useful to quickly evaluate the extractor with a default index/JSON. */
    default List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> getGroundTruthForEvaluation(
            IndexManager index,
            JSONRefParser refParser,
            Cleaners cleaners
    ) throws IOException {
        return getGroundTruthForEvaluation(index, refParser, cleaners,  null);
    }

    /** Get an index manager as defined by the properties. Can fail if properties are missing or wrongly defined */
    default IndexManager getDefaultIndexManager(Properties props) throws IOException {
        // figure out the directory of the index
        String indexPath = props.getProperty("index");
        File indexDir = new File(indexPath);
        try {
            Directory directory = FSDirectory.open(indexDir.toPath());
            // pick between sentence-based or sliding window paragraphs
            if (Boolean.parseBoolean(props.getProperty("use.sentence.based"))) {
                int numberOfSentencesPerParagraph = Integer.parseInt(props.getProperty("para.number.of.sentences"));
                return new SentenceBasedIndexManager(directory, numberOfSentencesPerParagraph, IndexManager.DEFAULT_ANALYZER);
            } else {
                return new SlidingWindowIndexManager(directory, props.getProperty("window.sizes").split(","), IndexManager.DEFAULT_ANALYZER);
            }
        } catch (org.apache.lucene.index.IndexNotFoundException indexNotFound) {
            // the code is probably running inside Docker, so the index hasn't been included or created at initialization
            // the index being only used during evaluation, and evaluation not happening in production,
            // it's fine for the IndexManager to remain null here
            return null;
        }
    }

    default IndexManager getSlidingWindowIndexManager(Properties props, String windowSizes) throws IOException {
        // figure out the directory of the index
        String indexPath = props.getProperty("index");
        File indexDir = new File(indexPath);
        Directory directory = FSDirectory.open(indexDir.toPath());
        return new SlidingWindowIndexManager(directory, windowSizes.split(","), IndexManager.DEFAULT_ANALYZER);
    }

    default int compareTo(IndexBasedAVPExtractor o) {
        return StringUtils.compare(toString(), o.toString());
    }
}
