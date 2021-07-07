package com.ibm.drl.hbcp.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.parser.cleaning.typing.LineConsistencyChecker;
import com.ibm.drl.hbcp.parser.jsonstructure.*;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
import lombok.Data;
import net.arnx.jsonic.JSON;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Parser for JSON file that handles both the 'code set' hierarchy for modeling
 * attributes (see {@link CodeSetTree}), and the references or instances of those
 * attributes which are found in documents (see {@link PerDocRefs}, which are members
 * of a {@link CodeSetTreeNode}).
 *
 * @author dganguly, marting
 */
public class JSONRefParser implements JSONRefParserBase {

    /**
     * Json file containing the Behaviour Change annotations
     */
    private final File jsonFile;

    // output
    private final JsonAnnotationFile annotationFile; // this can even be discarded, for more memory efficiency
    // attributes in the codesets (all)
    protected final Attributes attributes;
    // arms indexed by their id
    protected final Map<Integer, Arm> arms;
    private final boolean isArmified;
    // attribute instances (attribute value pairs)
    protected final AttributeValueCollection<AnnotatedAttributeValuePair> instances;
    //private final Map<AttributeType, CodeSetTree> trees;
    protected final CodeSetTree[] trees;
    // armification based on item id
    private final boolean isArmifiedBasedOnItemId;

    // useful but less essential information
    private final Map<String, PdfInfo> docNameToPdfInfo;
    private final Map<String, PdfInfo> shortTitleToPdfInfo;

    protected static final String ARM_ATTRIBUTE_NAME = "Arm name";
    private static final List<String> CONTEXT_SEPARATORS = Lists.newArrayList(" ; ", " ;;; ");
    private static Logger logger = LoggerFactory.getLogger(JSONRefParser.class);

    /**
     * Parses a JSON file containing Behaviour Change annotations.
     *
     * @param jsonFile a JSON file
     * @throws IOException occurs if the file didn't match the expected JSON structure, or otherwise in other traditional I/O-related cases
     */
    public JSONRefParser(File jsonFile, boolean isArmifiedBasedOnItemId) throws IOException {
        jsonFile = FileUtils.potentiallyGetAsResource(jsonFile);
        this.jsonFile = jsonFile;
        this.isArmifiedBasedOnItemId = isArmifiedBasedOnItemId;

        // map the JSON to the corresponding POJO objects as a first parsing step
        annotationFile = getJsonAnnotationFile(jsonFile);
        // parse the attributes
        attributes = new Attributes(annotationFile);
        // parse the arms
        isArmified = isArmified(annotationFile);
        arms = getArms(annotationFile);
        // get all the attribute instances
        instances = new AttributeValueCollection<>(getInstances(annotationFile));
        // build the CodeSetTree's (compatibility with first JSONRefParser)
        trees = new CodeSetTreeBuilder(annotationFile, attributes, instances).getTrees();
        docNameToPdfInfo = getDocNameToPdfInfo(annotationFile);
        shortTitleToPdfInfo = getShortTitleToPdfInfo(annotationFile);
    }

    public JSONRefParser(File jsonFile) throws IOException {
        this(jsonFile, false);
    }

    public JSONRefParser(Properties props) throws IOException {
        this(new File(BaseDirInfo.getBaseDir() + props.getProperty("ref.json")));
    }

    public JSONRefParser(String propFileName) throws IOException {
        this(Props.loadProperties(propFileName));
    }

    public JSONRefParser(URL url) throws IOException, URISyntaxException {
        this(new File(url.toURI()));
    }

    /**
     * Returns the CodeSetTree's
     */
    public CodeSetTree[] getTrees() {
        return trees;
    }

    /**
     * Returns all the attribute-value pairs parsed.
     */
    public AttributeValueCollection<AnnotatedAttributeValuePair> getAttributeValuePairs() {
        return instances;
    }

    static JsonAnnotationFile getJsonAnnotationFile(File jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategy.UpperCamelCaseStrategy());
        return objectMapper.readValue(jsonFile, JsonAnnotationFile.class);
    }

    protected Map<Integer, Arm> getArms(JsonAnnotationFile json) {
        Map<Integer, Arm> res = new HashMap<>();
        if (isArmifiedBasedOnItemId) {
            // go through all instances and get all the arm definitions (item id)
            for (JsonReference reference : json.getReferences()) {
                Arm arm = new Arm(String.valueOf(reference.getItemId()), reference.getShortTitle() + "-" + reference.getItemId());
                res.put(reference.getItemId(), arm);
            }
        } else {
            if (isArmified) {
                // first get the arm attribute id
                String armNameId = attributes.getFromName(ARM_ATTRIBUTE_NAME).getId();
                // go through all instances and get all the arm definitions (codes matching the armNameId)
                for (JsonReference reference : json.getReferences()) {
                    for (JsonCode code : reference.getCodes()) {
                        if (String.valueOf(code.getAttributeId()).equals(armNameId)) {
                            Arm arm = getArm(code);
                            res.put(code.getArmId(), arm);
                        }
                    }
                }
            }
            // put the empty arm if not already added
            res.putIfAbsent(0, Arm.EMPTY);
        }
        return res;
    }

    private List<AnnotatedAttributeValuePair> getInstances(JsonAnnotationFile json) {
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        for (JsonReference reference : json.getReferences()) { // a reference is an annotated document, basically
            if (reference.getCodes() != null) { // null can happen when no annotation has been done on the document
                for (JsonCode code : reference.getCodes()) {
                    // TODO: some codes are completely empty for some reason, with attribute ID: 3689776
                    if (code.getItemAttributeFullTextDetails() != null)
                        res.addAll(getAttributeValuePairs(code, reference.getItemId()));
                }
            }
            // separate handling of outcomes (only used for physical activity)
            if (reference.getOutcomes() != null) {
                for (JsonOutcome outcome : reference.getOutcomes()) {
                    res.addAll(getAttributeValuePairs(outcome, reference));
                }
            }
        }
        return res;
    }

    private List<AnnotatedAttributeValuePair> getAttributeValuePairs(JsonCode code, int itemId) {
        // analyze the context first (can contain multiple individual contexts for different values)
        String sharedContext = removeSpecialCharacters(code.getAdditionalText());
        List<String> splitContexts = ParsingUtils.split(sharedContext, CONTEXT_SEPARATORS);
        if (splitContexts.size() != code.getItemAttributeFullTextDetails().length) {
            // we couldn't match the contexts, keep the whole shared one, but deal with the multiple JsonItemAttributeFullTextDetail
            return Arrays.stream(code.getItemAttributeFullTextDetails())
                    .flatMap(fullTextDetail -> getAttributeValuePairs(fullTextDetail, code, sharedContext, itemId).stream())
                    .collect(Collectors.toList());
        } else {
            // we have a 1-for-1 match between contexts and itemAttributeFullTextDetails
            List<AnnotatedAttributeValuePair> res = new ArrayList<>();
            for (int i = 0; i < splitContexts.size(); i++) {
                JsonItemAttributeFullTextDetail fullTextDetail = code.getItemAttributeFullTextDetails()[i];
                res.addAll(getAttributeValuePairs(fullTextDetail, code, splitContexts.get(i), itemId));
            }
            return res;
        }
    }

    private List<? extends AnnotatedAttributeValuePair> getAttributeValuePairs(JsonItemAttributeFullTextDetail detail, JsonCode code, String context, int itemId) {
        String docName = detail.getDocTitle();
        String highlightedText = removeSpecialCharacters(detail.getText());
        String pageNumber = detail.getText().split(":")[0];
        int annotationPage;
        try {
            annotationPage = Integer.parseInt(pageNumber.replace("Page ", "")) - 1;
            if (annotationPage < 0) throw new NumberFormatException("Page was already 0 in the annotations, that's weird.");
        } catch (NumberFormatException e) {
            logger.warn("No page annotation for entity of paper: {}", docName);
            annotationPage = 0;
        }
        String sprintNo = code.getSprintNo() != null ? code.getSprintNo() : "";
        if (isArmified)
            sprintNo = "Sprint5";
        Arm arm = getAssignedArm(code, itemId);
        Attribute attribute;
        attribute = attributes.getFromId(String.valueOf(code.getAttributeId()));
        if (attribute == null) {
            // this happens in rare cases where a codeset node was deleted but children nodes (detached) remain
            // and so do their annotations (see internal HBCP Slack discussion of 24/04/2019)
            // this is (semi-)expected
            logger.debug("Attribute was NOT DEFINED in the CodeSets: {}. Ignored.", code.getAttributeId());
            return Lists.newArrayList();
        }
        // here split the highlighted text using the "\n" separators
        String[] values = highlightedText.split("\\n");
        final Arm finalArm = arm;
        final String finalSprintNo = sprintNo;
        final int finalAnnotationPage = annotationPage;
        if (shouldCreateNameNumberPairs(attribute)) {
            List<AnnotatedAttributeNameNumberTriple> res = new ArrayList<>();
            for (int i = 0; i + 1 < values.length; i += 2) { // assume that the values contain successive name-number pairs
                AnnotatedAttributeNameNumberTriple triple = new AnnotatedAttributeNameNumberTriple(attribute,
                        values[i],
                        values[i + 1],
                        docName, finalArm, context, finalSprintNo, finalAnnotationPage);
                res.add(triple);
            }
            return res;
        } else if (shouldCreateSeveralValues(attribute, values)) {
            return Arrays.stream(values)
                    .map(value -> new AnnotatedAttributeValuePair(attribute, value, docName, finalArm, context, value, finalSprintNo, finalAnnotationPage))
                    .collect(Collectors.toList());
        } else {
            return Lists.newArrayList(
                    new AnnotatedAttributeValuePair(attribute, highlightedText, docName, finalArm, context, highlightedText, finalSprintNo, finalAnnotationPage)
            );
        }
    }

    private List<AnnotatedAttributeValuePair> getAttributeValuePairs(JsonOutcome outcome, JsonReference reference) {
        Optional<String> docNameTrue = getFirstDocname(reference);
        String docName = docNameTrue.orElse(outcome.getShortTitle());
        // we use the highlighted text to put the table caption
        String highlightedText = outcome.getOutcomeDescription();
        Arm arm1 = getAssignedArm(outcome.getItemArmIdGrp1(), outcome.getGrp1ArmName());
        Arm arm2 = getAssignedArm(outcome.getItemArmIdGrp2(), outcome.getGrp2ArmName());
        // outcome values
        Attribute ovAttribute = attributes.getFromName("Outcome value");
        AnnotatedAttributeValuePair ov1 = new AnnotatedAttributeValuePair(ovAttribute, outcome.getData3(),
                docName, arm1, "", highlightedText, "", 0);
        AnnotatedAttributeValuePair ov2 = new AnnotatedAttributeValuePair(ovAttribute, outcome.getData4(),
                docName, arm2, "", highlightedText, "", 0);
        // timepoints
        Attribute timepointAttribute = attributes.getFromName("Longest follow up");
        AnnotatedAttributeValuePair tp1 = new AnnotatedAttributeValuePair(timepointAttribute, outcome.getTimepointString(),
                docName, arm1, "", highlightedText, "", 0);
        AnnotatedAttributeValuePair tp2 = new AnnotatedAttributeValuePair(timepointAttribute, outcome.getTimepointString(),
                docName, arm2, "", highlightedText, "", 0);
        // timepoint units
        Attribute timepointUnitAttribute = attributes.getFromName("Longest follow up (metric)");
        AnnotatedAttributeValuePair tpUnit1 = new AnnotatedAttributeValuePair(timepointUnitAttribute, outcome.getItemTimepointMetric(),
                docName, arm1, "", highlightedText, "", 0);
        AnnotatedAttributeValuePair tpUnit2 = new AnnotatedAttributeValuePair(timepointUnitAttribute, outcome.getItemTimepointMetric(),
                docName, arm2, "", highlightedText, "", 0);
        // sample size
        Attribute samplesizeAttribute = attributes.getFromName("Individual-level analysed");
        AnnotatedAttributeValuePair ss1 = new AnnotatedAttributeValuePair(samplesizeAttribute, outcome.getData1(),
                docName, arm1, "", highlightedText, "", 0);
        AnnotatedAttributeValuePair ss2 = new AnnotatedAttributeValuePair(samplesizeAttribute, outcome.getData2(),
                docName, arm2, "", highlightedText, "", 0);
        // TODO: anything else?
        return Lists.newArrayList(ov1, ov2, tp1, tp2, tpUnit1, tpUnit2, ss1, ss2);
    }

    protected Arm getAssignedArm(JsonCode code, int itemId) {
        int armId = isArmifiedBasedOnItemId ? itemId : (isArmified ? code.getArmId() : 0);
        return getAssignedArm(armId, code.getArmTitle());
    }

    protected Arm getAssignedArm(int armId, String armTitle) {
        Arm arm = arms.get(armId);
        if (arm == null) {
            // this means the arm is implicit in the document (not annotated/declared), we add it on-the-fly
            // this is an expected behavior
            arm = new Arm(String.valueOf(armId), armTitle);
            logger.debug("Arm {} was implicit (not declared/annotated).", armId);
            arms.put(armId, arm);
        }
        return arm;
    }

    private boolean shouldCreateNameNumberPairs(Attribute attribute) {
        return AnnotatedAttributeNameNumberTriple.ATTRIBUTE_NAMES.contains(attribute.getName());
    }

    private boolean shouldCreateSeveralValues(Attribute attribute, String[] values) {
        return attribute.getType() != AttributeType.ARM && (
                attribute.getType() == AttributeType.INTERVENTION
                || LineConsistencyChecker.areLinesHomogeneous(values)
        );
    }

    protected static Arm getArm(JsonCode code) {
        List<String> allNames = Arrays.stream(code.getItemAttributeFullTextDetails())
                .map(JsonItemAttributeFullTextDetail::getText)
                .flatMap(text -> parseArmNames(text).stream())
                .collect(Collectors.toList());
        return new Arm(String.valueOf(code.getArmId()), removeSpecialCharacters(code.getArmTitle()), allNames);
    }

    protected static List<String> parseArmNames(String text) {
        List<String> res = new ArrayList<>();
        for (String str : text.split("(?<!- )\n")) { // this negative lookbehind handles line-break hyphens
            String cleanStr = removeSpecialCharacters(str);
            if (!cleanStr.isEmpty())
                res.add(cleanStr);
        }
        return res;
    }

    private boolean isArmified(JsonAnnotationFile json) {
        return Arrays.stream(json.getCodeSets())
                .anyMatch(codeSet -> AttributeType.fromName(codeSet.getSetName()) == AttributeType.ARM);
    }

    /**
     * True if the JSON file comes from an armified annotation, i.e. the codeset contains an "Arms" attribute type
     */
    private boolean isArmified() {
        return isArmified;
    }

    private Map<String, PdfInfo> getDocNameToPdfInfo(JsonAnnotationFile json) {
        Map<String, PdfInfo> res = new HashMap<>();
        for (JsonReference reference : json.getReferences()) { // a reference is an annotated document, basically
            getFirstDocname(reference).ifPresent(docname ->
                    res.put(docname, new PdfInfo(docname, reference.getTitle(), reference.getAbstract(), reference.getShortTitle())));
        }
        return res;
    }

    private Map<String, PdfInfo> getShortTitleToPdfInfo(JsonAnnotationFile json) {
        Map<String, PdfInfo> res = new HashMap<>();
        for (JsonReference reference : json.getReferences()) { // a reference is an annotated document, basically
            getFirstDocname(reference).ifPresent(docname ->
                    res.put(reference.getShortTitle(), new PdfInfo(docname, reference.getTitle(), reference.getAbstract(), reference.getShortTitle())));
        }
        return res;
    }


    private Optional<String> getFirstDocname(JsonReference reference) {
        if (reference.getCodes() != null) { // null can happen when no annotation has been done on the document
            for (JsonCode code : reference.getCodes()) {
                if (code.getItemAttributeFullTextDetails() != null) {
                    List<AnnotatedAttributeValuePair> avp = getAttributeValuePairs(code, reference.getItemId());
                    if (!avp.isEmpty())
                        return Optional.of(avp.get(0).getDocName());
                }
            }
        }
        return Optional.empty();
    }

    @Data
    public static class PdfInfo {
        private final String filename;
        private final String title;
        private final String introduction;
        private final String shortTitle;

        public String getTitleFirstAuthorAndDate() {
            return title + ", " + shortTitle;
        }
    }

    /**
     * Removes the special characters from a string, e.g. the patterns
     * "\n", "[¬s]" from the JSON values.
     * @param x the input string
     * @return an output string with the special characters removed
     */
    public static String removeSpecialCharacters(String x) {
        x = x.replaceAll("\\r", " ");
        x = x.replaceAll("Page [0-9]+:\\n?", "");
        x = x.replaceAll("\\[¬s\\]", "");
        x = x.replaceAll("\\[¬e\\]", "");
        x = x.replaceAll("\"", "");
        x = x.replaceAll("·", "."); // the dot character is not a simple ASCII one in the JSON file
        // targets line break hyphens and only them (not " - " which can be a legit separator), hence the negative lookbehind
        x = x.replaceAll("(?<![ \\t])- \\n?", "");
        x = x.trim();
        return x;
    }

    public Map<String, Set<Arm>> getArmsInfo() {
        Map<String, Set<Arm>> armsPerDoc = new HashMap<>();
        for(String doc : instances.byDoc().keySet()){
            armsPerDoc.put(doc,
                    instances.byDoc().get(doc).stream()
                            .map(AnnotatedAttributeValuePair::getArm)
                            .collect(Collectors.toSet()));
        }
        return armsPerDoc;
    }

    @Override
    public void save() throws IOException { }

    @Override
    public void parse() throws IOException { }

    @Override
    public void loadCodeSets() throws IOException { }

    @Override
    public CodeSetTree loadCodeSet(int code) throws IOException { return trees[code]; }

    @Override
    public void groupByDocs() throws IOException {  }

    @Override
    public CodeSetTree getGroundTruths(int code) { return trees[code]; }

    @Override
    public void buildAll() throws IOException { }

    @Override
    public void buildCodeSetsFromURL(URL url) throws IOException {
        throw new NotImplementedException("This is only used in the servlet stuff.");
    }

    @Override
    public Map<String, Set<String>> docsBySprint() throws IOException {
        Map<String, Set<String>> res = new HashMap<>();
        for (AnnotatedAttributeValuePair avp : instances) {
            res.putIfAbsent(avp.getSprintNo(), new HashSet<>());
            res.get(avp.getSprintNo()).add(avp.getDocName());
        }
        return res;
    }

    @Override
    public void groupByDocs(CodeSetTree tree) throws IOException {  }

    @Override
    public Attributes getAttributes() { return attributes; }

    public File getFile() { return jsonFile; }

    public Optional<PdfInfo> getDocInfo(String docname) {
        PdfInfo res = docNameToPdfInfo.get(docname);
        return Optional.ofNullable(res);
    }

    public Optional<PdfInfo> getDocInfoFromShortTitle(String shortTitle) {
        PdfInfo res = shortTitleToPdfInfo.get(shortTitle);
        return Optional.ofNullable(res);
    }

    @Override
    public String getJSON(int code) {
        return trees[code].getJSON();
    }

    public static void printAll(String[] args) throws IOException {
        String path = "data/jsons/SmokingPapers_All_Attributes.json";
        System.setOut(new PrintStream(new File("output/log.txt")));
        System.out.println("Parsing " + path);
        JSONRefParser refParser = new JSONRefParser(new File(path));
        Map<Set<Attribute>, List<Collection<AnnotatedAttributeValuePair>>> armifiedPerBCTSet = new HashMap<>();
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs().distributeEmptyArm();
        for (String docname : annotations.byDoc().keySet()) {
            for (Arm arm : annotations.getArmifiedPairsInDoc(docname).keySet()) {
                Set<Attribute> bcts = annotations.getArmifiedPairsInDoc(docname).get(arm).stream()
                        .filter(aavp -> aavp.getAttribute().getType() == AttributeType.INTERVENTION)
                        .map(aavp -> aavp.getAttribute())
                        .collect(Collectors.toSet());
                armifiedPerBCTSet.putIfAbsent(bcts, new ArrayList<>());
                armifiedPerBCTSet.get(bcts).add(annotations.getArmifiedPairsInDoc(docname).get(arm));
            }
        }
        for (Map.Entry<Set<Attribute>, List<Collection<AnnotatedAttributeValuePair>>> bctSet :
                new ArrayList<>(armifiedPerBCTSet.entrySet()).stream().sorted(Map.Entry.comparingByValue(Comparator.comparing(l -> - l.size()))).collect(Collectors.toList())) {
            if (bctSet.getKey().size() > 0) {
                System.out.println("=============== BCTs ===============");
                System.out.println(bctSet.getKey());
                System.out.println(bctSet.getValue().size());
                for (Collection<AnnotatedAttributeValuePair> avps : bctSet.getValue()) {
                    Optional<AnnotatedAttributeValuePair> outcomeValue = avps.stream().filter(aavp -> aavp.getAttribute().getName().equals("Outcome value")).findFirst();
                    if (outcomeValue.isPresent()) {
                        System.out.println("------------------------------------");
                        System.out.println(outcomeValue.get().getValue());
                        for (AnnotatedAttributeValuePair avp : avps) {
                            System.out.println("\t" + avp.getAttribute().getName() + " <-> " + avp.getSingleLineValue());
                        }
                    }
                }
            }
        }
        System.out.println(refParser.getAttributes().getAttributeSet().size());
        System.out.println("Done.");
    }

    public static void displayAllNameNumberAnnotations() throws IOException {
        JSONRefParser parser = new JSONRefParser(new File("data/jsons/All_annotations_512papers_05March20.json"));
        List<AnnotatedAttributeNameNumberTriple> nameNumberValues = parser.getAttributeValuePairs().stream()
                .filter(AnnotatedAttributeValuePair::isNameNumberPair)
                .map(avp -> (AnnotatedAttributeNameNumberTriple)avp)
                .collect(Collectors.toList());
        for (AnnotatedAttributeNameNumberTriple avp : nameNumberValues) {
            System.out.println(avp.getValue());
        }
        System.out.println(nameNumberValues.size());
    }

    public static void displayReachAttributes() throws IOException {
        JSONRefParser parser = new JSONRefParser(new File("../data/jsons/All_annotations_512papers_05March20.json"));
        for (AnnotatedAttributeValuePair avp : parser.getAttributeValuePairs()) {
            if (avp.getAttribute().getName().contains(" analysed")) {
                System.out.println("For doc: " + avp.getDocName());
                System.out.println(avp);
            }
        }
    }

    public static void outcomeValuesStats() throws IOException {
        JSONRefParser parser = new JSONRefParser(new File("../data/jsons/All_annotations_512papers_05March20.json"));
        double total = 0;
        List<Double> values = new ArrayList<>();
        // compute mean
        double mean = 0.0;
        for (AnnotatedAttributeValuePair outcome : parser.getAttributeValuePairs().byId().get(Attributes.get().getFromName("Outcome value").getId())) {
            try {
                double value = ParsingUtils.parseFirstDouble(outcome.getValue());
                mean += value;
                values.add(value);
                total++;
            } catch (NumberFormatException e) {

            }
        }
        mean /= total;
        // compute SD
        double sd = 0.0;
        for (double value : values) {
            sd += (value - mean) * (value - mean);
        }
        sd /= total;
        sd = Math.sqrt(sd);
        System.out.println("Mean = " + mean);
        System.out.println("SD = " + sd);
    }

    public static void countAttributes() throws IOException {
        JSONRefParser parser = new JSONRefParser(new File("../data/jsons/All_annotations_512papers_05March20.json"));
        System.out.println("Attribute count: " + parser.getAttributeValuePairs().getAllAttributeIds().size());
    }

    public static void countAttributesPA() throws IOException {
        JSONRefParser parser = new JSONRefParser(new File("data/PhysicalActivity Sprint1ArmsAnd Prioritised47Papers.json"));
        System.out.println("Docs: " + parser.getAttributeValuePairs().getDocNames().size());
        parser = new JSONRefParser(new File("data/Batch2PhysicalActivityPrioritisedCodeset.json"));
        System.out.println("Docs: " + parser.getAttributeValuePairs().getDocNames().size());
    }

    public static void mainTableGrammar() throws IOException {
        // TODO: this file is not a resource yet
        JSONRefParser parser = new JSONRefParser(new File("../data/jsons/TableGrammarAnnotations/Table Grammar Annotations .json"),
                true);
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = parser.getAttributeValuePairs();
        // count outcome attributes
        List<AnnotatedAttributeValuePair> outcomes = annotations.stream().filter(aavp -> aavp.getAttribute().getId().equals("7800771")).collect(Collectors.toList());
        for (AnnotatedAttributeValuePair aavp : annotations) {
            //System.out.println(aavp);
        }
        // find docs with 2 tables (aka docs with 2 arms)
        List<String> docsWithTwoTables = annotations.getDocNames().stream().filter(docName ->
                annotations.byDoc().get(docName).stream().filter(aavp -> aavp.getValue().contains("Table ")).count() >= 2)
                .collect(Collectors.toList());
        for (String docNameWithTwoTables : docsWithTwoTables) {
            System.out.println("Doc: " + docNameWithTwoTables + "========================");
            for (AnnotatedAttributeValuePair aavp : annotations.byDoc().get(docNameWithTwoTables)) {
                System.out.println(aavp);
            }
        }
        System.out.println("Captions: " + annotations.stream().filter(avp -> avp.getAttribute().getId().equals("7800771")).count());
        System.out.println("Docs: " + annotations.getDocNames().size());
        System.out.println("Arms: " + annotations.getAllPairs().stream().map(ArmifiedAttributeValuePair::getArm).distinct().count());
        System.out.println("Outcomes: " + outcomes.size());
        System.out.println("Total annotations: " + annotations.size());
    }

    public static void main(String[] args) throws IOException {
        //mainTableGrammar();
        //countAttributes();
        countAttributesPA();
        //outcomeValuesStats();
    }
}