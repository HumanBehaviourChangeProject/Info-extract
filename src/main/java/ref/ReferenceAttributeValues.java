/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ref;

import java.io.*;
import java.util.*;

/**
 *
 * @author dganguly
 */

class Attributes {
    String docName;
    HashMap<String, Attribute> attribs;

    public Attributes(String docName) {
        this.docName = docName;
        this.attribs = new HashMap<>();
    }
    
    void add(String name, String value) {
        attribs.put(name, new Attribute(name, value));
    }
    
    Attribute getAttrib(String name) {
        return attribs.get(name);
    }
}

// Not used in the flow anymore. Was used in the CSV pipeline.
public class ReferenceAttributeValues {
    
    String csvFile;
    String[] names;
    String[] values;
    HashMap<String, Attributes> ref; 

    public ReferenceAttributeValues(String csvFile) throws Exception {
        this.csvFile = csvFile;
        ref = new HashMap<>();
        loadLines(csvFile);
    }
    
    void loadLines(String csvFile) throws Exception {
        FileReader fr = new FileReader(csvFile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        
        line = br.readLine();  // the first line is the names
        String[] tokens = line.split("\t");
        names = new String[tokens.length];
        values = new String[tokens.length];
        
        for (int i = 0; i < tokens.length; i++) {
            names[i] = tokens[i];
        }
        
        while ((line = br.readLine()) != null) {
            tokens = line.split("\t");
            String docName = tokens[0];
            Attributes attribs = new Attributes(docName);
            
            for (int i=1; i < tokens.length; i++) {
                attribs.add(names[i], tokens[i]);
            }

            ref.put(docName, attribs);
        }        
    }

    public String getValue(String docName, String attrName) {
        Attributes attribs = ref.get(docName);
        if (attribs == null)
            return null;
        return attribs.getAttrib(attrName).value;
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        for (Attributes attrs : this.ref.values()) {
            for (Attribute a : attrs.attribs.values()) {
                buff.append(a.name).append(",").append(a.value);
            }
            buff.append("\n");
        }
        return buff.toString();
    }
}
