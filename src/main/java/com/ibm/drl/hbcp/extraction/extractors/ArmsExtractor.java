package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import com.aliasi.cluster.CompleteLinkClusterer;
import com.aliasi.cluster.Dendrogram;
import com.aliasi.cluster.HierarchicalClusterer;
import com.aliasi.spell.EditDistance;
import com.aliasi.util.Distance;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInDoc;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.ArmIdentificationEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extractor.RefComparison;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extracts arms found in the paper by combining the number of predicted arms ({@link ArmNumberPrediction})
 * and the extracted names ({@link ArmNames}).
 * 
 * @author yhou
 *
 */
public class ArmsExtractor implements EvaluatedExtractor<IndexedDocument, Arm, CandidateInDoc<Arm>> {

    private ArmNames armNameExtractor;
    private ArmNumberPrediction armNumberPrediction;
    static final Distance<CharSequence> EDIT_DISTANCE = new EditDistance(false);

    public ArmsExtractor() throws ParseException {
        this(100);
    }

    public ArmsExtractor(int numberOfTopPassages) throws ParseException {
        armNameExtractor = new ArmNames(numberOfTopPassages);
        armNumberPrediction = new ArmNumberPrediction(numberOfTopPassages);
    }
    
    static Distance<String> ArmName_DISTANCE = new Distance<String>() {
        public double distance(String name1, String name2) {
            return 1.0 - EDIT_DISTANCE.distance(name1, name2);
        }
    };

    @Override
    public Collection<CandidateInDoc<Arm>> extract(IndexedDocument doc) throws IOException {
        List<CandidateInDoc<Arm>> res = new ArrayList<>();
//        IndexSearcher searcher = doc.getIndexManager().get(doc.getDocId()); // if docId is invalid, this will throw an IOException
//        String docName = doc.getDocName();
        
        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armNamesCollection = armNameExtractor.extract(doc);
        
        Set<String> armNames = new HashSet<>();
        for (CandidateInPassage<ArmifiedAttributeValuePair> candi : armNamesCollection) {
            ArmifiedAttributeValuePair cap = candi.getAnswer();
            armNames.add(cap.getValue());
        }
        // if no arm names, we can't cluster arm names
        if(armNames.isEmpty()) return res;

        // determine number of clusters from number of arm prediction/extraction
        Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armNumbersCollection = armNumberPrediction.extract(doc);
        int armNumber = 2;  // default number of arms
        if (!armNumbersCollection.isEmpty()) {
            String armNumPrediction = armNumbersCollection.stream().findFirst().get().getAnswer().getValue();
            try {
                // number of arms should be an integer (as a String) at this point
                armNumber = Integer.parseInt(armNumPrediction);  
            } catch (NumberFormatException e) {
                e.printStackTrace();
            }
        }

//        for (String s1 : armNames) {
//            for (String s2 : armNames) {
//                if (s1.compareTo(s2) < 0) {
//                    System.out.println("distance(" + s1 + "," + s2 + ")="
//                            + EDIT_DISTANCE.distance(s1, s2));
//                }
//            }
//        }

        // Complete-Link Clusterer
        HierarchicalClusterer<String> clClusterer
                = new CompleteLinkClusterer<String>(Integer.MAX_VALUE, EDIT_DISTANCE);

        Dendrogram<String> clDendrogram = clClusterer.hierarchicalCluster(armNames);
//        System.out.println("\nComplete Link Dendrogram");
//        System.out.println(clDendrogram.prettyPrint());

        // Dendrograms to Clusterings
        int clusterNum = armNumber;
        if (armNames.size() < armNumber) {
            // use the predicted number of arms unless there are fewer arm names than predicted arms
            clusterNum = armNames.size();
        }
//        System.out.println("\nComplete Link Clusterings");
        Set<Set<String>> clKClustering = clDendrogram.partitionK(clusterNum);
//        System.out.println(clKClustering);

        for (Set<String> arm_mentions: clKClustering) {
            // pick arm name
            String standardArmName = getStandardMention(arm_mentions, armNamesCollection);
            Arm arm = new Arm(standardArmName);
            for (String str: arm_mentions) {
                arm.addName(str);
            }
            CandidateInDoc<Arm> candidateArm = new CandidateInDoc<>(doc, arm);
            res.add(candidateArm);
        }
        return res;
    }

    private String getStandardMention(Set<String> arm_mentions,
            Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armNamesCollection) {
        // TODO currently this just takes the first name in the list as the 'standard' name, we could look for the most frequent as the standard
        return arm_mentions.stream().findFirst().get();
    }

    @Override
    public List<Evaluator<IndexedDocument, Arm>> getEvaluators() {
        return Lists.newArrayList(
//                new ArmIdentificationEvaluator<IndexedDocument>() {
//                },
                new ArmIdentificationEvaluator<IndexedDocument>() {
                    @Override
                    public boolean isCorrect(@NotNull Arm predicted, @NotNull Arm expected) {
                        return predicted.getStandardName().equalsIgnoreCase(expected.getStandardName());
                    }
                },
                new ArmIdentificationEvaluator<IndexedDocument>() {
                    @Override
                    public boolean isCorrect(@NotNull Arm predicted, @NotNull Arm expected) {
                        return predicted.getStandardName().toLowerCase().contains(expected.getStandardName().toLowerCase()) ||
                                expected.getStandardName().toLowerCase().contains(predicted.getStandardName().toLowerCase());
                    }
                }
        );
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        ArmsExtractor armsExtractor = new ArmsExtractor();
        try (IndexManager index = armsExtractor.armNameExtractor.getDefaultIndexManager(props)) {
            JSONRefParser refParser = new JSONRefParser(props);
            List<IndexedDocument> allDocs = index.getAllDocuments();
            List<Pair<IndexedDocument, Collection<Arm>>> groundTruth = new ArrayList<>(allDocs.size());
            Map<String, Set<Arm>> armsInfoPerDoc = refParser.getArmsInfo();
            for (IndexedDocument doc : allDocs) {
                Collection<Arm> annotations = armsInfoPerDoc.get(doc.getDocName());
                // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
                if (annotations == null) {
                    String docname = doc.getDocName();
                    System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                    continue;
                }
                groundTruth.add(Pair.of(doc, annotations));
                System.err.println("doc:" + doc.getDocName());
                Collection<CandidateInDoc<Arm>> prediction = armsExtractor.extract(doc);
                String goldCluster = "gold:";
                for(Arm goldArm: annotations){
                    goldCluster = goldCluster + "--" + goldArm.getId()+":" + goldArm.getAllNames().toString();
                }
                String predictCluster = "predict:";
                for(CandidateInDoc<Arm> arm: prediction){
                    predictCluster = predictCluster + "--" + arm.getAnswer().getAllNames().toString();
                }
                System.err.println("annotation:" + goldCluster);
                System.err.println("prediction:" + predictCluster);
            }
            for (Evaluator<IndexedDocument, Arm> evaluator : armsExtractor.getEvaluators()) {
                RefComparison singleEvaluation = evaluator.evaluate(armsExtractor, groundTruth);
                System.out.println(singleEvaluation);
            }
        }
    }

}
