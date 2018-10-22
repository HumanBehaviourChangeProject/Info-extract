/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package wvec;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 *
 * @author Debasis
 */

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
    
    public WordVecs(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));
        init(prop);
    }
    
    public WordVecs(Properties prop) throws Exception {
        init(prop);
    }
    
    void init(Properties prop) throws Exception {
        this.prop = prop;
        nnFile = prop.getProperty("wordvecs.nn");        
        
        if (wordvecmap != null)
            return; // already loaded from somewhere else in the flow...
        
        k = Integer.parseInt(prop.getProperty("wordvecs.numnearest", "5"));
        
        System.out.println("Loading word vecs");
        String loadFrom = prop.getProperty("wordvecs.readfrom");
        if (loadFrom == null)
            return;
        
        if (loadFrom.equals("vec"))
            loadFromTextFile();
        else
            loadObjectFromSerFile();
        
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
    
    void loadFromTextFile() {
        String wordvecFile = prop.getProperty("wordvecs.vecfile");
        wordvecmap = new HashMap();
        try (FileReader fr = new FileReader(wordvecFile);
                BufferedReader br = new BufferedReader(fr)) {
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
    
    void loadObjectFromSerFile() {
        try {
            File serFile = new File(prop.getProperty("wordvecs.objfile"));
            FileInputStream fin = new FileInputStream(serFile);
            ObjectInputStream oin = new ObjectInputStream(fin);
            wordvecmap = (HashMap<String, WordVec>)oin.readObject();
            oin.close();
            fin.close();
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
    
    // init NN info for each word vector
    public void initNN() throws Exception {
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

    // Sequentially compute the distances of every vector from a query
    // vector and store the sims in the wvec object.
    public List<WordVec> getNearestNeighbors(WordVec queryVec) {
        ArrayList<WordVec> distList = new ArrayList<>(wordvecmap.size());
        
        for (Map.Entry<String, WordVec> entry : wordvecmap.entrySet()) {
            WordVec wv = entry.getValue();
            wv.querySim = queryVec.cosineSim(wv);
            distList.add(wv);
        }
        
        Collections.sort(distList);
        return distList.subList(0, Math.min(k, distList.size()));        
    }
    
    public WordVec getVec(String word) {
        return wordvecmap.get(word);
    }

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
