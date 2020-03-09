package com.ibm.drl.hbcp.parser;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.inforetrieval.apr.AttributeVec;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.parser.cleaning.typing.LineConsistencyChecker;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonAnnotationFile;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonCode;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonItemAttributeFullTextDetail;
import com.ibm.drl.hbcp.parser.jsonstructure.JsonReference;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
import lombok.Data;
import org.apache.commons.lang3.NotImplementedException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.*;
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
    private final Attributes attributes;
    // arms indexed by their id
    private final Map<Integer, Arm> arms;
    private final boolean isArmified;
    // attribute instances (attribute value pairs)
    protected final AttributeValueCollection<AnnotatedAttributeValuePair> instances;
    //private final Map<AttributeType, CodeSetTree> trees;
    protected final CodeSetTree[] trees;

    // useful but less essential information
    private final Map<String, PdfInfo> docNameToPdfInfo;
    private final Map<String, PdfInfo> shortTitleToPdfInfo;

    private static final String ARM_ATTRIBUTE_NAME = "Arm name";
    private static final List<String> CONTEXT_SEPARATORS = Lists.newArrayList(" ; ", " ;;; ");
    private static Logger logger = LoggerFactory.getLogger(JSONRefParser.class);

    /**
     * Parses a JSON file containing Behaviour Change annotations.
     *
     * @param jsonFile a JSON file
     * @throws IOException occurs if the file didn't match the expected JSON structure, or otherwise in other traditional I/O-related cases
     */
    public JSONRefParser(File jsonFile) throws IOException {
        jsonFile = FileUtils.potentiallyGetAsResource(jsonFile);
        this.jsonFile = jsonFile;

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

    private Map<Integer, Arm> getArms(JsonAnnotationFile json) {
        Map<Integer, Arm> res = new HashMap<>();
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
        return res;
    }

    private List<AnnotatedAttributeValuePair> getInstances(JsonAnnotationFile json) {
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        for (JsonReference reference : json.getReferences()) { // a reference is an annotated document, basically
            if (reference.getCodes() != null) { // null can happen when no annotation has been done on the document
                for (JsonCode code : reference.getCodes()) {
                    // TODO: some codes are completely empty for some reason, with attribute ID: 3689776
                    if (code.getItemAttributeFullTextDetails() != null)
                        res.addAll(getAttributeValuePairs(code));
                }
            }
        }
        return res;
    }

    private List<AnnotatedAttributeValuePair> getAttributeValuePairs(JsonCode code) {
        // analyze the context first (can contain multiple individual contexts for different values)
        String sharedContext = removeSpecialCharacters(code.getAdditionalText());
        List<String> splitContexts = ParsingUtils.split(sharedContext, CONTEXT_SEPARATORS);
        if (splitContexts.size() != code.getItemAttributeFullTextDetails().length) {
            // we couldn't match the contexts, keep the whole shared one, but deal with the multiple JsonItemAttributeFullTextDetail
            return Arrays.stream(code.getItemAttributeFullTextDetails())
                    .flatMap(fullTextDetail -> getAttributeValuePairs(fullTextDetail, code, sharedContext).stream())
                    .collect(Collectors.toList());
        } else {
            // we have a 1-for-1 match between contexts and itemAttributeFullTextDetails
            List<AnnotatedAttributeValuePair> res = new ArrayList<>();
            for (int i = 0; i < splitContexts.size(); i++) {
                JsonItemAttributeFullTextDetail fullTextDetail = code.getItemAttributeFullTextDetails()[i];
                res.addAll(getAttributeValuePairs(fullTextDetail, code, splitContexts.get(i)));
            }
            return res;
        }
    }

    private List<AnnotatedAttributeValuePair> getAttributeValuePairs(JsonItemAttributeFullTextDetail detail, JsonCode code, String context) {
        String docName = detail.getDocTitle();
        String highlightedText = removeSpecialCharacters(detail.getText());
        String pageNumber = detail.getText().split(":")[0];
        int annotationPage;
        try {
            annotationPage = Integer.parseInt(pageNumber.replace("Page ", "")) - 1;
            if (annotationPage < 0) throw new NumberFormatException("Page was already 0 in the annotations, that's weird.");
        } catch (NumberFormatException e) {
            logger.warn("No page annotation for entity of paper: " + docName);
            annotationPage = 0;
        }
        String sprintNo = code.getSprintNo() != null ? code.getSprintNo() : "";
        if (isArmified)
            sprintNo = "Sprint5";
        int armId = isArmified ? code.getArmId() : 0;
        Arm arm = arms.get(armId);
        if (arm == null) {
            // this means the arm is implicit in the document (not annotated/declared), we add it on-the-fly
            // this is an expected behavior
            arm = new Arm(String.valueOf(code.getArmId()), code.getArmTitle());
            logger.debug("Arm " + code.getArmId() + " was implicit (not declared/annotated).");
            arms.put(code.getArmId(), arm);
        }
        Attribute attribute;
        attribute = attributes.getFromId(String.valueOf(code.getAttributeId()));
        if (attribute == null) {
            // this happens in rare cases where a codeset node was deleted but children nodes (detached) remain
            // and so do their annotations (see internal HBCP Slack discussion of 24/04/2019)
            logger.debug("Attribute was NOT DEFINED in the CodeSets: " + code.getAttributeId() + ". Ignored.");
            return Lists.newArrayList();
        }
        // here split the highlighted text using the "\n" separators
        String[] values = highlightedText.split("\\n");
        final Arm finalArm = arm;
        final String finalSprintNo = sprintNo;
        final int finalAnnotationPage = annotationPage;
        if (shouldCreateSeveralValues(attribute, values)) {
            return Arrays.stream(values)
                    .map(value -> new AnnotatedAttributeValuePair(attribute, value, docName, finalArm, context, value, finalSprintNo, finalAnnotationPage))
                    .collect(Collectors.toList());
        } else {
            return Lists.newArrayList(
                    new AnnotatedAttributeValuePair(attribute, highlightedText, docName, finalArm, context, highlightedText, finalSprintNo, finalAnnotationPage)
            );
        }

    }

    private boolean shouldCreateSeveralValues(Attribute attribute, String[] values) {
        return attribute.getType() != AttributeType.ARM && (
                attribute.getType() == AttributeType.INTERVENTION
                || LineConsistencyChecker.areLinesHomogeneous(values)
        );
    }

    private Arm getArm(JsonCode code) {
        List<String> allNames = Arrays.stream(code.getItemAttributeFullTextDetails())
                .map(JsonItemAttributeFullTextDetail::getText)
                .flatMap(text -> parseArmNames(text).stream())
                .collect(Collectors.toList());
        return new Arm(String.valueOf(code.getArmId()), removeSpecialCharacters(code.getArmTitle()), allNames);
    }

    private List<String> parseArmNames(String text) {
        List<String> res = new ArrayList<>();
        for (String str : text.split("\n")) {
            // TODO: deal with hyphens occurring in line-breaks like in "inter- vention"
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
                    List<AnnotatedAttributeValuePair> avp = getAttributeValuePairs(code);
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
        x = x.replaceAll("(?<![ \\t])- ", "");
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
    public AttributeVec getAttributesInDoc(String docName) {
        AttributeVec res = new AttributeVec(-1);
        for (CodeSetTree tree : trees) {
            if (tree != null) {
                for (CodeSetTreeNode treeNode : tree.cache.values()) {
                    AnnotatedAttributeValuePair attribute = treeNode.getDocRecordMap().getRefs().get(docName);
                    if (attribute != null)
                        res.addAttrib(attribute);
                }
            }
        }
        return res;
    }

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

    public static void main(String[] args) throws IOException {
        // docnames in annotation
        JSONRefParser parserPrevious = new JSONRefParser(new File("data/jsons/SmokingPapers_All_Attributes.json"));
        JSONRefParser parserNew = new JSONRefParser(new File("data/NewSmokingPapers_13Nov19.json"));
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = AttributeValueCollection.union(
                parserPrevious.getAttributeValuePairs(),
                parserNew.getAttributeValuePairs()
        );
        System.out.println(annotations.size());
    }
}