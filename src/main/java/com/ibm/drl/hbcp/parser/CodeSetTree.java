/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.parser;

import java.io.BufferedWriter;
import java.io.IOException;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *  Tree data structure for the code set hierarchy.  The JSON parser
 *  populates this data structure from the hierarchy of attributes (CodeSetTreeNodes) in the
 *  JSON.
 *  <br>
 *  For JSON ref parsing, one CodeSetTree is used per annotation type.
 *  This class also keeps a map of attribute ids to CodeSetTreeNodes. 
 *
 * @author dganguly
 */
public class CodeSetTree {
    String json;
    CodeSetTreeNode root;
    public final HashMap<String, CodeSetTreeNode> cache;
    public final HashMap<String, List<CodeSetTreeNode>> cacheArms;
    public Set<String> docs = new HashSet<>();
    
    static int DUMMY_ID = 1;    
    static Logger logger = LoggerFactory.getLogger(JSONRefParser.class);
        
    public CodeSetTree(CodeSetTreeNode root) {
        this.root = root;
        cache = new HashMap<>();
        cacheArms = new HashMap<>();
        try {
            traverseTreeRecursive(this.root, 0, TREE_INFO_ENUM.NONE, null);  // for building up the attribute-id lookup table.
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public String getJSON() { return json; }
    
    /** Traverse the tree and store the nodes in a hashmap keyed
     * by the attribute ids (for O(1) access to the nodes given
     * an attribute id). 
     * This is useful to fill in the nodes with the
     * reference values.
     */    
    public void traverse(TREE_INFO_ENUM info, BufferedWriter bw) { 
        try {
            traverseTreeRecursive(this.root, 0, info, bw);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    /**
     * Traverse this CodeSetTree from root to build JSON-formatted string of whole tree.
     * 
     * @return this CodeSetTree as JSON-formatted string
     */
    public String generateJSTreeStructuredJSON() {
        StringBuffer buff = new StringBuffer("[");
        buff.append(generateJSTreeStructuredJsonRecursive(root));
        buff.append("]");
        json = buff.toString();
        return json;
    }
    
    private String generateJSTreeStructuredJsonRecursive(CodeSetTreeNode node) {
        
        StringBuffer buff = new StringBuffer();
        buff.append("{").append("\n");
        
        buff.append("\"id\":");
        buff.append(node.attribute == null? DUMMY_ID++: node.attribute.getId());
        buff.append(",");
        
        buff.append("\"text\":\"");
        buff.append(node.getName());
        buff.append("\"");
        
        if (node.children != null) {
            buff.append(",");
            buff.append("\"children\":[");
            
            int numChildren = node.children.size();
            for (int i=0; i<numChildren-1; i++) {
                CodeSetTreeNode child = node.children.get(i);
                buff.append(generateJSTreeStructuredJsonRecursive(child));
                buff.append(",");
            }
            CodeSetTreeNode child = node.children.get(numChildren-1);
            buff.append(generateJSTreeStructuredJsonRecursive(child));            
            buff.append("]");
        }
        
        buff.append("}");
        return buff.toString();
    }
    
    private void traverseTreeRecursive(CodeSetTreeNode node, int depth,
                                        TREE_INFO_ENUM info,
                                        BufferedWriter bw) throws IOException { // Depth-first
        if (node == null)
            return;
        
        StringBuffer indent = new StringBuffer();
        for (int i=0; i < depth; i++)
            indent.append("\t");

        indent.append(node.getName());
        if (!node.isRoot) {
            indent.append(", ").append(node.attribute.getId());
        }
        
        if (info == TREE_INFO_ENUM.CODESET_ONLY) {
    //        logger.info(indent.toString());
        }
        else if (info == TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO) {
            logger.info(indent.toString() + "\n" + node.toString());
            docs.addAll(node.refs.refs.keySet());
        }
        else if(info==TREE_INFO_ENUM.CODESET_AND_PERDOC_ARM_INFO){ 
            logger.info(indent.toString() + "\n" + node.getDocsAndArmsInfo());
            docs.addAll(node.refs.refs.keySet());
        }
        else if (info == TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO_IN_CSV) {
            bw.write(node.refs.csvFormatted(node.attribute.getId()));
        }
        
        if (node.attribute != null && !cache.containsKey(node.attribute.getId())){
            cache.put(node.attribute.getId(), node);
            List<CodeSetTreeNode> arms = new ArrayList<>();
            arms.add(node);
            cacheArms.put(node.attribute.getId(), arms);
        }
        
        if(node.attribute != null && cacheArms.containsKey(node.attribute.getId())){
            cacheArms.get(node.attribute.getId()).add(node);
        }
        
        if (node.children != null) {
            for (CodeSetTreeNode child : node.children) {
                child.parent = node;
                traverseTreeRecursive(child, depth+1, info, bw);
            }
        }
    }
    
    /**
     * Returns the CodeSetTreeNode matching <code>attribId</code>.
     * traverse() must be called first top populate the attribute-id --to-- 
     * CodeSetTreeNode mapping.
     * 
     *  @return node matching given attribute id
     */
    public CodeSetTreeNode getNode(String attribId) {
        return cache.get(attribId);
    }

    /**
     * Returns a list of the CodeSetTreeNode matching <code>attribId</code>.
     * traverse() must be called first top populate the attribute-id --to-- 
     * CodeSetTreeNode list mapping.
     *
     */
    public List<CodeSetTreeNode> getArmNodes(String attribId) {
        return cacheArms.get(attribId);
    }

    
}
