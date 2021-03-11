package com.ibm.drl.hbcp.extraction.extractors;

import java.io.Closeable;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.stream.Collectors;

import org.apache.commons.collections4.ListUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extraction.answerselectors.BestAnswerPerArmSelector;
import com.ibm.drl.hbcp.extraction.answerselectors.TopKAnswerSelector;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInDoc;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.EqualityEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.inforetrieval.indexer.ExtractedInfoIndexer;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.util.Props;

/**
 * The main information extractor applying the extraction workflow: all of the
 * attribute extractors + armification
 *
 * @author marting
 */
public class InformationExtractor implements IndexBasedAVPExtractor, Closeable {

    private final List<IndexBasedAVPExtractor> extractors;
    private final ArmsExtractor armExtractor;
    private final Associator<ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>> armAssociator;
    private final JSONRefParser refParser;
    private final Cleaners cleaners;
    // this index is only used for evaluation, normal if not present in production
    private final IndexManager index;
    private final Set<Attribute> relevantAttributesForEvaluation;
	private static final Logger log = LoggerFactory.getLogger(InformationExtractor.class);


    public InformationExtractor(Properties props) throws ParseException, IOException {
        // JSON annotation parser (for attribute names and ground truth in evaluation)
        refParser = new JSONRefParser(props);
        cleaners = new Cleaners(props);
        relevantAttributesForEvaluation = refParser.getAttributes().getAttributeSet();
        // manager over a Lucene index
        index = getDefaultIndexManager(props);
        // AVP extractors
        extractors = Lists.newArrayList(
                // rank 1
                new OutcomeValue(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new PopulationMeanAge(IndexingMethod.slidingWindow(10, props), getNumberOfTopPassages(props, 5)),
                new MeanTobaccoUse(IndexingMethod.slidingWindow(20, props), getNumberOfTopPassages(props, 5)),
                new PopulationProportionFemale(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new PopulationProportionMale(IndexingMethod.slidingWindow(40, props), getNumberOfTopPassages(props, 5)),
                new ProportionEthnicGroups(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new GeographicalLocation(IndexingMethod.slidingWindow(30, props), getNumberOfTopPassages(props, 5)),
                new LongestFollowup(IndexingMethod.slidingWindow(30, props), getNumberOfTopPassages(props, 5)),
                new EffectSizePValue(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new BiochemicalVerification(IndexingMethod.slidingWindow(30, props), getNumberOfTopPassages(props, 5)),
                new EffectSizeEstimate(IndexingMethod.slidingWindow(20, props), getNumberOfTopPassages(props, 5)),
                new PopulationProportionEmployed(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new PopulationEducationAchievedCollege(IndexingMethod.slidingWindow(40, props), getNumberOfTopPassages(props, 5)),
                new SelfReportAssessment(IndexingMethod.slidingWindow(30, props), getNumberOfTopPassages(props, 5)),
                new HealthStatusType(IndexingMethod.slidingWindow(20, props), getNumberOfTopPassages(props, 5)),
                // rank 2
                new AggregateRelationshipStatus(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new ProportionMarried(IndexingMethod.slidingWindow(40, props), getNumberOfTopPassages(props, 5)),
                new MeanEducation(IndexingMethod.slidingWindow(40, props), getNumberOfTopPassages(props, 5)),
                new ProportionByIncome(IndexingMethod.slidingWindow(40, props), getNumberOfTopPassages(props, 5)),
                new ProportionBelongingToIncomeCategory(IndexingMethod.slidingWindow(30, props), getNumberOfTopPassages(props, 5)), // window size?
                new HealthcareFacility(IndexingMethod.slidingWindow(40, props), getNumberOfTopPassages(props, 5)), // window size?
                new DoctorPrimaryCareFacility(IndexingMethod.slidingWindow(40, props), getNumberOfTopPassages(props, 5)),
                new HospitalFacility(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                // new Site(),
                new IndividualLevelAllocated(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new IndividualLevelAnalyzed(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new FaceToFace(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new Distance(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new PrintedMaterial(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new DigitalContentType(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new WebsiteComputerAppMoD(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new Somatic(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new Patch(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new Pill(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new IndividualMoD(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new GroupBased(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new HealthProfessional(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new Psychologist(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new ResearcherNotSpecified(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new OtherInterventionist(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new ExpertiseOfSource(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                // rank 3
                new Dose(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new OverallDuration(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new NumberOfContacts(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new ContactFrequency(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new ContactDuration(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new Format(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new NicotineDependence(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new CognitiveBehaviouralTherapy(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new Mindfulness(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new MotivationalInterviewing(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new BriefAdvice(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new PhysicalActivity(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new IndividualReasonsForAttrition(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new EncounteredIntervention(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new CompletedIntervention(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new SessionsDelivered(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new PharmaceuticalCompanyFunding(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new TobaccoCompanyFunding(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new ResearchGrantFunding(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new NoFunding(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new PharmaceuticalCompanyCompetingInterest(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new TobaccoCompanyCompetingInterest(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new ResearchGrantCompetingInterest(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5)),
                new NoCompetingInterest(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5))
                // other
//                new PopulationMinAge(IndexingMethod.slidingWindow(10, props), getNumberOfTopPassages(props, 5)),
//                new PopulationMaxAge(IndexingMethod.slidingWindow(10, props), getNumberOfTopPassages(props, 5)),
//                new PopulationGender(IndexingMethod.slidingWindow(50, props), getNumberOfTopPassages(props, 5))
        );
        // presence detectors (mostly for BCTs)
        extractors.addAll(buildPresenceDetectors(props, refParser));
        // arm identification
        armExtractor = new ArmsExtractor();
        armAssociator = new ArmAssociator();
    }

    public InformationExtractor(Properties props, Properties extraProps) throws ParseException, IOException {
        this(Props.overrideProps(props, extraProps));
    }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(IndexedDocument doc) throws IOException {
        // first identify the arms
        Collection<CandidateInDoc<Arm>> candidateArms = armExtractor.extract(doc);
        Collection<Arm> arms = candidateArms.stream().map(x -> x.getAnswer()).collect(Collectors.toSet());
        // then run all the extractors 1 by 1, unarmified, then perform armification
        List<CandidateInPassage<ArmifiedAttributeValuePair>> res = new ArrayList<>();
        for (IndexBasedAVPExtractor extractor : extractors) {
            Collection<CandidateInPassage<ArmifiedAttributeValuePair>> unarmifiedValues = extractor.extract(doc);
            // pruning of the results
            unarmifiedValues = new TopKAnswerSelector<ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>>(arms.size()).select(unarmifiedValues);
            // do the armification
            Collection<CandidateInPassage<ArmifiedAttributeValuePair>> armifiedValues = armAssociator.associate(unarmifiedValues, arms);
            // pruning of the results
            armifiedValues = new BestAnswerPerArmSelector<ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>>()
                    .select(armifiedValues);
            res.addAll(armifiedValues);
        }
        return res;
    }

    private static List<GenericPresenceDetector>
            buildPresenceDetectors(Properties props, JSONRefParser parser) {
        String[] attribIds = props.getProperty("attributes.typedetect.ids").split(",");
        List<GenericPresenceDetector> res = new ArrayList<>();
        for (String attribId : attribIds) {
            String query = props.getProperty("attributes.typedetect.query." + attribId);
            // TODO revisit how to keep track of these 'type-detect' attributes
            Attribute attribFromId = parser.getAttributes().getFromId(attribId);
            if (attribFromId != null)
                res.add(new GenericPresenceDetector(IndexingMethod.slidingWindow(10, props),
                        getNumberOfTopPassages(props, 5), attribFromId, query, getThreshold(props, 0.2)));
        }
        return res;
    }

    public Map<IndexBasedAVPExtractor, List<RefComparison>>
            evaluateUnarmified() throws IOException {
        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser, cleaners);
        Map<IndexBasedAVPExtractor, List<RefComparison>> res = new HashMap<>();
        for (IndexBasedAVPExtractor extractor : extractors) {
            res.put(extractor, extractor.evaluate(groundTruth));
        }
        return res;
    }

    public Map<IndexBasedAVPExtractor, List<RefComparison>>
    evaluateUnarmified(List<String> docNames) throws IOException {
        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser, cleaners, docNames);
        Map<IndexBasedAVPExtractor, List<RefComparison>> res = new HashMap<>();
        for (IndexBasedAVPExtractor extractor : extractors) {
            res.put(extractor, extractor.evaluate(groundTruth));
        }
        return res;
    }

    public Map<IndexBasedAVPExtractor, List<RefComparison>> evaluate() throws IOException {
        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser, cleaners);
        Map<IndexBasedAVPExtractor, List<RefComparison>> res = new TreeMap<>();
        for (IndexBasedAVPExtractor extractor : extractors) {
            res.put(extractor, extractor.evaluate(groundTruth, armAssociator, armExtractor));
        }
        return res;
    }

    private static int getNumberOfTopPassages(Properties props, int defaultValue) {
        return Integer.parseInt(props.getProperty("ntoppassages", String.valueOf(defaultValue)));
    }

    private static double getThreshold(Properties props, double defaultValue) {
        return Double.parseDouble(props.getProperty("threshold", String.valueOf(defaultValue)));
    }

    @Override
    public void close() throws IOException {
        if (index != null) {
            index.close();
        }
    }

    @Override
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        return Lists.newArrayList(new EqualityEvaluator<>());
    }

    public ArmsExtractor getArmExtractor() { return armExtractor; }

    public static void evaluate(InformationExtractor ie) throws IOException {
        Map<IndexBasedAVPExtractor, List<RefComparison>> res = ie.evaluate();
        res.forEach((ext, refComps) -> {
            System.out.println("===============================");
            System.out.println(ext);
            System.out.println(StringUtils.join(refComps.stream().map(refComp -> refComp.toString()).toArray(), "\n"));
        });
    }

    public static void evaluate(String[] args) throws IOException, ParseException {
        try (InformationExtractor extractor = new InformationExtractor(Props.loadProperties("init.properties"))) {
            evaluate(extractor);
        }
    }

    private List<Pair<IndexedDocument, Map<Arm, Collection<ArmifiedAttributeValuePair>>>> getGoldClusters(Set<Attribute> relavantAttributes) throws IOException {
        List<Pair<IndexedDocument, Map<Arm, Collection<ArmifiedAttributeValuePair>>>> clusterRes = new ArrayList<>();
        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser, cleaners);
        for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> pairsPerDoc : groundTruth) {
            IndexedDocument doc = pairsPerDoc.getKey();
            Map<Arm, Collection<ArmifiedAttributeValuePair>> docCluster = new HashMap<>();
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                if (!relavantAttributes.contains(cap.getAttribute())) {
                    continue;
                }
                if (docCluster.containsKey(cap.getArm())) {
                    docCluster.get(cap.getArm()).add(cap);
                } else {
                    docCluster.put(cap.getArm(), Sets.newHashSet(cap));
                }
            }
            clusterRes.add(Pair.of(doc, docCluster));
        }
        return clusterRes;
    }

    private Set<Attribute> getEvalAttributs() {
        Set<Attribute> result = new HashSet<>();
        for (IndexBasedAVPExtractor extractor : extractors) {
            result.addAll(extractor.getExtractedAttributes());
        }
        return result;
    }

    public static void evaluteClustering() throws IOException, ParseException {
        Properties props = Props.loadProperties();
        ClusterExtractedEntities armCluster = new ClusterExtractedEntities();
        Map<String, List<String>> goldClusterToken = new HashMap<>();
        Map<String, List<String>> responseClusterToken = new HashMap<>();
        try (InformationExtractor extractor = new InformationExtractor(props)) {
            //get gold cluster infor
            Set<Attribute> evaluatedAttri = extractor.getEvalAttributs();
            List<Pair<IndexedDocument, Map<Arm, Collection<ArmifiedAttributeValuePair>>>> goldClusters = extractor.getGoldClusters(evaluatedAttri);
            for (Pair<IndexedDocument, Map<Arm, Collection<ArmifiedAttributeValuePair>>> docCluster : goldClusters) {
                String doc = docCluster.getKey().getDocName();
                goldClusterToken.put(doc, new ArrayList<>());
                int clusterid = 1;
                System.out.println(doc);
                for (Arm arm : docCluster.getValue().keySet()) {
                    System.out.println("cluster:" + arm.getStandardName());
                    for (ArmifiedAttributeValuePair result : docCluster.getValue().get(arm)) {
                        String token = "";
                        if (result.getAttribute().getType() == AttributeType.INTERVENTION) {
                            token = result.getAttribute().getId() + "_1" + "#" + clusterid;
                        } else {
                            token = result.getAttribute().getId() + "_" + result.getValue().replace(" ", "_") + "#" + clusterid;
                        }
                        goldClusterToken.get(doc).add(token);
                    }
                    clusterid++;
                }
            }

            IndexManager index = extractor.getSlidingWindowIndexManager(props, "10,20");
            for (IndexedDocument doc : index.getAllDocuments()) {
                // this is what the IE app will show
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results = extractor.extract(doc);
                // cluster results on different arms
                Map<Arm, Collection<CandidateInPassage<ArmifiedAttributeValuePair>>> clusterRes = armCluster.cluster(doc, results);
                System.out.println(doc.getDocName());
                responseClusterToken.put(doc.getDocName(), new ArrayList<>());
                int clusterId = 1;
                for (Arm arm : clusterRes.keySet()) {
                    System.out.println("cluster:" + arm.getStandardName());
                    for (CandidateInPassage<ArmifiedAttributeValuePair> result : clusterRes.get(arm)) {
                        String token = result.getAnswer().getAttribute().getId() + "_" + result.getAnswer().getValue().replace(" ", "_") + "#" + clusterId;
//                        String token = result.getAnswer().getAttribute().getId() + result.getAnswer().getValue().replace(" ", "") + "#" + clusterId;
                        responseClusterToken.get(doc.getDocName()).add(token);
                        System.out.println(token);
                    }
                    clusterId++;
                }
            }
        }
        //generate ConLL format for clustering evaluation
        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "clusterEval/key.txt"));
        StringBuffer sb1 = new StringBuffer();
        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "clusterEval/response.txt"));
        StringBuffer sb2 = new StringBuffer();
        for (String doc : responseClusterToken.keySet()) {
            if (goldClusterToken.containsKey(doc)) {
//                List<Pair<String, String>> keyTokens = new ArrayList<>();
                List<String> doctokens_gold = new ArrayList<>();
                List<String> doctokens_response = new ArrayList<>();
                goldClusterToken.get(doc).forEach((goldToken) -> {
                    doctokens_gold.add(goldToken.split("#")[0]);
                });
                responseClusterToken.get(doc).forEach((responseToken) -> {
                    doctokens_response.add(responseToken.split("#")[0]);
                });

                Map<String, Set<String>> responseWordClusterId = new HashMap<>();
                for (String responseToken : responseClusterToken.get(doc)) {
                    String word = responseToken.split("#")[0];
                    String clusterid = responseToken.split("#")[1];
                    if (responseWordClusterId.containsKey(word)) {
                        responseWordClusterId.get(word).add(clusterid);
                    } else {
                        responseWordClusterId.put(word, Sets.newHashSet(clusterid));
                    }
                }

                sb1.append("#begin document (" + doc + ");").append("\n");
                sb2.append("#begin document (" + doc + ");").append("\n");
                int position = 0;
                for (String goldToken : goldClusterToken.get(doc)) {
                    String word = goldToken.split("#")[0];
                    String clusterid = goldToken.split("#")[1];
                    sb1.append(doc + "\t" + "0" + "\t" + position + "\t" + word + "\t" + "(" + clusterid + ")").append("\n");
                    if (responseWordClusterId.containsKey(word)) {
                        Set<String> clusterIDs = responseWordClusterId.get(word);
                        if (clusterIDs.size() > 1) {
                            String firstID = clusterIDs.stream().findFirst().get();
                            sb2.append(doc + "\t" + "0" + "\t" + position + "\t" + word + "\t" + "(" + firstID + ")").append("\n");
                            clusterIDs.remove(firstID);
                            responseWordClusterId.put(word, clusterIDs);
                        } else {
                            String uniqueID = clusterIDs.stream().findFirst().get();
                            sb2.append(doc + "\t" + "0" + "\t" + position + "\t" + word + "\t" + "(" + uniqueID + ")").append("\n");
                        }
                    } else {
                        sb2.append(doc + "\t" + "0" + "\t" + position + "\t" + word + "\t" + "-").append("\n");
                    }
                    position++;
                }
                //list1(gold): [2,3,4,6,7]; list2(response): [1,2,3,4,4,4]
                //return: [1,4,4]
                for (String additionalWord : ListUtils.subtract(doctokens_response, doctokens_gold)) {
                    sb1.append(doc + "\t" + "0" + "\t" + position + "\t" + additionalWord + "\t" + "-").append("\n");
                    //sb2
                    Set<String> clusterIDs = responseWordClusterId.get(additionalWord);
                    if (clusterIDs.size() > 1) {
                        String firstID = clusterIDs.stream().findFirst().get();
                        sb2.append(doc + "\t" + "0" + "\t" + position + "\t" + additionalWord + "\t" + "(" + firstID + ")").append("\n");
                        clusterIDs.remove(firstID);
                        responseWordClusterId.put(additionalWord, clusterIDs);
                    } else {
                        String uniqueID = clusterIDs.stream().findFirst().get();
                        sb2.append(doc + "\t" + "0" + "\t" + position + "\t" + additionalWord + "\t" + "(" + uniqueID + ")").append("\n");
                    }

                    position++;
                }
                sb1.append(doc + "\t" + "0" + "\t" + position + "\t" + "." + "\t" + "-").append("\n");
                sb2.append(doc + "\t" + "0" + "\t" + position + "\t" + "." + "\t" + "-").append("\n");
                sb1.append("\n");
                sb2.append("\n");
                sb1.append("#end document").append("\n");
                sb2.append("#end document").append("\n");
            }
        }
        writer1.write(sb1.toString());
        writer1.close();
        writer2.write(sb2.toString());
        writer2.close();
    }

    public static void extractAndStoreInIndex(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties();
        int valueCount = 0;
        try (ExtractedInfoIndexer indexer = new ExtractedInfoIndexer(Props.getDefaultPropFilename())) {
            try (InformationExtractor extractor = new InformationExtractor(props)) {
                IndexManager index = extractor.getDefaultIndexManager(props);
                for (IndexedDocument doc : index.getAllDocuments()) {
                    Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results = extractor.extract(doc);
                    // index the values
                    for (CandidateInPassage<ArmifiedAttributeValuePair> candidate : results) {
                        indexer.addDocument(candidate.getAnswer().getLuceneDocument());
                    }
                    valueCount += results.size();
                    //System.out.println(results);
                }
            }
        }
        log.debug("#" + valueCount + " entities extracted.");
    }

    public static void runOnAllDocs(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties();
        int valueCount = 0;
        try (InformationExtractor extractor = new InformationExtractor(props)) {
            IndexManager index = extractor.getDefaultIndexManager(props);
            for (IndexedDocument doc : index.getAllDocuments()) {
                // this is what the IE app will show
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> results = extractor.extract(doc);
                // do something with that
                // ...
                valueCount += results.size();
            }
        }
        log.debug("#" + valueCount + " entities extracted.");
    }

    public void count() throws IOException, Exception{
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        Cleaners cleaners = new Cleaners(props);
        IndexManager index = getDefaultIndexManager(props);

        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser, cleaners);
        int instanceNum_annotate = 0;
        for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> pairsPerDoc : groundTruth) {
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                if (cap.getAttribute().getId().equalsIgnoreCase("5140146")) {
                    instanceNum_annotate++;
                }
            }
        }
        System.err.println(instanceNum_annotate);
    }

    public static void main(String[] args) throws Exception {
//        runOnAllDocs(args);
        evaluate(args);
        //extractAndStoreInIndex(args);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributesForEvaluation;
    }
}
