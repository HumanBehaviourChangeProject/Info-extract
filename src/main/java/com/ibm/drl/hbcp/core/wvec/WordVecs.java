/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import javax.annotation.Nullable;
import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * A collection of WordVec instances for each unique term in
 * the collection.
 * @author Debasis
 */
public class WordVecs {

    Properties prop;
    int k;
    String nnFile;
    HashMap<String, WordVec> wordvecmap;
    HashMap<String, List<WordVec>> nearestWordVecsMap; // Store the pre-computed NNs
    
    /**
     * Initializes the container class from a specified properties file
     * @param propFile
     * @throws IOException 
     */
    public WordVecs(String propFile) throws IOException {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        init(null, prop);
    }
    
    /**
     * Initializes the container class from a specified properties object.
     * @param prop
     * @throws IOException 
     */
    public WordVecs(Properties prop) throws IOException {
        init(null, prop);
    }

    /**
     * Initializes the container class from an in-memory stream formatted in the same
     * way as a text-formatted word2vec output. This is useful to avoid intermediate writing of files.
     * @param input
     * @param props
     * @throws IOException 
     */
    public WordVecs(@Nullable InputStream input, Properties props) throws IOException {
        init(input, props);
    }
    
    void init(InputStream input, Properties prop) throws IOException {
        this.prop = prop;
        nnFile = prop.getProperty("wordvecs.nn");        
        
        if (wordvecmap != null)
            return; // already loaded from somewhere else in the flow...
        
        k = Integer.parseInt(prop.getProperty("wordvecs.numnearest", "5"));
        
        System.out.println("Loading word vecs");
        String loadFrom = prop.getProperty("wordvecs.readfrom");
        if (loadFrom == null)
            return;

        if (input != null || loadFrom.equals("vec")) {
            if (input == null)
                input = new FileInputStream(prop.getProperty("wordvecs.vecfile"));
            loadFromTextFile(input);
        }
        else {
            File serFile = new File(prop.getProperty("wordvecs.objfile"));
            loadObjectFromSerFile(new FileInputStream(serFile));
        }
        
        System.out.println("Loaded word vecs");
        initNN();
    }
    
    boolean hasDigit(String word) {
        int len = word.length();
        for (int i=0; i < len; i++) {
            if (Character.isDigit(word.charAt(i)))
                return true;
        }
        return false;
    }
    
    void loadFromTextFile(InputStream wordvecFile) {
        wordvecmap = new HashMap();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(wordvecFile))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                if (hasDigit(line.split("\\s+")[0]))
                    continue;
                WordVec wv = new WordVec(line);
                wordvecmap.put(wv.word, wv);
            }
        }
        catch (Exception ex) { ex.printStackTrace(); }        
    }
    
    void loadObjectFromSerFile(InputStream serFileInput) {
        try {
            ObjectInputStream oin = new ObjectInputStream(serFileInput);
            wordvecmap = (HashMap<String, WordVec>)oin.readObject();
            oin.close();
            serFileInput.close();
        }
        catch (Exception ex) { ex.printStackTrace(); }
    }

    public void storeVectorsAsSerializedObject() throws Exception {
        File oFile = new File(prop.getProperty("wordvecs.objfile"));
        FileOutputStream fout = new FileOutputStream(oFile);
        ObjectOutputStream oos = new ObjectOutputStream(fout);
        oos.writeObject(this.wordvecmap);
        oos.close();
        fout.close();
    }
    
    /**
     * Initializes a list of nearest neighbors (pre-computed) for each word vector.
     * @throws IOException 
     */
    public void initNN() throws IOException {
        if (nnFile==null || !(new File(nnFile).exists())) {
            System.out.println("No NN file to load NN data!");
            return;
        }
        
        nearestWordVecsMap = new HashMap<>(wordvecmap.size());
        FileReader fr = new FileReader(nnFile);
        BufferedReader br = new BufferedReader(fr);
        String line;
        
        while ((line = br.readLine())!=null) {
            String[] tokens = line.split("\\s+");
            String key = tokens[0]; // the current word
            WordVec keywvec = wordvecmap.get(key);
            
            String nnlist = tokens[1]; // nnlist (: separated)
            String[] nnwords = nnlist.split(":");
            List<WordVec> nnwvecs = new ArrayList<>(nnwords.length);
            
            for (String nnword: nnwords) {
                WordVec nnwvec = wordvecmap.get(nnword);
                nnwvec.querySim = nnwvec.cosineSim(keywvec);
                nnwvecs.add(nnwvec);
            }
            
            nearestWordVecsMap.put(key, nnwvecs);
        }
        br.close();
        fr.close();
    }

    /**
     * Precomputes a list of nearest neighbors for each vector and saves it. Avoids
     * computing nearest neighbors on the fly; instead performs a table look-up
     * which is much faster.
     * @throws Exception 
     */
    public void computeAndStoreNNs() throws Exception {
        System.out.println("Computing nearest neighbors for " + wordvecmap.size() + " words...");
        
        FileWriter fw = new FileWriter(nnFile);
        BufferedWriter bw = new BufferedWriter(fw);
        
        int count = 0;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            count++;
            
            WordVec wv = entry.getValue();
            bw.write(wv.getWord());
            bw.write(" ");
            
            List<WordVec> nns = getNearestNeighbors(wv.word, k);
            StringBuffer buff = new StringBuffer();
            
            for (WordVec nn: nns) {
                buff.append(nn.getWord()).append(":");
            }
            buff.deleteCharAt(buff.length()-1);
            
            bw.write(buff.toString());
            bw.newLine();
            
            if (count%10000==0)
                System.out.println("Finished for " + count + " words...");
        }
        
        bw.close();
        fw.close();
    }
    
    public List<WordVec> getPrecomputedNearestNeighbors(String queryWord) {
        List<WordVec> nnlist = nearestWordVecsMap.get(queryWord);
        return nnlist.subList(0, Math.min(k, nnlist.size()));
    }

    /** Returns the K nearest neighbors using a query word (that should be contained in the collection).
     * You can access a query sim value in each of the returned WordVec objects. */
    public List<WordVec> getNearestNeighbors(String queryWord, int k) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        WordVec queryVec = wordvecmap.get(queryWord);
        if (queryVec == null) {
            System.err.println("No vec found for word " + queryWord);
            return null;
        }
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            if (wv.word.equals(queryWord))
                continue;
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }

    /** Sequentially computes the distances of every vector from a query
        vector and store the sims in the com.ibm.drl.hbcp.core.wvec object. */
    public List<WordVec> getNearestNeighbors(WordVec queryVec, int k) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }
    
    /**
     * Get the vector given a word from this container class.
     * @param word
     * @return 
     */
    public WordVec getVec(String word) {
        return wordvecmap.get(word);
    }

    /**
     * Retrieves the corresponding vectors given two words and returns the similarity between them.
     * @param u First word
     * @param v Second word
     * @return Similarity between the pair.
     */
    public float getSim(String u, String v) {
        WordVec uVec = wordvecmap.get(u);
        WordVec vVec = wordvecmap.get(v);
        return uVec.cosineSim(vVec);
    }
    
    public static void main(String[] args) {
        try {
            WordVecs qe = new WordVecs("init.properties");
            qe.computeAndStoreNNs();
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
