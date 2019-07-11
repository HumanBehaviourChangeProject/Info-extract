/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import static com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer.analyze;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.FSDirectory;

/**
 * Generates a text file (concatenated text extracted from all documents in the collection)
 * to learn word vector representations using standard approaches, such as word2vec.
 *
 * @author dganguly
 */
public class WordVecTrainingFileGenerator extends AbstractIndexer {
    Analyzer analyzer;
    
    public WordVecTrainingFileGenerator(String propFile) throws Exception {
        super(propFile);
        
        analyzer = PaperIndexer.constructAnalyzer(BaseDirInfo.getPath(prop.getProperty("stopfile")));
    }
    
    void writeFileForWordVectorTraining() throws Exception {
        String indexPath = prop.getProperty("index");
        File indexDir = new File(indexPath);
        IndexReader reader = DirectoryReader.open(FSDirectory.open(indexDir.toPath()));
        int nDocs = reader.numDocs();
        
        FileWriter fw = new FileWriter(prop.getProperty("wordvec.trainfile", "data/com.ibm.drl.hbcp.core.wvec/wvectrain.txt"));
        BufferedWriter bw = new BufferedWriter(fw);
        
        for (int i=0; i<nDocs; i++) {
            Document d = reader.document(i);
            String content = d.get(ResearchDoc.FIELD_CONTENT);
            System.out.println("Writing out document " + d.get(ResearchDoc.FIELD_TITLE));
            content = analyze(analyzer, content); // stopword remove and stem
            bw.write(content);
            bw.newLine();
        }
        
        bw.close();
        fw.close();
        
        reader.close();
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            args = new String[1];
            System.out.println("Usage: java WordVecTrainingFileGenerator <prop-file>");
            args[0] = "init.properties";
        }

        try {
            WordVecTrainingFileGenerator wvecgen = new WordVecTrainingFileGenerator(args[0]);
            wvecgen.writeFileForWordVectorTraining();
        }
        catch (Exception ex) { ex.printStackTrace(); }        
    }
    
}
