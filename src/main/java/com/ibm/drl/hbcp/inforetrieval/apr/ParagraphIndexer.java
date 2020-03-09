/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import com.ibm.drl.hbcp.util.LuceneField;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.FSDirectory;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.StringReader;
import java.text.BreakIterator;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Properties;

/**
 *
 * @author dganguly
 */
public class ParagraphIndexer {

    Properties prop;
    Analyzer analyzer;
    IndexWriter paraWriter;
    ParagraphAnnotator paraAnnotator;

    public static final String APR_ID_FIELD = "ID";
    public static final String APR_DOCTITLE_FIELD = "TITLE";
    public static final String APR_PARA_CONTENT_FIELD = "TEXT";
    public static final String APR_IS_ANNOTATED_PARAGRAPH_FIELD = "ANNOTATED";
    public static final String APR_ATTRIB_ID_FIELD = "ATTRIB_ID";
    @Deprecated
    public static final String APR_ATTRIB_NAME_FIELD = "ATTRIB_NAME"; // not actually indexed
    public static final String APR_ATTRIB_VALUE_FIELD = "ATTRIB_VALUE";
    public static final String APR_ATTRIB_CONTEXT_FIELD = "ATTRIB_CONTEXT";
    
    public static final String ATTRIB_SEPARATOR_PATTERN = ":";
    
    
    public ParagraphIndexer(String propFileName) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(new File(propFileName)));
        
        analyzer = new ParagraphAnalyzer();
        paraAnnotator = new ParagraphAnnotator(propFileName, analyzer);

        File indexDirDocs = new File(prop.getProperty("apr.index"));
        IndexWriterConfig iwcfg = new IndexWriterConfig(analyzer);
        iwcfg.setOpenMode(IndexWriterConfig.OpenMode.CREATE);
        paraWriter = new IndexWriter(FSDirectory.open(indexDirDocs.toPath()), iwcfg);
    }
    
    public void indexAll() throws Exception {
        String collPath = prop.getProperty("coll");
        File folderName = new File(collPath);
        File files[] = folderName.listFiles();
        int count;
        int docCount = 0;
        
        for (File f : files) {
            List<String> paraList = getParagraphsForEachDoc(f);
            count = 0;
            for (String para : paraList) {
                indexFile(f.getName(), para, docCount, count++);
            }
            System.out.println(String.format("Indexed %d paragraphs of document %s", count, f.getName()));
            docCount++;
        }
        paraWriter.close();
    }
    
    boolean isBlankLine(String line) {
        return line.trim().length()==0;
    }
    
    int getNumSentences(String source) {
        BreakIterator brkiterator = BreakIterator.getSentenceInstance(Locale.US);
        brkiterator.setText(source);
        
        int length = 0;
        int boundaryIndex = brkiterator.first();
        
        while (boundaryIndex != BreakIterator.DONE) {
            length++;
            boundaryIndex = brkiterator.next();
        }
        return length;
    }
    
    boolean startOfParagraph(String line, String prevPara) {
        if (line.length() == 0) return false;
        
        boolean startOfPara = Character.isUpperCase(line.trim().charAt(0));
        if (prevPara.length() > 0)
            startOfPara &= prevPara.charAt(prevPara.length()-1) != '-';
        
        return startOfPara;        
    }
    
    String removeReferences(String text) {
        String pptext;
        pptext = text.replaceAll("\\.[0-9]+[-,][0-9]+ ", "\\. ");
        pptext = pptext.replaceAll("\\.[0-9]+ ", "\\. ");
        pptext = pptext.replaceAll(",[0-9]+[-,][0-9]+ ", ", ");
        pptext = pptext.replaceAll(",[0-9]+ ", ", ");
        return pptext;
    }
    
    List<String> getParagraphsForEachDoc(File fileName) throws Exception {
        ResearchDoc rd = new ResearchDoc(fileName);
        rd.extractInfoFromDOM();
        
        String procesedText = rd.getPlainText();
        
        BufferedReader br = new BufferedReader(new StringReader(procesedText));
        String line, para;
        StringBuffer buff = new StringBuffer();
        
        int length;
        int minNumSentencesInDoc = Integer.parseInt(prop.getProperty("apr.mindoclen", "2"));
        boolean blankSeen = false;
        
        List<String> paraList = new ArrayList<>();
        
        while ((line = br.readLine()) != null) {
            if (isBlankLine(line)) {
                blankSeen = true;
                continue;
            }
            
            boolean startOfPara = blankSeen && startOfParagraph(line, buff.toString()); // check beginning with capital letter
            
            if (startOfPara) {
                para = buff.toString() + " ";
                para = removeReferences(para).trim();
                length = getNumSentences(para);
                if (length > minNumSentencesInDoc) {
                    paraList.add(para);
                }
                buff = new StringBuffer();
            }
            
            length = buff.length();
            if (length > 0 && buff.charAt(length-1)=='-') {
                buff.deleteCharAt(length-1);  // remove trailing hyphens
            }
            else
                buff.append(" ");
                
            buff.append(line);
            blankSeen = false;
        }
        return paraList;
    }
    
    public static String paraId(String docName, int paraId) {
        return docName + "_" + paraId;
    }
    
    public void indexFile(String filename, String line, int docId, int paragraphId) throws Exception {
        Document doc = new Document();
        String paraId = paraId(String.valueOf(docId), paragraphId);
        
        doc.add(new Field(ParagraphIndexer.APR_DOCTITLE_FIELD, filename, LuceneField.STORED_NOT_ANALYZED.getType()));
        doc.add(new Field(ParagraphIndexer.APR_ID_FIELD, paraId, LuceneField.STORED_NOT_ANALYZED.getType()));
        doc.add(new Field(ParagraphIndexer.APR_PARA_CONTENT_FIELD, line, LuceneField.STORED_NOT_ANALYZED.with(ft -> ft.setStoreTermVectors(true)).getType()));
        paraAnnotator.annotate(doc, filename, line);
        paraWriter.addDocument(doc);
    }
    
    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java ParagraphIndexer <prop-file>");
            args[0] = "init.properties";
        }
        
        try {
            ParagraphIndexer pindexer = new ParagraphIndexer(args[0]);
            pindexer.indexAll();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }
}
