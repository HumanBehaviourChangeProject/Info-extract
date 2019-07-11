
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.parser;

import java.io.BufferedWriter;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;

/**
 * Parser for JSON file that handles both the 'code set' hierarchy for modeling 
 * attributes (see {@link CodeSetTree}), and the references or instances of those
 * attributes which are found in documents (see {@link PerDocRefs}, which are members
 * of a {@link CodeSetTreeNode}).  The instances are grouped both by arms within a document.
 * This parser handles 'armified' JSON files.
 * <br>
 * Typical usage involves calling constructor and then {@link #buildAll()}.
 *
 * @author yhou
 */
public class JSONRefParser4Armification extends JSONRefParser {

    public static final int INTERVENTION_ARMIFICATION = 0;
    public static final int SETTING_ARMIFICATION = 1;
    public static final int OUTCOME_ARMIFICATION = 2;
    public static final int OUTCOME_VALUE_ARMIFICATION = 3;
    public static final int EFFECT_ARMIFICATION = 4;
    public static Map<String,Set<String>> armsInfo = new HashMap<>();

    static final String[] CodeSetTreeNames_Armification = {"Intervention","Setting","Outcome", "Outcome_value", "Effect"};

    public JSONRefParser4Armification(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        this.trees = new CodeSetTree[CodeSetTreeNames_Armification.length];

        init(prop);
    }

    public JSONRefParser4Armification(Properties prop) {
        this.trees = new CodeSetTree[CodeSetTreeNames_Armification.length];
        init(prop);
    }

    public JSONRefParser4Armification() {
        this.trees = new CodeSetTree[CodeSetTreeNames_Armification.length];
    }

    private void init(Properties prop) {
        this.baseDir = BaseDirInfo.getBaseDir();
        this.fileName = prop.getProperty("ref_armification.json");
    }

    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#loadCodeSets()
     */
    @Override
    public void loadCodeSets() throws IOException {
        this.trees[INTERVENTION_ARMIFICATION] = loadCodeSet(INTERVENTION_ARMIFICATION);
        this.trees[SETTING_ARMIFICATION] = loadCodeSet(SETTING_ARMIFICATION);
        this.trees[OUTCOME_ARMIFICATION] = loadCodeSet(OUTCOME_ARMIFICATION);
        this.trees[OUTCOME_VALUE_ARMIFICATION] = loadCodeSet(OUTCOME_VALUE_ARMIFICATION);
        this.trees[EFFECT_ARMIFICATION] = loadCodeSet(EFFECT_ARMIFICATION);
    }

    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#groupByDocs()
     */
    @Override
    public void groupByDocs() throws IOException {
        groupByDocs(trees[INTERVENTION_ARMIFICATION]);
        groupByDocs(trees[SETTING_ARMIFICATION]);
        groupByDocs(trees[OUTCOME_ARMIFICATION]);
        groupByDocs(trees[OUTCOME_VALUE_ARMIFICATION]);
        groupByDocs(trees[EFFECT_ARMIFICATION]);
    }

    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#buildCodeSetsFromURL(java.net.URL)
     */
    @Override
    public void buildCodeSetsFromURL(URL url) throws IOException {
        parseURL(url);
        loadCodeSets();
        trees[INTERVENTION_ARMIFICATION].generateJSTreeStructuredJSON();
        trees[SETTING_ARMIFICATION].generateJSTreeStructuredJSON();
        trees[OUTCOME_ARMIFICATION].generateJSTreeStructuredJSON();
        trees[OUTCOME_VALUE_ARMIFICATION].generateJSTreeStructuredJSON();
        trees[EFFECT_ARMIFICATION].generateJSTreeStructuredJSON();
    }

    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#groupByDocs(com.ibm.drl.hbcp.parser.CodeSetTree)
     */
    @Override
    public void groupByDocs(CodeSetTree tree) throws IOException {

        net.minidev.json.JSONArray codes = ctx.read("$.References..Codes[*]");

        for (Iterator iter = codes.iterator(); iter.hasNext();) {

            LinkedHashMap child = (LinkedHashMap) iter.next();
//            String attribId = ((Integer)(child.get("AttributeId"))).toString();
            String attribId = (child.get("AttributeId")).toString();

            net.minidev.json.JSONArray itemAttribTextDetails
                    = (net.minidev.json.JSONArray) child.get("ItemAttributeFullTextDetails");
            if (itemAttribTextDetails == null || itemAttribTextDetails.isEmpty()) {
                continue;
            }

            List<CodeSetTreeNode> nodes = tree.getArmNodes(attribId);
            if (nodes == null || nodes.isEmpty()) {
//                logger.trace("No node found for attribute id: " + attribId);
                continue; // attribute must be in another CodeSetTree
            }

            LinkedHashMap itemObjMap = (LinkedHashMap) itemAttribTextDetails.get(0);

            String docName = (String) itemObjMap.get("DocTitle");

            String context = removeSpecialCharacters((String) child.get("AdditionalText"));
            String highlightedText = removeSpecialCharacters((String) itemObjMap.get("Text"));
            String sprintNo = (String) child.get("SprintNo");
            String armID = String.valueOf(child.get("ArmId"));
            String armTitle = (String) child.get("ArmTitle");
            // +++DG: @YHOU: The following two were never used... Please check
            // String pageNumber=((String)itemObjMap.get("Text")).split(":")[0];
            // int annotationPage = Integer.parseInt(pageNumber.replace("Page ","")); 
            // --DG
            if (armTitle.isEmpty()) {
                armTitle = "general";
            }

            // TODO check multiple arms/nodes should have same attribute instance information
            for (CodeSetTreeNode node : nodes) {
                Attribute attribute = node.getAttribute();
                AnnotatedAttributeValuePair attrib = new AnnotatedAttributeValuePair(attribute, highlightedText, docName, armID, context, highlightedText, sprintNo, 0);
    
                if(!attrib.getArm().equalsIgnoreCase("general")){
                    if(armsInfo.containsKey(attrib.getDocName())){
                        armsInfo.get(attrib.getDocName()).add(attrib.getArm());
                    }else{
                        Set<String> arms = new HashSet<>();
                        arms.add(attrib.getArm());
                        armsInfo.put(attrib.getDocName(), arms);
                    }
                }


                    node.addPerDocPerArmRcd(docName, armTitle, attrib);
                    node.addPerDocRcd(docName, attrib);
            }

        }
    }

    public Map<String, Set<String>> getArmsInfo(){
        return armsInfo;
    }
    
    
    
    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#showParseTrees()
     */
    @Override
    void showParseTrees() {
        showParseTree(trees[INTERVENTION_ARMIFICATION]);
        documents.addAll(trees[INTERVENTION_ARMIFICATION].docs);
        showParseTree(trees[SETTING_ARMIFICATION]);
        documents.addAll(trees[SETTING_ARMIFICATION].docs);
        showParseTree(trees[OUTCOME_ARMIFICATION]);
        documents.addAll(trees[OUTCOME_ARMIFICATION].docs);
        showParseTree(trees[OUTCOME_VALUE_ARMIFICATION]);
        documents.addAll(trees[OUTCOME_VALUE_ARMIFICATION].docs);
        showParseTree(trees[EFFECT_ARMIFICATION]);
        documents.addAll(trees[EFFECT_ARMIFICATION].docs);
    }

    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#showParseTreesInCSV()
     */
    @Override
    void showParseTreesInCSV() throws IOException {
        FileWriter fw = new FileWriter(baseDir + "/" + prop.getProperty("ref.json") + ".csv");
        BufferedWriter bw = new BufferedWriter(fw);

        showParseTreeInCSV(trees[SETTING_ARMIFICATION], bw);
        showParseTreeInCSV(trees[INTERVENTION_ARMIFICATION], bw);
        showParseTreeInCSV(trees[OUTCOME_VALUE_ARMIFICATION], bw);

        bw.close();
        fw.close();
    }

    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#showParseTree(com.ibm.drl.hbcp.parser.CodeSetTree)
     */
    @Override
    void showParseTree(CodeSetTree tree) {
        logger.info("Codeset tree obtained after parsing JSON...");
        tree.traverse(TREE_INFO_ENUM.CODESET_AND_PERDOC_ARM_INFO, null);
    }

    /* (non-Javadoc)
     * @see com.ibm.drl.hbcp.parser.JSONRefParser#docsBySprint()
     */
    @Override
    public Map<String, Set<String>> docsBySprint() throws IOException {
        Map<String, Set<String>> docsbysprint = new HashMap<>();
        net.minidev.json.JSONArray docs = ctx.read("$.References[*]");
        for (Iterator iter = docs.iterator(); iter.hasNext();) {
            LinkedHashMap child = (LinkedHashMap) iter.next();
            List child_codes = (List) child.get("Codes");
            if(child_codes==null) {
                System.err.println("no annotation file:" + child.toString());
                continue;
            }
            for (Iterator iter1 = child_codes.iterator(); iter1.hasNext();) {
                LinkedHashMap attribute = (LinkedHashMap) iter1.next();
                List detail = (List) attribute.get("ItemAttributeFullTextDetails");
                if(detail==null) continue;
                for (Iterator iter2 = detail.iterator(); iter2.hasNext();) {
                    LinkedHashMap attriDetail = (LinkedHashMap) iter2.next();
                    if (attriDetail.get("DocTitle") == null) {
                        continue;
                    }
                    String doctitle = (attriDetail.get("DocTitle")).toString();
                    String sprintno = "Sprint5";
                    if (docsbysprint.containsKey(sprintno)) {
                        docsbysprint.get(sprintno).add(doctitle);
                    } else {
                        Set<String> documents = new HashSet<>();
                        documents.add(doctitle);
                        docsbysprint.put(sprintno, documents);
                    }

                }
            }
        }
        return docsbysprint;
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.err.println("Usage: java JSONRefParser <prop-file>");
            args[0] = "init.properties";
        }

        try {
            JSONRefParser4Armification jsonrefParser = new JSONRefParser4Armification(args[0]);
            jsonrefParser.buildAll();
//            jsonrefParser.showParseTreesInCSV();
            jsonrefParser.showParseTrees();
            //jsonrefParser.checkFileNamesBetweenJsonAndPdf();

        } catch (Exception ex) {
            ex.printStackTrace();
        }

    }
}
