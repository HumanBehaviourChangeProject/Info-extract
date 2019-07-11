/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.tableanalyzer;

import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import java.util.*;
import java.io.*;

/**
 *
 * @author dganguly
 */

class Table {
    int direction;
    String caption;
    List<String> note;
    List<String> body;
        
    Table(String caption, int direction) {
        this.caption = caption;
        note = new ArrayList<>();
        body = new ArrayList<>();
        this.direction = direction;
    }
    
    void addNote(String line) { this.note.add(line); }
    
    void addRcd(String line) {
        body.add(line);
    }
    
    void impartStructure() {
        // Find the average number of columns in the records
        int numRecords = this.body.size();
        int[] numCols = new int[numRecords];
        float avg = 0, stdev = 0;
        
        for (int i=0; i < numRecords; i++) {
            String thisRcd = body.get(i);
            numCols[i] = thisRcd.split("\\s+").length;
            avg += numCols[i];
        }
        avg = avg/numRecords;
        
        for (int i=0; i < numRecords; i++) {
            String thisRcd = body.get(i);
            stdev += (numCols[i]-avg)*(numCols[i]-avg);
        }
        stdev = (float)Math.sqrt(stdev/numRecords);
        
        for (int i=0; i < numRecords; i++) {
            if (numCols[i] >= avg - stdev && numCols[i] <= avg + stdev)
                System.out.println(this.body.get(i));
        }
        
    }
    
    public String toString() {
        StringBuffer buff = new StringBuffer();
        buff.append("Caption: ").append("\n")
                .append(this.caption).append("\n");
        
        int n = note.size();
        buff.append("Note: ").append("\n");
        for (int i=n-1; i >= 0; i--) {
            buff.append(note.get(i)).append("\n");
        }
        buff.append("\n");
        
        n = body.size();
        buff.append("Body: ").append("\n");
        
        if (direction == 1) {
            for (int i=n-1; i>=0; i--) {
                String rcd = body.get(i);
                buff.append(rcd).append("\n");
            }
        }
        else {
            for (int i=0; i<n; i++) {
                String rcd = body.get(i);
                buff.append(rcd).append("\n");
            }            
        }
        return buff.toString();
    }
    
    // Some rows might have been split. If that's the case, merge consecutive <tr>s
    public void mergeRows() {
        
        int sum = 0;
        for (String row : body) {
            int ntokens = row.split("\\s+").length;
            sum += ntokens;            
        }
        
        float avg = sum/(float)body.size();
        
        for (int i=0; i < body.size(); i++) {
            String row = body.get(i);
            int ntokens = row.split("\\s+").length;
            if (ntokens < avg && ntokens > avg/2) {
                // merge with previous
                if (i>1) {
                    body.set(i, row + body.get(i-1));
                    body.remove(i-1);                    
                }
            }            
        }
    }
}

public class TableDataExtractor {
    String plainText;
    String[] lines;
    List<Table> tables;
    
    static final String TEXT_DELIMS = ";,.?!";
    static final int TEXT_DELIMS_LEN = TEXT_DELIMS.length();
    static final int MARGIN = 50;
    
    public TableDataExtractor(String file) throws Exception {
        ResearchDoc researchDoc = new ResearchDoc(new File(file));
        researchDoc.extractInfoFromDOM();
        this.plainText = researchDoc.getPPText();
        
        loadAsLines();
        tables = new ArrayList<>();
    }
    
    void loadAsLines() {
        lines = plainText.split("\n");
    }
    
    boolean candidateTableText(String line) {
        line = line.replaceAll("\\.", "-");
        
        for (char ch : line.toCharArray()) {
            if (Character.isDigit(ch))
                return true;
        }
        
        for (int i=0; i < TEXT_DELIMS_LEN; i++) {
            char ch = TEXT_DELIMS.charAt(i);
            int pos = line.indexOf(ch);
            if (pos >= 0)
                return false;
        }
        return true;
    }
    
    List<Table> identifyTableCaptions() {
        
        List<Integer> captionIndexes = new ArrayList<>();
        for (int i=0; i < lines.length; i++) {
            if (lines[i].toLowerCase().startsWith("table ")) {
                captionIndexes.add(i);
            }
        }

        int ntables = captionIndexes.size();
        for (int i=0; i < ntables; i++) {
            int direction = directionToGrowTable(captionIndexes.get(i));
            
            if (direction != 0) {
                Table t = growTable(captionIndexes.get(i), direction);            
                System.out.println("Table: " + (i+1) + "\n" + t);
                tables.add(t);
            }
        }
        return tables;
    }
    
    List<Table> getTables() { return tables; }
    
    
    boolean isEOS(String line) {
        line = line.trim();
        char ch = line.charAt(line.length()-1);
        return ch=='.' || ch=='?' || ch=='!';
    }
    
    int directionToGrowTable(int captionIndex) {
        
        final int MARGIN = 10;
        int prevIndex = captionIndex;
        int nextIndex = captionIndex;
        
        // Check the previous and the next lines
        while ((lines[--prevIndex].split("\\s+").length==1));
        if (!isEOS(lines[prevIndex]))
            return -1; // downwards
        while ((lines[++nextIndex].split("\\s+").length==1));
        if (!isEOS(lines[nextIndex]))
            return 1; // upwards
            
        prevIndex--;
        nextIndex++;
        // Go to the prev/next content lines
        while ((lines[prevIndex].split("\\s+").length==1 || !candidateTableText(lines[prevIndex])) && captionIndex-prevIndex < MARGIN)
            prevIndex--;
        while ((lines[nextIndex].split("\\s+").length==1 || !candidateTableText(lines[nextIndex])) && nextIndex-captionIndex < MARGIN)
            nextIndex++;
        
        return captionIndex-prevIndex < MARGIN? 1 : nextIndex-captionIndex < MARGIN? -1 : 0;        
    }
    
    int numTokens(String line) { return line.split("\\s+").length; }
    
    Table growTable(int captionPos, int direction) {
        String[] tokens;
        int offset = 1;
        String contextLine;
    
        Table table = new Table(lines[captionPos], direction);
        
        // Skip to last line of caption text
        do {
            contextLine = lines[captionPos - direction*offset];
            tokens = contextLine.split("\\s+");
            offset++;
        }
        while (captionPos-offset >= 0 && tokens.length > 1);

        if (direction == 1) {
            // Skip to first line of caption text
            do {
                contextLine = lines[captionPos - direction*offset];
                tokens = contextLine.split("\\s+");
                offset++;
                table.addNote(contextLine);
            }
            while (captionPos-offset >= 0 && tokens.length > 1);
        }
        
        // Go on unless you find consecutive lines, i.e. no paragraph breaks
        // Each row of a table is separated by a paragraph break, whereas
        // when you go back to the text part of the pdf, it's expected
        // that there wont be any paragraph break.
        do {
            if (captionPos - direction*offset >= lines.length)
                break;
            if (captionPos - direction*(offset+1) >= lines.length)
                break;
            
            contextLine = lines[captionPos - direction*offset];
            String prevLine = lines[captionPos - direction*(offset + 1)];
            
            if (!(candidateTableText(prevLine) || candidateTableText(contextLine)))
                break;
            
            // consider the case of lines that are too long to fit in
            // the table width... have to merge such lines...
            if (candidateTableText(prevLine)) {
                table.addRcd(prevLine + " " + contextLine);
                offset = offset + 2;
                continue;
            }
            
            // prevLine has to be a para break
            tokens = contextLine.split("\\s+");
            if (prevLine.split("\\s+").length > 1)
                break;
                        
            offset = offset + 2;
            table.addRcd(contextLine);
        }
        while (captionPos - direction*offset >= 0);

        table.mergeRows();
        return table;
    }
    
    public static void main(String[] args) {
        try {
            System.out.println("Identifying locations of tabular data...");
            
            String fileName = "2015 LEAP trial outcome paper.pdf";
            String filePath = BaseDirInfo.getBaseDir();
            TableDataExtractor te = new TableDataExtractor(filePath + "data/pdfs/" + fileName);
            
            List<Table> tables = te.identifyTableCaptions();
            
            System.out.println("Identifying structure from tables...");
            int count = 0;
            for (Table table : tables) {
                System.out.println("Table" + (count++));
                table.impartStructure();
            }
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        
    }
    
}
