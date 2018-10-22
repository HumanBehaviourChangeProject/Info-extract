/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ref;

import java.io.BufferedWriter;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dganguly
 */
public class CodeSetTree {
    String json;
    CodeSetTreeNode root;
    HashMap<String, CodeSetTreeNode> cache;
    public Set<String> docs = new HashSet();
    
    
    
    static int DUMMY_ID = 1;    
    static Logger logger = LoggerFactory.getLogger(JSONRefParser.class);
        
    public CodeSetTree(CodeSetTreeNode root) {
        this.root = root;
        cache = new HashMap<>();
        try {
            traverseTreeRecursive(this.root, 0, TREE_INFO_ENUM.NONE, null);  // for building up the attribute-id lookup table.
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public String getJSON() { return json; }
    
    // while traversing the tree store the nodes in a hashmap keyed
    // by the attribute ids for O(1) access to the nodes given
    // an attribute id.. this is useful to fill in the nodes with the
    // reference values...    
    public void traverse(TREE_INFO_ENUM info, BufferedWriter bw) { 
        try {
            traverseTreeRecursive(this.root, 0, info, bw);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
    
    public String generateJSTreeStructuredJSON() {
        StringBuffer buff = new StringBuffer("[");
        buff.append(generateJSTreeStructuredJSON_Recursive(root));
        buff.append("]");
        json = buff.toString();
        return json;
    }
    
    public String generateJSTreeStructuredJSON_Recursive(CodeSetTreeNode node) {
        
        StringBuffer buff = new StringBuffer();
        buff.append("{").append("\n");
        
        buff.append("\"id\":");
        buff.append(node.a.id==null? DUMMY_ID++: node.a.id);
        buff.append(",");
        
        buff.append("\"text\":\"");
        buff.append(node.a.name);
        buff.append("\"");
        
        if (node.children != null) {
            buff.append(",");
            buff.append("\"children\":[");
            
            int numChildren = node.children.size();
            for (int i=0; i<numChildren-1; i++) {
                CodeSetTreeNode child = node.children.get(i);
                buff.append(generateJSTreeStructuredJSON_Recursive(child));
                buff.append(",");
            }
            CodeSetTreeNode child = node.children.get(numChildren-1);
            buff.append(generateJSTreeStructuredJSON_Recursive(child));            
            buff.append("]");
        }
        
        buff.append("}");
        return buff.toString();
    }
    
    private void traverseTreeRecursive(CodeSetTreeNode node, int depth,
                                        TREE_INFO_ENUM info,
                                        BufferedWriter bw) throws Exception { // Depth-first
        if (node == null)
            return;
        
        StringBuffer indent = new StringBuffer();
        for (int i=0; i < depth; i++)
            indent.append("\t");

        indent.append(node.getName())
              .append(", ")
              .append(node.a.id);
        
        if (info == TREE_INFO_ENUM.CODESET_ONLY) {
    //        logger.info(indent.toString());
        }
        else if (info == TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO) {
    //        logger.info(indent.toString() + "\n" + node.toString());
            docs.addAll(node.refs.refs.keySet());
            
        }
        else if (info == TREE_INFO_ENUM.CODESET_AND_PERDOC_INFO_IN_CSV) {
            bw.write(node.refs.csvFormatted(node.a.id));
        }
        
        if (node.a.id != null && !cache.containsKey(node.a.id))
            cache.put(node.a.id, node);
        
        if (node.children != null) {
            for (CodeSetTreeNode child : node.children) {
                child.parent = node;
                traverseTreeRecursive(child, depth+1, info, bw);
            }
        }
    }
    
    public CodeSetTreeNode getNode(String attribId) {
        return cache.get(attribId);
    }
    
}
