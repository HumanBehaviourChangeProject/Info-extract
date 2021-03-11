/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.parser;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;

/**
 * Code set or attribute node in the attribute hierarchy.
 * <br>
 * Code set points to a unique attribute and keeps track of all the  
 * annotations (per document) of instances of that attribute.
 * <br>
 * For example, "Age" is an attribute with id 4507411, and has an instance in
 * Joyce 2008.pdf with some value "65-69..."
 * <br>
 * The root node of the tree will have a name, but no id or attribute.
 * The node's attribute is added only when both name and id have been matched in
 * the parsing.
 * <br>
 *
 * @author dganguly
 */
public class CodeSetTreeNode {

    CodeSetTreeNode parent;
    public List<CodeSetTreeNode> children;
    public boolean isRoot;
    protected Attribute attribute;  // code-set attribute 
    protected String name;
    protected String id;
    public final PerDocRefs refs; // ref attributes - per doc

    public CodeSetTreeNode() {
        refs = new PerDocRefs();
    }

    public static CodeSetTreeNode buildRoot(String name, String id, Function<CodeSetTreeNode, List<CodeSetTreeNode>> childrenBuilder) {
        CodeSetTreeNode res = new CodeSetTreeNode();
        res.parent = null;
        res.isRoot = true;
        res.attribute = null;
        res.name = name;
        res.id = id;
        res.children = childrenBuilder.apply(res);
        return res;
    }

    public static CodeSetTreeNode buildAttributeNode(Attribute attribute, CodeSetTreeNode parent,  Function<CodeSetTreeNode, List<CodeSetTreeNode>> childrenBuilder) {
        CodeSetTreeNode res = new CodeSetTreeNode();
        res.parent = parent;
        res.isRoot = false;
        res.attribute = attribute;
        res.name = attribute.getName();
        res.id = attribute.getId();
        res.children = childrenBuilder.apply(res);
        return res;
    }

    /**
     * Get wrapper object for map of doc names to annotated instance of this node (attribute).
     */
    public PerDocRefs getDocRecordMap() {
        return refs;
    }
    
    public Set<String> getAnnotatedArms(String docName){
        return refs.getArmsInfo(docName);
    }

    /**
     * Get the highlighted/annotated text of this node (attribute) in document <code>docName</code>
     */
    public String getAnnotatedText(String docName) {
        return refs.getAnnotatedText(docName);
    }
    
    /**
     * Get the page number of annotation of this node (attribute) in document <code>docName</code>
     */
    public Integer getAnnotationPage (String docName) {
        return refs.getAnnotationPage(docName);
    }
    /**
     * Get the surrounding context the annotation of this node (attribute) in document <code>docName</code>
     */
    public String getAnnotatedContext(String docName) {
        return refs.getAnnotatedContext(docName);
    }

    public String getAnnotatedTextAllArmsContatenated(String docName) {
        String annotatedTextContatenated = "";
        Map<String, String> annotations = getAnnotatedTextAllArms(docName);
        if (annotations == null) {
            return null;
        }
        for (String s : annotations.values()) {
            annotatedTextContatenated = annotatedTextContatenated + " " + s;
        }
        return annotatedTextContatenated;
    }

    public String getAnnotatedContextAllArmsContatenated(String docName) {
        String annotatedContextContatenated = "";
        Map<String, String> annotations = getAnnotatedContextAllArms(docName);
        if (annotations == null) {
            return null;
        }
        for (String s : annotations.values()) {
            annotatedContextContatenated = annotatedContextContatenated + " " + s;
        }
        return annotatedContextContatenated;
    }

    public Map<String, String> getAnnotatedTextAllArms(String docName) {
        return refs.getAnnotatedText4AllArms(docName);
    }

    public Map<String, String> getAnnotatedContextAllArms(String docName) {
        return refs.getAnnotatedContext4AllArms(docName);
    }

    public String getSprintNo(String docName) {
        return refs.getSprintNo(docName);
    }

    /**
     * Get the name of this CodeSetTreeNode.  The root node has no attribute, so return name, otherwise
     * return the name of the attribute.
     */
    public String getName() {
        // TODO maybe correct this.  Root node doesn't have an attribute so we use the name field for that node.  
        //   Other nodes (should) have attributes so we will get the name from the attribute.
        if (attribute == null)
            return name;
        else
            return attribute.getName();
    }

    public List<CodeSetTreeNode> getChildren() {
        return this.children;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setId(String id) {
        this.id = id;
    }
    
    public boolean setAttribute(int typeInt) {
        // set attribute only have we have both name and id
        if (name != null && id != null) {
            attribute = new Attribute(id, AttributeType.values()[typeInt], name);
            return true;
        } else {
            return false;
        }
    }

    public String getPath() {
        StringBuffer buff = new StringBuffer();
        CodeSetTreeNode p = this;
        while (p != null) {
            buff.append(p.getName()).append(":");
            p = p.parent;
        }
        buff.deleteCharAt(buff.length() - 1);
        return buff.toString();
    }

    public void addPerDocRcd(String docName, AnnotatedAttributeValuePair a) {
        this.refs.add(docName, a);
    }

    public void addPerDocPerArmRcd(String docName, String armName, AnnotatedAttributeValuePair a) {
        this.refs.addArm(docName, armName, a);
    }

    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("CodeSetNode: ")
                .append(this.getName())
                .append(" (");
        if (isRoot)
            buff.append("root");
        else
            buff.append(attribute.getId());
        buff.append(") " + this.refs.getNumDocs() + " documents \n")
                .append("Per document records:")
                .append("\n")
                .append(this.refs.toString());
        return buff.toString();
    }

    public String getDocsAndArmsInfo() {
        return this.toString() + this.refs.getArmInfo();
    }

    public CodeSetTreeNode getParent() {
        return parent;
    }

    public boolean isRoot() { return isRoot; }

    public Attribute getAttribute() {
        return attribute;
    }
}
