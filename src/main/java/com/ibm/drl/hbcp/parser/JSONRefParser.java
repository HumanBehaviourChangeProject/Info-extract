/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.parser;


import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringReader;
import java.io.Writer;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.Stack;
import java.util.stream.Collectors;

import javax.json.Json;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.internal.JsonFormatter;

import net.minidev.json.JSONStyle;

enum NAME_ID_ENUM {
    NONE,
    NAME,
    ID
};

enum TREE_INFO_ENUM {
    NONE,
    CODESET_ONLY,
    CODESET_AND_PERDOC_INFO,
    CODESET_AND_PERDOC_INFO_IN_CSV,
    CODESET_AND_PERDOC_ARM_INFO
};

/**
 * Parser for JSON file that handles both the 'code set' hierarchy for modeling 
 * attributes (see {@link CodeSetTree}), and the references or instances of those
 * attributes which are found in documents (see {@link PerDocRefs}, which are members
 * of a {@link CodeSetTreeNode}).  
 * <br>
 * Typical usage involves calling constructor and then {@link #buildAll()}.
 * 
 * @author dganguly
 */
public class JSONRefParser {
    Properties prop;
    String baseDir;
    String fileName;
    ReadContext ctx;
    
    CodeSetTree[] trees;

    private AttributeCache attributes = null;
    // attributes indexed

    static Logger logger = LoggerFactory.getLogger(JSONRefParser.class);
    
    public static Set<String> documents = new HashSet<>();
    
    public JSONRefParser(String propFile) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        this.trees = new CodeSetTree[AttributeType.values().length];
        
        init(prop);
    }
    
    public JSONRefParser(Properties prop) {
        this.trees = new CodeSetTree[AttributeType.values().length];
        init(prop);
    }
    
    public JSONRefParser() {
        this.trees = new CodeSetTree[AttributeType.values().length];
    }

    /**
     * Get the JSON filename (rooted in the base directory) with AST extension.
     * 
     * @return filename and path rooted in base directory
     */
    String getASTFileName() {
        return baseDir + "/" + prop.getProperty("ref.json") + ".ast";
    }
    
    /**
     * Saves the parsed reference tree in secondary memory
     * @throws IOException
     */
    public void save() throws IOException {
        String astFileName = getASTFileName();
        FileOutputStream fos = new FileOutputStream(astFileName);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(this);
        out.close();
        fos.close();
    }
    
    /**
     *  Loads the saved parse tree from secondary memory. Have to pass
     *  the file name from the JSP page.
     *
     * @param astFileName
     * @return instance of a JSONRefParser loaded from file 
     * @throws ClassNotFoundException 
     * @throws IOException
     */
    static public JSONRefParser load(String astFileName) throws IOException, ClassNotFoundException {
        JSONRefParser refObj = null;
        
        FileInputStream fileIn = new FileInputStream(astFileName);
        ObjectInputStream in = new ObjectInputStream(fileIn);
        refObj = (JSONRefParser) in.readObject();
        
        in.close();
        fileIn.close();                
        
        return refObj;
    }
    
    public void saveJSONs() throws Exception {
        String resourceBase = JSONRefParser.class.getClassLoader().getResource("").getPath();
        for (AttributeType type : AttributeType.values()) {
            String fileName = resourceBase + "/codeset." + type.code() + ".json";
            FileWriter fw = new FileWriter(fileName);
            System.out.println("Saving JSON " + fileName);
            fw.write(this.trees[type.code()].json);
            fw.close();
        }
    }

    /**
     * Get JSON-formatted string of code set tree for given attribute type (e.g., population, BCT, outcome)
     * 
     * @param code for attribute type
     * @return JSON-formatted string
     */
    public String getJSON(int code) {
        return trees[code].getJSON();
    }
    
    private void init(Properties prop) {
        this.baseDir = BaseDirInfo.getBaseDir();
        this.fileName = prop.getProperty("ref.json");        
    }

    protected void parseURL(URL url) throws IOException {
        File jsonFile = new File("ctree.json");
        FileUtils.copyURLToFile(url, jsonFile);
        parseFile(jsonFile);
        jsonFile.delete();
    }
    
    protected void parseFile(File jsonFile) throws IOException {
        String json = FileUtils.readFileToString(jsonFile, StandardCharsets.UTF_8);
        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL).addOptions(Option.ALWAYS_RETURN_LIST);
        ctx = JsonPath.using(conf).parse(json);
    }
    
    /**
     * Parse the JSON file specified in properties file.
     * {@link #init(Properties)} must be called first to get the filename.
     * 
     * @throws IOException
     */
    public void parse() throws IOException {
        String filePath = BaseDirInfo.getBaseDir();
        File jsonFile = new File(filePath.concat(fileName));
        parseFile(jsonFile);
    }
    
    /**
     * Removes the special characters from a string, e.g. the patterns
     * "\n", "[¬s]" from the JSON values.
     * @param x the input string
     * @return an output string with the special characters removed
     */
    public static String removeSpecialCharacters(String x) {
        x = x.replaceAll("\\n", "");
        x = x.replaceAll("\\r", " ");
        x = x.replaceAll("Page [0-9]*:", "");
        x = x.replaceAll("\\[¬s\\]", "");
        x = x.replaceAll("\\[¬e\\]", "");
        x = x.replaceAll("\"", "");
        x = x.replace("·", "."); // the dot character is not a simple ASCII one in the JSON file
        x = x.replace("\\- ", ""); // the dot character is not a simple ASCII one in the JSON file
        x = x.trim();
        return x;        
    }
    
    private CodeSetTreeNode multipop(Stack<CodeSetTreeNode> nodeStack) {
        if (nodeStack.isEmpty())
            return null;
        CodeSetTreeNode node = null;
        do {
            node = nodeStack.pop();
        }
        while (node.attribute == null && !node.isRoot);
        return node;
    }

    /**
     * Runs {@link #loadCodeSet(int)} for all code set (e.g., population, intervent, outcome, etc.)
     * 
     * @throws IOException
     */
    public void loadCodeSets() throws IOException {
        this.trees[AttributeType.POPULATION.code()] = loadCodeSet(AttributeType.POPULATION.code());
        this.trees[AttributeType.INTERVENTION.code()] = loadCodeSet(AttributeType.INTERVENTION.code());
        this.trees[AttributeType.OUTCOME.code()] = loadCodeSet(AttributeType.OUTCOME.code());
        this.trees[AttributeType.OUTCOME_VALUE.code()] = loadCodeSet(AttributeType.OUTCOME_VALUE.code());
        this.trees[AttributeType.EFFECT.code()] = loadCodeSet(AttributeType.EFFECT.code());
    }
    
    /**
     * Uses JSON parser to populate {@link CodeSetTree} structure for given attribute type
     * and returns this data structure. 
     * 
     * @param code the indicates the attribute type
     * @return populated {@link CodeSetTree} from JSON parse
     * @throws IOException
     */
    public CodeSetTree loadCodeSet(int code) throws IOException {
        
        CodeSetTree tree;
        CodeSetTreeNode root;
        String codeToRead = "$.CodeSets[" + code + "].Attributes";
        String codesets = ctx.read(codeToRead).toString();
        logger.debug(codesets);
        
        Stack<CodeSetTreeNode> nodeStack = new Stack<>();
        
//        root = new CodeSetTreeNode(CodeSetTreeNames[code]);
        root = new CodeSetTreeNode();
        root.setName(AttributeType.values()[code].getName());
        root.isRoot = true;
        nodeStack.push(root);
        
        List<CodeSetTreeNode> children = null;
        JsonParser parser = Json.createParser(new StringReader(codesets));
        NAME_ID_ENUM nameOrId = NAME_ID_ENUM.NONE;

        // We are only interested in the events of:
        // 1. reaching a leaf node, i.e. with an array and an object open - current object part of an array
        // 2. read the keys 'AttributeName' and 'AttributeId'
        while (parser.hasNext()) {
            Event e = parser.next();
            
            if (e == Event.START_OBJECT) {
                CodeSetTreeNode top = nodeStack.peek();
                if (children==null && top.children!=null)
                    children = top.children;
                
                CodeSetTreeNode currentAttrib = new CodeSetTreeNode();
                nodeStack.push(currentAttrib);
            }
            else if (e == Event.END_OBJECT) {
                CodeSetTreeNode a = nodeStack.peek();
                if (children != null && a.attribute.getName()!=null) {
                    children.add(a);
                    nodeStack.pop();
                }
            }
            else if (e == Event.END_ARRAY) {
                CodeSetTreeNode parent = multipop(nodeStack);
                
                if (parent != null && children != null) {
                    logger.debug("Collected children of node " + parent.getName());
                    
                    if (parent.children==null)
                        parent.children = children; // associate the collected list to the parent
                }
                
                if (!parent.isRoot) {
                    // set grand-parent child to this
                    CodeSetTreeNode gparent = multipop(nodeStack); // get grand-parent
                    if (gparent.children == null)
                        gparent.children = new ArrayList<>();
                    gparent.children.add(parent);
                    
                    nodeStack.push(gparent);                    
                }
                else { // if root
                    nodeStack.push(parent); // push-back root                    
                }
                
                // Don't begin collecting unless you see 'AttributesList'
                children = null;                
            }
            else if (e == Event.KEY_NAME) {                
                String keyname = parser.getString();
                
                if (keyname.equals("AttributeName")) {
                    nameOrId = NAME_ID_ENUM.NAME;
                }
                else if (keyname.equals("AttributeId")) {
                    nameOrId = NAME_ID_ENUM.ID;
                }
                else if (keyname.equals("AttributesList")) {
                    children = new ArrayList<>();
                }
                else
                    nameOrId = NAME_ID_ENUM.NONE;
            }
            else if (e == Event.VALUE_STRING) {
                CodeSetTreeNode a = nodeStack.peek();
                String value = parser.getString();
                if (nameOrId == NAME_ID_ENUM.NAME) {
                    a.setName(value);
                    logger.debug("set node name to " + value);
                    a.setAttribute(code);
                }
            }
            else if (e == Event.VALUE_NUMBER) {
                CodeSetTreeNode a = nodeStack.peek();
                String value = parser.getString();
                if (nameOrId == NAME_ID_ENUM.ID) {
                    a.setId(value);
                    a.setAttribute(code);
                }
            }
        }
        
        tree = new CodeSetTree(root);
        
        // For debugging only
        if (logger.isDebugEnabled())
            tree.traverse(TREE_INFO_ENUM.CODESET_ONLY, null);
        
        return tree;
    }
    
    /**
     * Call {@link #groupByDocs(CodeSetTree)} for all attribute types
     * 
     * @throws IOException
     */
    public void groupByDocs() throws IOException {
        for (AttributeType type : AttributeType.values()) {
            groupByDocs(trees[type.code()]);
        }
    }
    
    public CodeSetTree getGroundTruths(int code) {
        return trees[code];
    }
    
    /**
     * Calls methods to parse JSON, populate {@link CodeSetTree}, and add annotated attribute references
     * per document per attribute (see {@link CodeSetTreeNode}).  
     * 
     * @throws IOException
     */
    public void buildAll() throws IOException {
        parse();
        loadCodeSets();
        groupByDocs();
        attributes = getAttributeCache();
        //showParseTrees();
    }

    /**
     * Calls methods to parse JSON from URL and populate {@link CodeSetTree}.
     * 
     * To be called from the web server.
     * @param url
     * @throws IOException
     */
    public void buildCodeSetsFromURL(URL url) throws IOException {
        parseURL(url);
        loadCodeSets();
        
        trees[AttributeType.POPULATION.code()].generateJSTreeStructuredJSON();
        trees[AttributeType.INTERVENTION.code()].generateJSTreeStructuredJSON();
        trees[AttributeType.OUTCOME.code()].generateJSTreeStructuredJSON();
        trees[AttributeType.EFFECT.code()].generateJSTreeStructuredJSON();
    }
    
    private void convertCode1Annotation2Code2 () throws Exception{
        String filePath = BaseDirInfo.getBaseDir();
        File jsonFile1 = new File(filePath.concat("data/jsons/Sprint1_Codeset1.json"));
        String json1 = FileUtils.readFileToString(jsonFile1, StandardCharsets.UTF_8);
        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL).addOptions(Option.ALWAYS_RETURN_LIST);
        ReadContext ctx1 = JsonPath.using(conf).parse(json1);

        File jsonFile2 = new File(filePath.concat("data/jsons/Sprint2_Codeset2_23Jan18.json"));
        String json2 = FileUtils.readFileToString(jsonFile2, StandardCharsets.UTF_8);
        ReadContext ctx2 = JsonPath.using(conf).parse(json2);

        
        
        //read mapping and extract doc annotation from codeset1, map them to codeset2 
        Map<String, String> codemap = new HashMap<>();
        BufferedReader br = new BufferedReader(new FileReader(filePath.concat("data/migrateAnnotationFromCodeset1toCodeset2_v0.1")));
        String line;
        while ((line = br.readLine()) != null) {     
            if(line.contains("action")) continue;
            codemap.put(line.split(",")[0].trim(), line.split(",")[1].trim());
        }
        br.close();
        net.minidev.json.JSONArray changedDocRef = new net.minidev.json.JSONArray(); 
        net.minidev.json.JSONArray doccodes = ctx1.read("$.References[*]");        
        for(Iterator iter = doccodes.iterator(); iter.hasNext();){
            LinkedHashMap doc = (LinkedHashMap)iter.next();
           // net.minidev.json.JSONObject docclone = new net.minidev.json.JSONObject(doc);
            String docItemId = doc.get("ItemId").toString();
            net.minidev.json.JSONArray codes = (net.minidev.json.JSONArray)doc.get("Codes");
            List<LinkedHashMap> toRemove = new ArrayList<>();
            for(Iterator iter1 = codes.iterator(); iter1.hasNext();){
                 LinkedHashMap child = (LinkedHashMap)iter1.next();            
                 String attribId = child.get("AttributeId").toString();
                 if(codemap.containsKey(attribId)){
                     if(codemap.get(attribId).toLowerCase().matches("null|ignore")){
                         toRemove.add(child);
                     }else{
                        // System.err.println(codemap.get(attribId));
                         child.put("AttributeId", Integer.valueOf(codemap.get(attribId)));
                     }
                 }
               //female/male, percentage female/male
                 if(attribId.equalsIgnoreCase("3587701")||attribId.equalsIgnoreCase("3587702")){
                     child.put("AttributeId", Integer.valueOf("4269096"));
                 }
                 if(attribId.equalsIgnoreCase("3602585")||attribId.equalsIgnoreCase("3602584")){ 
                     child.put("AttributeId", Integer.valueOf("4269096"));
                 }
               //for four papers from codeset1 about women, change gender to 4266589 all female
                 if(attribId.equalsIgnoreCase("3587702")&&docItemId.matches(("27451059|28856439|27444324|27444340"))){
                     child.put("AttributeId", Integer.valueOf("4266589"));
                 }
            }
            
            //remove
            for(LinkedHashMap remove: toRemove){
                codes.remove(remove);
            }
           logger.debug(JsonFormatter.prettyPrint(doc.toString()));
           changedDocRef.add(doc);
        }
        //merge changedDocRef from codeset1 to codeset2 annotations
        net.minidev.json.JSONObject obj = new net.minidev.json.JSONObject();
        net.minidev.json.JSONArray obj1 = ctx2.read("$.CodeSets[*]");
        net.minidev.json.JSONArray obj2 = ctx2.read("$.References[*]");
        obj2.addAll(changedDocRef);
        
        obj.put("CodeSets", obj1);
        obj.put("References", obj2);
        Writer writer = new FileWriter(filePath.concat("data/jsons/Sprint2_Codeset2_Codeset1_merge.json"));
        writer.write(obj.toJSONString(JSONStyle.NO_COMPRESS));
        writer.close();

    } 
    
    /**
     * Create mapping of sprint number to set of document names
     * 
     * @return map of sprint numbers to set of document names
     * @throws IOException
     */
    public Map<String, Set<String>> docsBySprint() throws IOException {
        Map<String, Set<String>> docsbysprint = new HashMap<>();
        net.minidev.json.JSONArray docs = ctx.read("$.References[*]");        
        for (Iterator iter = docs.iterator(); iter.hasNext(); ) {
            LinkedHashMap child = (LinkedHashMap)iter.next(); 
            if(child.get("DocTitle")==null) continue;
            String doctitle =  (child.get("DocTitle")).toString();
            String sprintno = (child.get("SprintNo")).toString();
            if(docsbysprint.containsKey(sprintno)){
                docsbysprint.get(sprintno).add(doctitle);
            } else {
                Set<String> documents = new HashSet<>();
                documents.add(doctitle);
                docsbysprint.put(sprintno, documents);
            }
        } 
        return docsbysprint;
    } 
    
    /**
     * Add annotated attribute references (per document) to {@link CodeSetTreeNode} (which represent an attribute in the 
     * {@link CodeSetTree} structure).
     * 
     * @param tree
     * @throws IOException
     */
    public void groupByDocs(CodeSetTree tree) throws IOException {

        net.minidev.json.JSONArray codes = ctx.read("$.References..Codes[*]");        
        
        for (Iterator iter = codes.iterator(); iter.hasNext(); ) {
            
            LinkedHashMap child = (LinkedHashMap)iter.next();    
//            String attribId = ((Integer)(child.get("AttributeId"))).toString();
            String attribId = (child.get("AttributeId")).toString();
            
            net.minidev.json.JSONArray itemAttribTextDetails =
                    (net.minidev.json.JSONArray)child.get("ItemAttributeFullTextDetails");
            if (itemAttribTextDetails==null || itemAttribTextDetails.isEmpty())
                continue;
            
            //logger.debug("attrib-id = " + attribId);
            CodeSetTreeNode node = tree.getNode(attribId);
            if (node == null) {
//                logger.trace("No node found for attribute id: " + attribId);
                continue; // attribute must be in another CodeSetTree
            }

            LinkedHashMap itemObjMap = (LinkedHashMap)itemAttribTextDetails.get(0);
            
            String docName = (String)itemObjMap.get("DocTitle");
    
            String context = removeSpecialCharacters((String)child.get("AdditionalText"));
            String highlightedText = removeSpecialCharacters((String)itemObjMap.get("Text"));
            String pageNumber=((String)itemObjMap.get("Text")).split(":")[0];
            int annotationPage = Integer.parseInt(pageNumber.replace("Page ",""));
            String sprintNo = (String)child.get("SprintNo");
            String arm = "default";
            // value is highlighted text (i.e., not normalized yet)
            AnnotatedAttributeValuePair attrib = new AnnotatedAttributeValuePair(node.getAttribute(), highlightedText, docName, arm, context, highlightedText, sprintNo, annotationPage);

            node.addPerDocRcd(docName, attrib);
        }
    }
    
    private void checkFileNamesBetweenJsonAndPdf() {
        logger.debug("size:" + documents.size());
        String pdfPath = BaseDirInfo.getBaseDir().concat("data/pdfs_Sprint1234");

        File folder1 = new File(pdfPath);
        int count = 0;
        Set<String> pdfs = new HashSet<>();
        for (File s : folder1.listFiles()) {
            String fileName = s.getName();
            pdfs.add(fileName);
            if (!documents.contains(fileName)) {
                logger.debug(fileName);
                count++;
            }
        }
        logger.debug("pdf files which are not in json file:" + count);

        int count1 = 0;
        for (String s : documents) {
            if (!pdfs.contains(s)) {
                logger.debug(s);
                count1++;
            }
        }
        logger.debug("json doc titles which are not in pdf files:" + count1);
    }
    
    /**
     * Call {@link #showParseTree(CodeSetTree)} for all attribute types.
     * Collect list of all document names.
     * 
     * @throws IOException
     */
    void showParseTrees() {
        // TODO collecting all documents in a set might be split off from here
        // TODO we might need to revisit what all the 'showParseTree*' methods are doing
        showParseTree(trees[AttributeType.POPULATION.code()]);
        documents.addAll(trees[AttributeType.POPULATION.code()].docs);
        showParseTree(trees[AttributeType.INTERVENTION.code()]);
        documents.addAll(trees[AttributeType.INTERVENTION.code()].docs);
        showParseTree(trees[AttributeType.OUTCOME.code()]);
        documents.addAll(trees[AttributeType.OUTCOME.code()].docs);
        showParseTree(trees[AttributeType.EFFECT.code()]);
        documents.addAll(trees[AttributeType.EFFECT.code()].docs);
    }
    
    /**
     * Write parsed {@link CodeSetTree} hierarchy to CSV file for all attribute types.
     * Trees must be populated first with {@link #buildAll()} or similar.
     * 
     * @throws IOException
     */
    void showParseTreesInCSV() throws IOException {
        FileWriter fw = new FileWriter(baseDir + "/" + prop.getProperty("ref.json") + ".csv");
        BufferedWriter bw = new BufferedWriter(fw);
        
        showParseTreeInCSV(trees[AttributeType.POPULATION.code()], bw);
        showParseTreeInCSV(trees[AttributeType.INTERVENTION.code()], bw);
        showParseTreeInCSV(trees[AttributeType.OUTCOME.code()], bw);
        showParseTreeInCSV(trees[AttributeType.EFFECT.code()], bw);
        
        bw.close();
        fw.close();
    }
    
    /**
     * Print parsed {@link CodeSetTree} hierarchy to logger output.
     * 
     * @param tree
     */
    void showParseTree(CodeSetTree tree) {
        logger.info("Codeset tree obtained after parsing JSON...");
        tree.traverse(TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO, null);
    }

    /**
     * Write parsed {@link CodeSetTree} hierarchy to CSV file for given attribute type.
     * Trees must be populated first with {@link #buildAll()} or similar.
     * 
     * @throws IOException
     */
    void showParseTreeInCSV(CodeSetTree tree, BufferedWriter bw) {
        logger.info("Codeset tree flattened in CSV...");
        tree.traverse(TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO_IN_CSV, bw);
    }

    /**
     * Returns a vector of all the attributes present in a document.
     * @param docName The filename of the document
     * @return
     */
   /* public AttributeVec getAttributesInDoc(String docName) {
        AttributeVec res = new AttributeVec(-1);
        for (CodeSetTree tree : trees) {
            for (CodeSetTreeNode treeNode : tree.cache.values()) {
                AnnotatedAttributeValuePair attribute = treeNode.getDocRecordMap().getRefs().get(docName);
                if (attribute != null)
                    res.addAttrib(attribute);
            }
        }
        return res;
    }
*/
    /**
     * 
     * @return mapping of attribute ids to attribute objects
     */
    protected AttributeCache getAttributeCache() {
        return new AttributeCache(getAttributeValuePairs().getAllPairs().stream()
                .map(AttributeValuePair::getAttribute).collect(Collectors.toList()));
    }

    /**
     * @return mapping of attribute ids to attribute objects
     */
    public AttributeCache getAttributes() { return attributes; }

    public CodeSetTree[] getTrees() { return trees; }

    /** Returns all the attribute-value pairs parsed. */
    public AttributeValueCollection<AnnotatedAttributeValuePair> getAttributeValuePairs() {
        List<AnnotatedAttributeValuePair> pairs = new ArrayList<>();
        // retrieve all the pairs in the Json com.ibm.drl.hbcp.parser's trees
        for (AttributeType type : AttributeType.values()) {
            int i = type.code();
            CodeSetTree tree = trees[i];
            for (String attributeId : tree.cacheArms.keySet()) {
                List<CodeSetTreeNode> armifiedNodes = tree.cacheArms.get(attributeId);
                for (CodeSetTreeNode node : armifiedNodes) {
                    // this should be an instance with a value
                    Collection<AnnotatedAttributeValuePair> avPairs = node.getDocRecordMap().getRefs().values();
                    pairs.addAll(avPairs);
                }
            }
        }
        // build the collection
        return new AttributeValueCollection<>(pairs);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.err.println("Usage: java JSONRefParser <prop-file>");
            args[0] = "init.properties";
        }
        
        try {
            JSONRefParser jsonrefParser = new JSONRefParser(args[0]);
            //jsonrefParser.convertCode1Annotation2Code2();
            jsonrefParser.buildAll();
//            jsonrefParser.showParseTreesInCSV();
            jsonrefParser.showParseTrees();
            //jsonrefParser.checkFileNamesBetweenJsonAndPdf();
        
        }
        catch (Exception ex) { ex.printStackTrace(); }
        
    }
}
