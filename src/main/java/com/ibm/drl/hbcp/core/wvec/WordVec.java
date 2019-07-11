package com.ibm.drl.hbcp.core.wvec;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import org.apache.commons.math3.ml.clustering.Clusterable;
import org.apache.lucene.util.BytesRef;

/**
 * Class for representing word/node vector objects in memory. Provides functionalities for
 * reading from a text-formatted word2vec file
 * (tab separated file of word/id followed by space separated component values).
 * Also contains utility functions for computing the nearest neighbors. 
 *
 * @author dganguly
 */

public class WordVec implements Comparable<WordVec>, Serializable, Clusterable {
    String word;
    float[] vec;
    float norm;  // L2 norm
    float querySim; // distance from a reference query point
    transient boolean isComposed;
    int clusterId;

    public static final String COMPOSING_DELIM = ":";
    
    public WordVec(int dimension) { vec = new float[dimension]; }
    
    /**
     * Constructs a word vector from the content of a text-formatted word2vec (C code) output.
     * @param line 
     */
    public WordVec(String line) {
        String[] tokens = line.split("\\s+");
        word = tokens[0];
        vec = new float[tokens.length-1];
        for (int i = 1; i < tokens.length; i++)
            vec[i-1] = Float.parseFloat(tokens[i]);
    }
    
    /**
     * Constructs this object given a word and its corresponding vector in the form of an array of doubles.
     * @param word
     * @param vec 
     */
    public WordVec(String word, double[] vec) {
        this.word = word;
        float[] fvec = new float[vec.length];
        for (int i=0; i<vec.length; i++)
            fvec[i] = (float)vec[i];
        
        this.vec = fvec;
    }
    
    /**
     * L2-normalizes this vector.
     */
    public void normalize() {
        norm = getNorm(); // compute norm if not already done
        for (int i=0; i<vec.length; i++) {
            vec[i] = vec[i]/norm;
        }
    }
    
    public boolean isComposed() { return isComposed; }
    
    public void setClusterId(int clusterId) { this.clusterId = clusterId; }
    
    public int getClusterId() { return clusterId; }
    
    /** Computes the length (L2 norm) of this vector */
    public float getNorm() {
        if (norm > 0)
            return norm;
        
        // calculate and store
        float sum = 0;
        for (int i = 0; i < vec.length; i++) {
            sum += vec[i]*vec[i];
        }
        norm = (float)Math.sqrt(sum);
        return norm;
    }
    
    /**
     * Returns the dimension of this vector.
     * @return 
     */
    public int getDimension() { return this.vec.length; }
    
    /**
     * Computes the sum (composes) two given vectors.
     * @param a
     * @param b
     * @return The sum vector
     */
    static public WordVec sum(WordVec a, WordVec b) {
        WordVec sum = new WordVec(a.vec.length);
        sum.word = a.word + COMPOSING_DELIM + b.word;
        for (int i = 0; i < a.vec.length; i++) {
            sum.vec[i] = (a.vec[i] + b.vec[i]);
        }
        sum.isComposed = true;
        return sum;
    }
    
    /**
     * Computes the cosine similarity between two vectors
     * @param that Another vector with which to compute the similarity of this one.
     * @return Similarity value in the range of [-1, 1]
     */
    public float cosineSim(WordVec that) {
        float sum = 0;
        for (int i = 0; i < this.vec.length; i++) {
            sum += vec[i] * that.vec[i];
        }        
        return sum/(this.getNorm()*that.getNorm());
    }
    
    /**
     * Computes L2 distance between two vectors (this and another one).
     * @param that The reference vector with which distance is to be measured.
     * @return A distance value (note that this is not bounded like the similarity value). Could be any positive number.
     */
    public float euclideanDist(WordVec that) {
        float sum = 0;
        for (int i = 0; i < this.vec.length; i++) {
            sum += (vec[i] - that.vec[i]) * ((vec[i] - that.vec[i]));
        }
        return (float)Math.sqrt(sum);
    }

    public String getWord() { return word; }
    public void setWord(String name) { this.word = name; }
    
    public float getQuerySim() { return querySim; }
    
    /**
     * Comparator function to help sorting (descending order) by the similarities.
     * @param that
     * @return 
     */
    @Override
    public int compareTo(WordVec that) {
        return this.querySim > that.querySim? -1 : this.querySim == that.querySim? 0 : 1;
    }
    
    /**
     * Multiplies this vector with a scalar. Helpful for normalization.
     * @param alpha 
     */
    public void scalarMutiply(float alpha) {
        for (int i=0; i < vec.length; i++)
            vec[i] = vec[i]*alpha;
    }
    
    /**
     * Useful to serialize this object as a byte stream.
     * @return An array of bytes for this object.
     * @throws IOException 
     */
    public byte[] getBytes() throws IOException {
        byte[] byteArray;
        try (ByteArrayOutputStream bos = new ByteArrayOutputStream()) {
            ObjectOutput out;
            out = new ObjectOutputStream(bos);
            out.writeObject(this);
            byteArray = bos.toByteArray();
            out.close();
        }
        return byteArray;
    }
    
    /**
     * Helps reconstructing this vector object from a byte array.
     * @param bytes
     * @return
     * @throws Exception 
     */
    public static WordVec decodeFromByteArray(BytesRef bytes) throws Exception {
        ObjectInput in;
        Object o;
        try (ByteArrayInputStream bis = new ByteArrayInputStream(bytes.bytes)) {
            in = new ObjectInputStream(bis);
            o = in.readObject();
        }
        in.close();
        return (WordVec)o;
    }

    /**
     * Get the array of doubles for this vector object.
     * @return 
     */
    @Override
    public double[] getPoint() {
        double[] vec = new double[this.vec.length];
        for (int i=0; i<this.vec.length; i++)
            vec[i] = this.vec[i];
        return vec;
    }
    
    /**
     * String repreentation is the word followed by the vector.
     * @return 
     */
    @Override
    public String toString() {
        StringBuffer buff = new StringBuffer(word);
        buff.append(" ");
        for (double d : this.vec) {
            buff.append(d).append(" ");
        }
        return buff.toString();
    }
    
    /**
     * Returns the vector (array of numbers) as a space separated string of the component values.
     * @return 
     */
    public String getVecStr() {
        StringBuffer buff = new StringBuffer();
        for (double d : this.vec) {
            buff.append(d).append(" ");
        }
        return buff.toString();
    }
}

