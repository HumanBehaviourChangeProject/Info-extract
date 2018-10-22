/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package ref;

import java.util.*;
import java.io.*;

/**
 *
 * @author dganguly
 */
public class Attribute {
    String id;
    String name;
    String value;
    String context;
    String highlightedText;
    String docTitle;
    String sprintNo;
    
    public Attribute() { }
    
    public Attribute(String name, String value) {
        this.name = name;
        this.value = value;
    }

    public Attribute(String id) {
        this.id = id;
    }    
    
    public String getContextText() { return this.context; }
    public String getAnnotatedText() { return this.highlightedText; }
    public String getId() { return id; }
    public String getName() { return name; }
    public String getDocTitle() { return docTitle; }
    public String getSprintNo() {return sprintNo;}
    
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff
            .append("id='")    
            .append(id)
            .append("'")
            .append("name='")    
            .append(name)
            .append(",")
            /*
            .append("value='")    
            .append(value)
            .append("',")
            .append("context='")    
            .append(context)
            .append("',")
            .append("highlighted='")    
            .append(highlightedText)
            .append("'")*/
        ;
        return buff.toString();
    }
}
