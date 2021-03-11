/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.wvec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import javax.annotation.Nullable;

import org.apache.commons.lang3.tuple.Pair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
    HashMap<String, List<Pair<WordVec, Double>>> nearestWordVecsMap; // Store the pre-computed NNs
	private static final Logger log = LoggerFactory.getLogger(WordVecs.class);

    /**
     * Initializes the container class from a specified properties file
     * @param propFile
     * @throws IOException 
     */
    public WordVecs(String propFile) throws IOException {
        prop = new Properties();
        try {        	
        	prop.load(new FileReader(propFile));
        } catch(FileNotFoundException fnfe) {
        	prop.load(this.getClass().getClassLoader().getResourceAsStream(propFile));
        }
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
    
    public WordVecs(@Nullable InputStream input) {
        loadFromTextFile(input);        
    }
    
    public WordVecs(@Nullable InputStream input, String delim) {
        loadFromTextFile(input, delim);        
    }
    
    void init(InputStream input, Properties prop) throws IOException {
        this.prop = prop;
        nnFile = prop.getProperty("wordvecs.nn");        
        
        if (wordvecmap != null)
            return; // already loaded from somewhere else in the flow...
        
        k = Integer.parseInt(prop.getProperty("wordvecs.numnearest", "5"));
        
        log.debug("Loading word vecs...");
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
        
        log.info("Loaded word vecs.");
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
        wordvecmap = new HashMap<>();
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

    void loadFromTextFile(InputStream wordvecFile, String delim) {
        wordvecmap = new HashMap<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(wordvecFile))) {
            String line = br.readLine(); // skip the first line
            
            while ((line = br.readLine()) != null) {
                /*
                if (hasDigit(line.split(delim)[0]))
                    continue;
                */
                WordVec wv = new WordVec(line, delim);
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
        	log.warn("No NN file to load NN data.");
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
            List<Pair<WordVec, Double>> nnwvecs = new ArrayList<>(nnwords.length);
            
            for (String nnword: nnwords) {
                WordVec nnwvec = wordvecmap.get(nnword);
                double querySim = nnwvec.cosineSim(keywvec);
                nnwvecs.add(Pair.of(nnwvec, querySim));
            }
            nnwvecs.sort(Comparator.comparing((Pair<WordVec, Double> p) -> p.getValue()).reversed());
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
    	log.debug("Computing nearest neighbors for " + wordvecmap.size() + " words...");
        
        FileWriter fw = new FileWriter(nnFile);
        BufferedWriter bw = new BufferedWriter(fw);
        
        int count = 0;
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            count++;
            
            WordVec wv = entry.getValue();
            bw.write(wv.getWord());
            bw.write(" ");
            
            List<Pair<WordVec, Double>> nns = getNearestNeighbors(wv.word, k);
            StringBuffer buff = new StringBuffer();
            
            for (Pair<WordVec, Double> nn: nns) {
                buff.append(nn.getKey().getWord()).append(":");
            }
            buff.deleteCharAt(buff.length()-1);
            
            bw.write(buff.toString());
            bw.newLine();
            
            if (count%10000==0)
                log.debug("Finished for " + count + " words...");
        }
        
        bw.close();
        fw.close();
    }
    
    public List<Pair<WordVec, Double>> getPrecomputedNearestNeighbors(String queryWord) {
        List<Pair<WordVec, Double>> nnlist = nearestWordVecsMap.get(queryWord);
        return nnlist.subList(0, Math.min(k, nnlist.size()));
    }

    /** Returns the K nearest neighbors using a query word (that should be contained in the collection).
     * You can access a query sim value in each of the returned WordVec objects. */
    public List<Pair<WordVec, Double>> getNearestNeighbors(String queryWord, int k) {
        WordVec queryVec = wordvecmap.get(queryWord);
        if (queryVec == null) {
            System.err.println("No vec found for word " + queryWord);
            return null;
        } else {
            return getNearestNeighbors(queryVec, k);
        }
    }

    /** Sequentially computes the distances of every vector from a query
        vector and store the sims in the com.ibm.drl.hbcp.core.wvec object. */
    public List<Pair<WordVec, Double>> getNearestNeighbors(WordVec queryVec, int k) {
        List<Pair<WordVec, Double>> wordVecsWithSim = new ArrayList<>(wordvecmap.size());
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            double querySim = queryVec.cosineSim(wv);
            wordVecsWithSim.add(Pair.of(wv, querySim));
        }

        wordVecsWithSim.sort(Comparator.comparing((Function<Pair<WordVec, Double>, Double>) Pair::getValue).reversed());
        return wordVecsWithSim.subList(0, Math.min(k, wordVecsWithSim.size()));
    }

    public WordVec zeroVec(String word) {
        String key = wordvecmap.keySet().iterator().next();
        WordVec vec = wordvecmap.get(key);
        int dim = vec.getDimension();
        return new WordVec(word, new double[dim]);
    }
    
    public HashMap<String, WordVec> getWVecMap() { return wordvecmap; }
    
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
