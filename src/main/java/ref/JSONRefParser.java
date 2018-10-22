/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ref;


import com.jayway.jsonpath.Configuration;
import com.jayway.jsonpath.JsonPath;
import com.jayway.jsonpath.Option;
import com.jayway.jsonpath.ReadContext;
import com.jayway.jsonpath.internal.JsonFormatter;
import indexer.BaseDirInfo;
import java.io.*;
import java.net.URL;
import java.nio.charset.Charset;
import java.util.*;
import org.apache.commons.io.FileUtils;
import javax.json.*;
import javax.json.stream.JsonParser;
import javax.json.stream.JsonParser.Event;
import net.minidev.json.JSONStyle;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * 
 * @author dganguly
 */

enum NAME_ID_ENUM {
    NONE,
    NAME,
    ID
};

enum TREE_INFO_ENUM {
    NONE,
    CODESET_ONLY,
    CODESET_AND_PERDOC_INFO,
    CODESET_AND_PERDOC_INFO_IN_CSV
};

public class JSONRefParser {
    Properties prop;
    String baseDir;
    String fileName;
    ReadContext ctx;
    
    CodeSetTree[] trees;
    
    static Logger logger = LoggerFactory.getLogger(JSONRefParser.class);
    
    public static final int POPULATION = 0;
    public static final int INTERVENTION = 1;
    public static final int OUTCOME = 2;
    public static final int OUTCOME_VALUE = 3;
    public static final int EFFECT = 4;
    
    public static Set<String> documents = new HashSet();
    
    static final String[] CodeSetTreeNames = { "Population",  "Intervention", "Outcome", "Outcome_value", "Effect" };
    
    public JSONRefParser(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        this.trees = new CodeSetTree[CodeSetTreeNames.length];        
        
        init(prop);
    }
    
    public JSONRefParser(Properties prop) {
        this.trees = new CodeSetTree[CodeSetTreeNames.length];        
        init(prop);
    }
    
    public JSONRefParser() {
        this.trees = new CodeSetTree[CodeSetTreeNames.length];        
    }

    String getASTFileName() {
        return baseDir + "/" + prop.getProperty("ref.json") + ".ast";
    }
    
    // Saves the parsed reference tree in secondary memory
    public void save() throws Exception {
        String astFileName = getASTFileName();
        FileOutputStream fos = new FileOutputStream(astFileName);
        ObjectOutputStream out = new ObjectOutputStream(fos);
        out.writeObject(this);
        out.close();
        fos.close();
    }
    
    /**
     * Loads the saved parse tree from secondary memory. Have to pass
     * the file name from the JSP page.
     * @param astFileName
     * @return
     * @throws Exception 
     */
    static public JSONRefParser load(String astFileName) throws Exception {
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
        for (int i=JSONRefParser.POPULATION; i<=JSONRefParser.EFFECT; i++) {
            String fileName = resourceBase + "/codeset." + i + ".json";
            FileWriter fw = new FileWriter(fileName);
            System.out.println("Saving JSON " + fileName);
            fw.write(this.trees[i].json);
            fw.close();
        }
    }
    
    public String getJSON(int code) {
        return trees[code].getJSON();
    }
    
    private void init(Properties prop) {
        this.baseDir = BaseDirInfo.getBaseDir();
        this.fileName = prop.getProperty("ref.json");        
    }

    void parseURL(URL url) throws Exception {
        File jsonFile = new File("ctree.json");
        FileUtils.copyURLToFile(url, jsonFile);
        parseFile(jsonFile);
        jsonFile.delete();
    }
    
    void parseFile(File jsonFile) throws Exception {
        String json = FileUtils.readFileToString(jsonFile, Charset.defaultCharset());
        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL).addOptions(Option.ALWAYS_RETURN_LIST);
        ctx = JsonPath.using(conf).parse(json);
    }
    
    public void parse() throws Exception {
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
    
    Attribute top(Stack<Attribute> nodeStack) {
        return nodeStack.empty()? null : nodeStack.peek();
    }

    CodeSetTreeNode multipop(Stack<CodeSetTreeNode> nodeStack) {
        if (nodeStack.isEmpty())
            return null;
        CodeSetTreeNode a = null;
        do {
            a = nodeStack.pop();
        }
        while (a.a.name == null);
        return a;
    }

    public void loadCodeSets() throws Exception {
        this.trees[POPULATION] = loadCodeSet(POPULATION);
        this.trees[INTERVENTION] = loadCodeSet(INTERVENTION);
        this.trees[OUTCOME] = loadCodeSet(OUTCOME);
        this.trees[OUTCOME_VALUE] = loadCodeSet(OUTCOME_VALUE);
        this.trees[EFFECT] = loadCodeSet(EFFECT);
    }
    
    public CodeSetTree loadCodeSet(int code) throws Exception {
        
        CodeSetTree tree;
        CodeSetTreeNode root;
        String codeToRead = "$.CodeSets[" + code + "].Attributes";
        String codesets = ctx.read(codeToRead).toString();
        logger.debug(codesets);
        
        Stack<CodeSetTreeNode> nodeStack = new Stack<>();
        
        root = new CodeSetTreeNode(CodeSetTreeNames[code]);
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
                if (children != null && a.a.name!=null) {
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
                }
            }
            else if (e == Event.VALUE_NUMBER) {
                CodeSetTreeNode a = nodeStack.peek();
                String value = parser.getString();
                if (nameOrId == NAME_ID_ENUM.ID)
                    a.setId(value);
            }
        }
        
        tree = new CodeSetTree(root);
        
        // For debugging only
        if (logger.isDebugEnabled())
            tree.traverse(TREE_INFO_ENUM.CODESET_ONLY, null);
        
        return tree;
    }
    
    public void groupByDocs() throws Exception {
        groupByDocs(trees[POPULATION]);
        groupByDocs(trees[INTERVENTION]);
        groupByDocs(trees[OUTCOME]);    
        groupByDocs(trees[OUTCOME_VALUE]);
        groupByDocs(trees[EFFECT]);        
    }
    
    public CodeSetTree getGroundTruths(int code) {
        return trees[code];
    }
    
    public void buildAll() throws Exception {
        parse();
        loadCodeSets();
        groupByDocs();
        //showParseTrees();
    }

    // To be called from the web server.
    public void buildCodeSetsFromURL(URL url) throws Exception {
        parseURL(url);
        loadCodeSets();
        
        trees[POPULATION].generateJSTreeStructuredJSON();
        trees[INTERVENTION].generateJSTreeStructuredJSON();
        trees[OUTCOME].generateJSTreeStructuredJSON();
        trees[EFFECT].generateJSTreeStructuredJSON();
    }
    
    public void convertCode1Annotation2Code2 () throws Exception{
        String filePath = BaseDirInfo.getBaseDir();
        File jsonFile1 = new File(filePath.concat("data/jsons/Sprint1_Codeset1.json"));
        String json1 = FileUtils.readFileToString(jsonFile1, Charset.defaultCharset());
        Configuration conf = Configuration.defaultConfiguration().addOptions(Option.DEFAULT_PATH_LEAF_TO_NULL).addOptions(Option.ALWAYS_RETURN_LIST);
        ReadContext ctx1 = JsonPath.using(conf).parse(json1);

        File jsonFile2 = new File(filePath.concat("data/jsons/Sprint2_Codeset2_23Jan18.json"));
        String json2 = FileUtils.readFileToString(jsonFile2, Charset.defaultCharset());
        ReadContext ctx2 = JsonPath.using(conf).parse(json2);

        
        
        //read mapping and extract doc annotation from codeset1, map them to codeset2 
        Map<String, String> codemap = new HashMap();
        BufferedReader br = new BufferedReader(new FileReader(filePath.concat("data/migrateAnnotationFromCodeset1toCodeset2_v0.1")));
        String line;
        while ((line = br.readLine()) != null) {     
            if(line.contains("action")) continue;
            codemap.put(line.split(",")[0].trim(), line.split(",")[1].trim());
        }
        net.minidev.json.JSONArray changedDocRef = new net.minidev.json.JSONArray(); 
        net.minidev.json.JSONArray doccodes = ctx1.read("$.References[*]");        
        for(Iterator iter = doccodes.iterator(); iter.hasNext();){
            LinkedHashMap doc = (LinkedHashMap)iter.next();
           // net.minidev.json.JSONObject docclone = new net.minidev.json.JSONObject(doc);
            String docItemId = doc.get("ItemId").toString();
            net.minidev.json.JSONArray codes = (net.minidev.json.JSONArray)doc.get("Codes");
            List<LinkedHashMap> toRemove = new ArrayList();
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
    
    public Map<String, Set<String>> DocsBySprint() throws Exception {
        Map<String, Set<String>> docsbysprint = new HashMap();
        net.minidev.json.JSONArray docs = ctx.read("$.References[*]");        
        for (Iterator iter = docs.iterator(); iter.hasNext(); ) {
            LinkedHashMap child = (LinkedHashMap)iter.next(); 
            if(child.get("DocTitle")==null) continue;
            String doctitle =  (child.get("DocTitle")).toString();
            String sprintno = (child.get("SprintNo")).toString();
            if(docsbysprint.containsKey(sprintno)){
                docsbysprint.get(sprintno).add(doctitle);
            } else {
                Set<String> documents = new HashSet();
                documents.add(doctitle);
                docsbysprint.put(sprintno, documents);
            }
        } 
        return docsbysprint;
    } 
    
    public void groupByDocs(CodeSetTree tree) throws Exception {

        net.minidev.json.JSONArray codes = ctx.read("$.References..Codes[*]");        
        
        for (Iterator iter = codes.iterator(); iter.hasNext(); ) {
            
            LinkedHashMap child = (LinkedHashMap)iter.next();    
//            String attribId = ((Integer)(child.get("AttributeId"))).toString();
            String attribId = (child.get("AttributeId")).toString();
            
            net.minidev.json.JSONArray itemAttribTextDetails =
                    (net.minidev.json.JSONArray)child.get("ItemAttributeFullTextDetails");
            if (itemAttribTextDetails==null || itemAttribTextDetails.isEmpty())
                continue;
            
            LinkedHashMap itemObjMap = (LinkedHashMap)itemAttribTextDetails.get(0);
            
            String docName = (String)itemObjMap.get("DocTitle");
    
            Attribute attrib = new Attribute(String.valueOf(attribId));
            attrib.docTitle = docName;
            attrib.context = removeSpecialCharacters((String)child.get("AdditionalText"));
            attrib.highlightedText = removeSpecialCharacters((String)itemObjMap.get("Text"));
            attrib.sprintNo=(String)child.get("SprintNo");

            //logger.debug("attrib-id = " + attribId);
            CodeSetTreeNode node = tree.getNode(attribId);
            if (node != null)
                node.addPerDocRcd(docName, attrib);
            else
                logger.trace("No node found for attribute id: " + attribId);
        }
    }
    
    void checkFileNamesBetweenJsonAndPdf(){
        logger.debug("size:" + documents.size());
	String pdfPath = BaseDirInfo.getBaseDir().concat("data/pdfs_Sprint1234");
	Set<String> xmlFiles = new HashSet();
	
	File folder1 = new File(pdfPath);
        int count = 0;
        Set<String> pdfs = new HashSet();
	for(File s: folder1.listFiles()){
	    String fileName = s.getName();
            pdfs.add(fileName);
            if(!documents.contains(fileName)){
                logger.debug(fileName);
                count++;
        }else{
               // System.err.println(fileName);
      }
        }
        logger.debug("pdf files which are not in json file:" + count);
        
        int count1 = 0;
        for(String s: documents){
            if(!pdfs.contains(s)){
                logger.debug(s);
                count1++;
            }
        }
        logger.debug("json doc titles which are not in pdf files:" + count1);
    }
    
    void showParseTrees() {
        showParseTree(trees[POPULATION]);
        documents.addAll(trees[POPULATION].docs);
        showParseTree(trees[INTERVENTION]);
        documents.addAll(trees[INTERVENTION].docs);
        showParseTree(trees[OUTCOME]);
        documents.addAll(trees[OUTCOME].docs);
        showParseTree(trees[EFFECT]);
        documents.addAll(trees[EFFECT].docs);
    }

    void showParseTreesInCSV() throws IOException {
        FileWriter fw = new FileWriter(baseDir + "/" + prop.getProperty("ref.json") + ".csv");
        BufferedWriter bw = new BufferedWriter(fw);
        
        showParseTreeInCSV(trees[POPULATION], bw);
        showParseTreeInCSV(trees[INTERVENTION], bw);
        showParseTreeInCSV(trees[OUTCOME], bw);
        showParseTreeInCSV(trees[EFFECT], bw);
        
        bw.close();
        fw.close();
    }
    
    void showParseTree(CodeSetTree tree) {
        logger.info("Codeset tree obtained after parsing JSON...");
        tree.traverse(TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO, null);
    }

    void showParseTreeInCSV(CodeSetTree tree, BufferedWriter bw) {
        logger.info("Codeset tree flattened in CSV...");
        tree.traverse(TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO_IN_CSV, bw);
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
            //jsonrefParser.showParseTreesInCSV();
            jsonrefParser.showParseTrees();
            jsonrefParser.checkFileNamesBetweenJsonAndPdf();
        
        }
        catch (Exception ex) { ex.printStackTrace(); }
        
    }
}
