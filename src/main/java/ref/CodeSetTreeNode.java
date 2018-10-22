/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ref;

import java.io.*;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author dganguly
 */
public class CodeSetTreeNode {
    CodeSetTreeNode parent;
    List<CodeSetTreeNode> children;
    boolean isRoot;
    Attribute a;  // code-set attribute 
    PerDocRefs refs; // ref attributes - per doc
    
    public CodeSetTreeNode() {
        a = new Attribute();
        refs = new PerDocRefs();
    }
    
    public CodeSetTreeNode(String name) {
        a = new Attribute();
        a.name = name;
        refs = new PerDocRefs();
    }
    
    public PerDocRefs getDocRecordMap() { return refs; }
    
    public String getAnnotatedText(String docName) {
        return refs.getAnnotatedText(docName);
    }
    
    public String getSprintNo(String docName) {
        return refs.getSprintNo(docName);
    }



    public String getName() { return a.name; }

    public List<CodeSetTreeNode> getChildren() { return this.children; }
    
    public void setName(String name) {
        a.name = name;
    }
    
    public void setId(String id) {
        a.id = id;
    }
    
    public String getPath() {
        StringBuffer buff = new StringBuffer();
        CodeSetTreeNode p = this;
        while (p!= null) {
            buff.append(p.getName()).append(":");
            p = p.parent;
        }
        buff.deleteCharAt(buff.length()-1);
        return buff.toString();                
    }

    public void addPerDocRcd(String docName, Attribute a) {
        this.refs.add(docName, a);
    }
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("Attribute: ")
            .append(this.a.name)
            .append(" (")
            .append(a.id)
            .append(") " + this.refs.getNumDocs() +" documents \n")
            .append("Per document records:")
            .append("\n")
            .append(this.refs.toString())
        ;
        return buff.toString();
    }
    
    public CodeSetTreeNode getParent() {
        return parent;
    }
}

