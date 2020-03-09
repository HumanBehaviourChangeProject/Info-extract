package com.ibm.drl.hbcp.predictor.graph;

import com.google.common.primitives.UnsignedInteger;
import com.google.common.primitives.UnsignedLong;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.core.wvec.WordVecs;
import org.apache.commons.io.IOUtils;

import java.io.*;
import static java.lang.System.exit;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Properties;
import java.util.Random;

/**
 * A Java port of node2vec.c
 *
 * node2vec is an algorithmic framework for representational learning on graphs.
 * Given any graph, it can learn continuous feature representations for the nodes,
 * which can then be used for various downstream machine learning tasks.
 *
 * Concretely, given a graph, node2vec produces vector representations for each node.
 *
 * // Copyright 2013 Google Inc. All Rights Reserved.
 * //
 * //  Licensed under the Apache License, Version 2.0 (the "License");
 * //  you may not use this file except in compliance with the License.
 * //  You may obtain a copy of the License at
 * //
 * //      http://www.apache.org/licenses/LICENSE-2.0
 * //
 * //  Unless required by applicable law or agreed to in writing, software
 * //  distributed under the License is distributed on an "AS IS" BASIS,
 * //  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * //  See the License for the specific language governing permissions and
 * //  limitations under the License.
 *
 * @author marting, debasis
 * */

/*
Translation notes:
Here is the type mapping I tried first:

typedef float real -> float
typedef char byte -> boolean if it represents a 1/0 (usually used for booleans)
char* -> String
anything* -> Anything or Anything[] (TODO: see if List<Anything> are better)


THERE IS A BUG in the C code: l 327-328
    should be if (!directed) then add the reverse edge

Confirmed that:
    RNG is preserved
    Word hash is preserved
    'float' should be equivalent in Java and C

charlesj 6/11/2019:
   RNG is changed to Java Random
 */

// Refer to the node2vec paper --- consider moving from t,v and then to x
enum VisitStatus {
    CASE_P,
    CASE_ONE,
    CASE_Q
}

public class Node2Vec {

    public static final int EXP_TABLE_SIZE = 1000;
    public static final int MAX_EXP = 6;
    public static final int MAX_LINE_SIZE = 10000;
    public static final int MAX_SENTENCE_LENGTH = 1000;
    public static final int MAX_CODE_LENGTH = 40;
    public static final int MAX_OUT_DEGREE = 20000;
    public static final int MAX_CONTEXT_PATH_LEN = 100;

    public static final int vocab_hash_size = 300000;  // Maximum 30 * 0.7 = 21M words in the vocabulary
    private static final int MAX_SKIPPED_EDGES = 7;  // max skipped edges from subsampling in pq-sampling
    private Random rand;

    public static class edge {
        public vocab_node dest; //struct vocab_node* dest;
        public float weight;
        public boolean twohop; // 1 if two-hop;
    }

    edge[][] multiHopEdgeLists;
    float p1, q1;

    // represents a node structure
    public static class vocab_node {
        public int id; // the id (hash index) of the word
        // TODO: char* originally, see if String fits
        public String word;
        //TODO: edge *edge_list;
        public edge[] edge_list = null;
        public int cn; // out degree
        public boolean visited;
    }

    InputStream train_file;
    OutputStream output_file, output_file_vec;
    vocab_node[] vocab;
    int debug_mode = 2; int window = 10; int min_count = 0;
    double sample = 1e-3;
    boolean pqsampling;
    int[] vocab_hash; //int *vocab_hash;
    int vocab_max_size = 1000; int vocab_size = 0; int layer1_size = 100;
    int train_nodes = 0; int iter = 5; boolean directed = true;
    float alpha = 0.025f;
    // TODO: last one might be a pointer to a cell in 'syn0' (in this case, turn to int)
    float[] syn0, syn1, syn1neg, expTable, pt_syn0;
    float onehop_pref = 0.7f;
    float one_minus_onehop_pref;
    int negative = 5;
    final int table_size = (int)1e8;
    int[] table;
    InputStream pretrained_file;
    // TODO: probably a pointer to a byte array
    //char* pt_word_buff;
    //int pt_word_buff;
    long pt_vocab_words = 0;
    WordVecs ptWordVecs; // pre-trained word vectors

    Properties props;

    public Node2Vec() {
        rand = new Random(123456L);
    }

    public Node2Vec(Graph graph, Properties props) {
        this();
        this.props = props;
        setInput(IOUtils.toInputStream(graph.toText(), StandardCharsets.UTF_8));
        
        readParameters();
    }
    
    //+++DG-ENHANCEMENT: Graph should be a property of the NodeVecs (container of Node2vec)
    // load the graph nodes from the specified path in properties
    public Node2Vec(Properties prop) throws IOException {
        this.props = props;
        Graph graph = AttribNodeRelations.fromFile(new File(props.getProperty("prediction.graph.loadfrom")));
        setInput(IOUtils.toInputStream(graph.toString(), StandardCharsets.UTF_8));
        
        readParameters();
    }

    final void readParameters() {
        layer1_size = Integer.parseInt(props.getProperty("node2vec.layer1_size", "128"));
        onehop_pref = Float.parseFloat(props.getProperty("node2vec.onehop_pref", "0.7"));
        alpha = Float.parseFloat(props.getProperty("node2vec.alpha", "0.025"));
        directed = Boolean.parseBoolean(props.getProperty("node2vec.directed", "true"));
        window = Integer.parseInt(props.getProperty("node2vec.window", "5"));
        sample = Double.parseDouble(props.getProperty("node2vec.sample", "0"));
        negative = Integer.parseInt(props.getProperty("node2vec.ns", "10"));
        iter = Integer.parseInt(props.getProperty("node2vec.niters", "10"));
        pqsampling = Boolean.parseBoolean(props.getProperty("node2vec.pqsampling", "true"));
        min_count = Integer.parseInt(props.getProperty("node2vec.mincount", "1"));
        p1 = Float.parseFloat(props.getProperty("node2vec.p1", "0.5"));
        q1 = Float.parseFloat(props.getProperty("node2vec.q1", "0.5"));
        
        try {
            String ptFile = props.getProperty("node2vec.ptfile");
            if (ptFile != null)
                pretrained_file = new FileInputStream(ptFile);
        }
        catch (IOException ex) {
            ex.printStackTrace();
        }
    }
    
    public void setInput(InputStream input) {
        train_file = input;
    }

    public void setOutput(OutputStream output) {
        output_file = output;
    }

    void InitUnigramTable() {
        int a, i;
        int train_nodes_pow = 0;
        float d1;
        float power = 0.75f;
        table = new int[table_size];
        for (a = 0; a < vocab_size; a++) train_nodes_pow += Math.pow(vocab[a].cn, power);
        i = 0;
        d1 = (float)Math.pow(vocab[i].cn, power) / (float)train_nodes_pow;
        for (a = 0; a < table_size; a++) {
            table[a] = i;
            if (a / (float)table_size > d1) {
                i++;
                d1 += Math.pow(vocab[i].cn, power) / (float)train_nodes_pow;
            }
            if (i >= vocab_size) i = vocab_size - 1;
        }
    }

    // Reads the node id from a file... stops reading when it sees a tab character.
    // Each line in the graph file is: <src node>\t<dest-node>\t<weight>
    // marting: returns true when a line has been read, false if EOF
    boolean ReadSrcNode(BufferedReader fin) throws IOException {
        String node_id1, node_id2;
        float f;
        int a, i;

        String line = fin.readLine();
        if (line == null) return false;
        String[] split = line.split("\\t");
        assert split.length >= 3 : "Wrong format, didn't match expected: <src node>\\t<dest-node>\\t<weight>\\t<etc...>";
        node_id1 = split[0];
        node_id2 = split[1];
        //f = Float.parseFloat(split[2]); // not used here
        String[] node_ids = { node_id1, node_id2 };
        for (String node_id : node_ids) {
            i = SearchVocab(node_id);
            if (i == -1) {
                a = AddWordToVocab(node_id);
                vocab[a].cn = 1;
            }
            else vocab[i].cn++;
        }
        return true;
    }

    // Returns hash value of a word (tested to be exactly the same as in C)
    int GetWordHash(String word) {
        UnsignedInteger hash = UnsignedInteger.valueOf(0);
        for (int a = 0; a < word.length(); a++) {
            hash = hash.times(UnsignedInteger.valueOf(257)).plus(UnsignedInteger.valueOf(word.charAt(a)));
        }
        hash = hash.mod(UnsignedInteger.valueOf(vocab_hash_size));
        return hash.intValue();
    }

    // Returns position of a word in the vocabulary; if the word is not found, returns -1
    int SearchVocab(String word) {
        int hash = GetWordHash(word);
        while (true) {
            if (vocab_hash[hash] == -1) return -1;
            if (word.equals(vocab[vocab_hash[hash]].word)) return vocab_hash[hash];
            hash = (hash + 1) % vocab_hash_size;
        }
    }

    // Adds a word to the vocabulary
    int AddWordToVocab(String word) {
        int hash, id;
        vocab[vocab_size] = new vocab_node();
        vocab[vocab_size].word = word;
        vocab[vocab_size].cn = 0;
        vocab[vocab_size].visited = false;

        vocab_size++;
        // Reallocate memory if needed
        if (vocab_size + 2 >= vocab_max_size) {
            vocab_max_size += 1000;
            vocab = Arrays.copyOf(vocab, vocab_max_size);
        }
        hash = GetWordHash(word);
        while (vocab_hash[hash] != -1) hash = (hash + 1) % vocab_hash_size;

        id = vocab_size - 1;
        vocab_hash[hash] = id;
        // vocab_size-1 is the index of the current word... save it in the node object
        vocab[id].id = id;
        //printf("\n%s Adding word",word);
        return id;
    }

    // Used later for sorting by out degrees
    int VocabCompare(vocab_node a, vocab_node b) {
        if (b.cn != a.cn)
            return b.cn - a.cn;
        else return a.word.compareTo(b.word);
    }

    // Sorts the vocabulary by frequency using word counts
    void SortVocab() {
        int a, size;
        int hash;
        // Sort the vocabulary and keep </s> at the first position
        // in C, qsort uses the count as second parameter, in Java, sort uses the upper bound (excluded)
        //qsort(&vocab[1], vocab_size - 1, sizeof(struct vocab_node), VocabCompare);
        Arrays.sort(vocab, 1, vocab_size, new Comparator<vocab_node>() {
            @Override
            public int compare(vocab_node o1, vocab_node o2) {
                return VocabCompare(o1, o2);
            }
        });
        for (a = 0; a < vocab_hash_size; a++) vocab_hash[a] = -1;
        size = vocab_size;
        train_nodes = 0;
        for (a = 0; a < size; a++) {
            // Nodes with out-degree less than min_count times will be discarded from the vocab
            if ((vocab[a].cn < min_count) && (a != 0)) {
                vocab_size--;
                vocab[a] = null;
            } else {
                // Hash will be re-computed, as after the sorting it is not actual
                hash=GetWordHash(vocab[a].word);
                while (vocab_hash[hash] != -1) hash = (hash + 1) % vocab_hash_size;
                vocab_hash[hash] = a;
                train_nodes += vocab[a].cn;
            }
        }
        vocab = Arrays.copyOf(vocab, vocab_size + 1);
    }

    // Stores a list of pointers to edges for each source node.
    // The overall list is thus a pointer to pointer to the lists.
    void initContexts() {
        // a list of contexts for each source node in the graph
        multiHopEdgeLists = new edge[vocab_size][];
        for (int i = 0; i < vocab_size; i++) {
            multiHopEdgeLists[i] = new edge[MAX_CONTEXT_PATH_LEN + 1]; // +1 for the NULL termination
        }
    }

    boolean addEdge(String src, String dest, float wt) {
        int src_node_index, dst_node_index, cn;
        edge[] edge_list;

        // Get src node id
        src_node_index = SearchVocab(src);
        if (src_node_index == -1) {
            System.out.println(String.format("Word '%s' OOV...", src));
            return false;
        }

        // Get dst node id
        dst_node_index = SearchVocab(dest);
        if (dst_node_index == -1) {
            System.out.println(String.format("Word '%s' OOV...", dest));
            return false;
        }

        // allocate edges
        edge_list = vocab[src_node_index].edge_list;
        if (edge_list == null) {
            edge_list = new edge[MAX_OUT_DEGREE];
            cn = 0;
        }
        else {
            cn = vocab[src_node_index].cn; // current number of edges
        }

        if (cn == MAX_OUT_DEGREE) {
            System.err.println("Can't add anymore edges...");
            return false;
        }
        edge_list[cn] = new edge();
        edge_list[cn].dest = vocab[dst_node_index];
        edge_list[cn].dest.id = dst_node_index;
        edge_list[cn].weight = wt;
        vocab[src_node_index].edge_list = edge_list;

        vocab[src_node_index].cn = cn + 1; // number of edges
        
        assert(vocab[src_node_index].edge_list != null);
        
        // +++DG: Test-case fix... Can't check this now... Have to defer it
        // in the calling function... currently, only checking for source not null
        //assert(vocab[dst_node_index].edge_list != null);
        // ---DG
        
        return true;
    }

    // Each line represents an edge...
    // format is <src-node-id> \t <dest-node-id> \t <weight>
    // supports the option of directed/undirected...
    // for undirected option, the function adds the reverse edges
    void constructGraph(BufferedReader fp) throws IOException {
        int i, count = 0;
        String src_word, dst_word, wt_word;
        float wt;

        if (debug_mode > 2)
            System.out.println("Reading edges from each line...");
        String line;
        while ((line = fp.readLine()) != null) {

            String[] split = line.split("\\t");
            src_word = split[0];
            dst_word = split[1];
            wt_word = split[2];
            wt = Float.parseFloat(wt_word);

            if (!addEdge(src_word, dst_word, wt))
                continue;  // add this edge to G

            if (!directed) {
                if (!addEdge(dst_word, src_word, wt))
                    continue;
            }

            count++;
            if (debug_mode > 3)
                System.out.println(String.format("Read line %d", count));
        }
    }

    // an important step is to normalize the edge weights to probabilities
    // of samples that would be used later on during sampling nodes
    // from this pre-built context.
    void preComputePathContextForSrcNode(int src_node_index) {
        int i = 0, j, num_one_hops;  // index into the context buffer
        edge q;
        edge[] multiHopEdgeList;
        vocab_node src_node;

        src_node = vocab[src_node_index];
        multiHopEdgeList = multiHopEdgeLists[src_node_index]; // write to the correct buffer

        // First, collect a set of one hop nodes from this source node
        for (int pIndex = 0; pIndex < src_node.cn; pIndex++) {
            edge p = src_node.edge_list[pIndex];
            // visit a one-hop node from source
            if (!p.dest.visited && i < MAX_CONTEXT_PATH_LEN) {
                multiHopEdgeList[i++] = p;
                p.twohop = false;
                p.dest.visited = true;
            }
        }
        num_one_hops = i;

        // iterate over the one hops collected to reach the 2 hops (that are not one-hop connections)
        for (j = 0; j < num_one_hops; j++) {
            q = multiHopEdgeList[j];
            if (!q.dest.visited && q.dest != src_node && i < MAX_CONTEXT_PATH_LEN) { // q->dest != src_node avoids cycles!
                multiHopEdgeList[i++] = q;
                q.twohop = true;
                q.dest.visited = true;
            }
        }

        // TODO: probably not required in Java
        multiHopEdgeList[i] = null;  // terminate with a NULL

        // reset the visited flags (for next call to the function)
        for (j = 0; j < i; j++) {
            multiHopEdgeList[j].weight *= multiHopEdgeList[j].twohop ? one_minus_onehop_pref : onehop_pref;  // prob of one-hop vs two-hop
            multiHopEdgeList[j].dest.visited = false;
        }
    }

    // Precompute the set of max-hop nodes for each source node.
    void preComputePathContexts() {
        initContexts();

        if (debug_mode > 2)
            System.out.println("Initialized contexts...");

        for (int i = 0; i < vocab_size; i++) {
            preComputePathContextForSrcNode(i);
            if (debug_mode > 3)
                System.out.println(String.format("Precomputed contexts for node %d (%s)", i, vocab[i].word));
        }
    }

    // Sample a context of size <window>
    // contextBuff is an o/p parameter
    int adjSampling(int src_node_index, edge[] contextBuff) {
        edge[] multiHopEdgeList;
        int len = MAX_CONTEXT_PATH_LEN;
        float x, cumul_p, z, norm_wt;

        // see how many 2-hop adj neighbors we have got for this node

        multiHopEdgeList = multiHopEdgeLists[src_node_index]; // buffer to sample from
        int pIndex;
        for (pIndex = 0; pIndex < MAX_CONTEXT_PATH_LEN; pIndex++) {
            edge p = multiHopEdgeList[pIndex];
            if (p == null) break;
        }
        len = pIndex;
        if (debug_mode > 2)
            System.out.println(String.format("#nodes in 2-hop neighborhood = %d", len));

        // TODO: I don't understand this memset here, we intend to fill the first 'window' cells of contextBuff
        // and 'window' is always >= 'len', so why bother with this memset?
        Arrays.fill(contextBuff, 0, len, null);
        //memset(contextBuff, 0, sizeof(edge*)*len);

        // normalize the weights so that they sum to 1;
        z = 0;
        for (int i = 0; i < len; i++) {
            edge p = multiHopEdgeList[i];
            z += p.weight;
        }

        if (debug_mode > 2)
            System.out.print("Sampled context: ");

        int j = 0;
        for (int i = 0; i < window; i++) {  // draw 'window' samples

            x= rand.nextFloat();
            cumul_p = 0;

            // Find out in which interval does this belong to...
            for (pIndex = 0; pIndex < len; pIndex++) {
                edge p = multiHopEdgeList[pIndex];
                norm_wt = p.weight / z;
                if (cumul_p <= x && x < cumul_p + norm_wt)
                    break;
                cumul_p += norm_wt;
            }

            // The subsampling randomly discards frequent words while keeping the ranking same
            if (sample <= 0 || keepEdge(multiHopEdgeList[pIndex])) {  // sample==0 means subsampling is off
                // save sampled nodes in context
                contextBuff[j++] = multiHopEdgeList[pIndex];
                if (debug_mode > 2)
                    System.out.print(String.format("%s ", vocab[multiHopEdgeList[pIndex].dest.id].word));
            } else {
                if (debug_mode > 2)
                    System.out.print(String.format("Ignore edge to %s (with subsampling)", vocab[multiHopEdgeList[pIndex].dest.id].word));
            }
        }
        if (debug_mode > 2) System.out.println();;
        return j;
    }

    private boolean keepEdge(edge edge) {
        // this corresponds to subsample threshold in word2vec (in TrainModelThread)
        // less frequent nodes give higher value for 'ran' here and more likely to be kept in the subsample
        double ran = (Math.sqrt(edge.dest.cn / (sample * train_nodes)) + 1) * (sample * train_nodes) / edge.dest.cn;
        return (ran >= rand.nextFloat());
    }

    // Each line in this graph file is of the following format:
    // <src-node-id>\t [<dest-node-id>:<weight of this edge> ]*
    void LearnVocabFromTrainFile() throws IOException {
        // read the input stream once and save the output (this will used a stream any time you need it consequently)
        byte[] trainFile = IOUtils.toByteArray(train_file);
        // first build the vocab
        //TODO: the original was using mode 'rb', why? fin = fopen(train_file, "rb");
        try (BufferedReader fin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(trainFile)))) {
            if (debug_mode > 2)
                System.out.println("Loading nodes from graph file...");

            System.out.println(vocab_hash_size);
            for (int a = 0; a < vocab_hash_size; a++) vocab_hash[a] = -1;
            vocab_size = 0;

            while (ReadSrcNode(fin)) ;

            SortVocab();
            if (debug_mode > 2) {
                System.out.println(String.format("#nodes: %d", vocab_size));
            }
        }
        // then build the actual graph
        try (BufferedReader fin = new BufferedReader(new InputStreamReader(new ByteArrayInputStream(trainFile)))) {
            constructGraph(fin);
        }

        if (debug_mode > 2)
            System.out.println("Loaded graph in memory...");

        if (!pqsampling) {
            preComputePathContexts();
            if (debug_mode > 2)
                System.out.println("Successfully initialized path contexts");
        }
    }
    
    void InitNet() {
        pt_vocab_words = 0;
        syn0 = new float[vocab_size * layer1_size];

        if (negative > 0) {
            syn1neg = new float[vocab_size * layer1_size];
        }

        if (ptWordVecs != null) {
            // Initialize the net weights from a pretrained model
            // The file to be loaded is a binary file saved by word2vec.
            // This is to ensure that the word vectors will not be trained
            //TODO: Not for CIKM paper...  
        }
        else {
            // Random initialization in absence of pre-trained file
            for (int a = 0; a < vocab_size; a++) {
                for (int b = 0; b < layer1_size; b++) {
                    syn0[a * layer1_size + b] = (rand.nextFloat() - 0.5f) / layer1_size;//Initialize by random nos
                }
            }
        }
    }
    
    //+++DG: Added the functionality for p-q sampling
    int pqSampling(int src_node_index, edge[] contextBuff) {
        float x, cumul_p, norm_wt;
        float z = 0.0f;
        vocab_node src_node, next_node = null, prev_node = null;
        edge p = null, q = null;
        int p_index;
        VisitStatus status;
        float delw;
        int j = 0;

        src_node = vocab[src_node_index];
        next_node = src_node;
        prev_node = src_node;

        // bad sample parameter could make it hard to fill contextBuff, so we limit the number of consecutive times
        //  an edge is skipped
        int skippedCounter = 0;
        while (j < window) {
            if (skippedCounter == 0) { // if previous node was skipped with subsampling we can use old z
                // normalize the weights so that they sum to 1;
                z = 0.0f;
                for (p_index = 0; p_index < next_node.cn; p_index++) {
                    if (next_node == null || next_node.edge_list == null)
                        next_node = next_node;

                    p = next_node.edge_list[p_index];
                    status = checkNeighbour(prev_node, vocab[p.dest.id]);
                    delw = status == VisitStatus.CASE_P ? p1 : status == VisitStatus.CASE_ONE ? 1 : q1;
                    z += p.weight * delw;
                }
            }

            x = rand.nextFloat();

            cumul_p = 0;
            for (p_index=0; p_index < next_node.cn; p_index++) {
                p = next_node.edge_list[p_index];
                status = checkNeighbour(prev_node, vocab[p.dest.id]);
                delw = status == VisitStatus.CASE_P? p1 : status == VisitStatus.CASE_ONE? 1 : q1;
                norm_wt = p.weight*delw/z;
                if (cumul_p <= x && x < cumul_p + norm_wt) {
                    break;
                }
                cumul_p += norm_wt;
                q = p;
            }

            edge candidateEdge = p_index < next_node.cn ? p : q;
            if (sample <= 0 || j == 0 || skippedCounter > MAX_SKIPPED_EDGES ||   //  always add first edge
                    keepEdge(candidateEdge)) {  // sample==0 means subsampling is off
                contextBuff[j++] = candidateEdge;
                prev_node = next_node;
                next_node = p_index < next_node.cn ? vocab[p.dest.id] : vocab[q.dest.id];
                skippedCounter = 0;
            } else {
                if (skippedCounter == MAX_SKIPPED_EDGES)
                    System.err.println(MAX_SKIPPED_EDGES + " edges were skipped with subsampling.  Consider tuning 'sample' parameter.");
                skippedCounter++;
            }
        }
        return j;
    }
    
    VisitStatus checkNeighbour(vocab_node prev_node, vocab_node current_node) {
        edge p;
        int p_index;
        
        if (prev_node.id == current_node.id) {
            return VisitStatus.CASE_Q;
        }

        for (p_index=0; p_index < prev_node.cn; p_index++) {
            p = prev_node.edge_list[p_index];
            if (vocab[p.dest.id].id == current_node.id) {
                return VisitStatus.CASE_ONE;
            }
        }
        return VisitStatus.CASE_P;
    }
    //---DG

    void skipgram() {
        int last_word;
        int l1, l2, target, label;
        int context_len;
        edge[] contextBuff = new edge[MAX_CONTEXT_PATH_LEN];
        float f, g;
        float[] neu1e = new float[layer1_size];

        for (int word = 0; word < vocab_size; word++) {
            if (debug_mode > 2) {
                System.out.println(String.format("Skip-gram iteration for source word %s", vocab[word].word));
                System.out.println("Word occurs " + vocab[word].cn + " times");
            }

            // context sampled for each node
            context_len = !pqsampling?
                adjSampling(word, contextBuff):
                pqSampling(word, contextBuff);

            // train skip-gram on node contexts
            for (int a = 0; a < context_len; a++) {
                edge p = contextBuff[a];
                // TODO: how is the first case possible?
                if (p == null || p.dest == null) {
                    continue;
                }

                last_word = p.dest.id;
                l1 = last_word * layer1_size;

                //memset(neu1e, 0, layer1_size * sizeof(real));
                // TODO: technically this corresponds more closely to Arrays.fill, not sure this is needed performance-wise
                neu1e = new float[layer1_size];

                // NEGATIVE SAMPLING
                if (negative > 0)
                    for (int d = 0; d < negative + 1; d++) {
                        if (d == 0) {
                            target = word;
                            label = 1;
                        } else {
                            UnsignedLong next_random = UnsignedLong.valueOf(Math.abs(rand.nextLong()));
                            target = table[next_random.bigIntegerValue().shiftRight(16).mod(BigInteger.valueOf(table_size)).intValue()];
                            if (target == 0) target = next_random.bigIntegerValue().mod(BigInteger.valueOf(vocab_size - 1)).intValue() + 1;
                            if (target == word) continue;
                            label = 0;
                        }
                        l2 = target * layer1_size;
                        f = 0;
                        for (int c = 0; c < layer1_size; c++) f += syn0[c + l1] * syn1neg[c + l2];
                        // compute gradient
                        if (f > MAX_EXP) g = (label - 1) * alpha;
                        else if (f < -MAX_EXP) g = (label - 0) * alpha;
                        else g = (label - expTable[(int)((f + MAX_EXP) * (EXP_TABLE_SIZE / MAX_EXP / 2))]) * alpha;

                        for (int c = 0; c < layer1_size; c++) neu1e[c] += g * syn1neg[c + l2];
                        for (int c = 0; c < layer1_size; c++) syn1neg[c + l2] += g * syn0[c + l1];
                    }

                // Learn weights input -> hidden
                for (int c = 0; c < layer1_size; c++) syn0[c + l1] += neu1e[c];

            }
        }
        if (debug_mode > 2)
            System.out.println("Skipgram training done...");
    }

    public boolean train() throws IOException {
        
        if (pretrained_file != null) {
            ptWordVecs = new WordVecs(pretrained_file, props);
        }
        
        System.out.println("Starting training using input graph.");
        LearnVocabFromTrainFile();

        if (output_file == null) { System.err.println("Graph file not found"); return false; }
        InitNet();

        if (negative > 0) InitUnigramTable();
        System.out.println("Unigram table initialized...");

        for (int i=0; i < iter; i++) {
            skipgram();
            System.out.println("Iteration " + i + " done...");            
        }

        // TODO: the C code was outputting 2 files, binary and string, we only to string
        //output_file_vec = String.format("%s.vec", output_file);
        output_file_vec = output_file;

        // TODO: these files were open in mode 'wb' but this shouldn't be needed in Java
        // marting: TODO: also I didn't handle the binary version, likely not needed for us
        try (BufferedWriter fo = new BufferedWriter(new OutputStreamWriter(output_file_vec))) {
            // Save the word vectors
            fo.write(String.format("%d %d\n", (long)vocab_size + pt_vocab_words, layer1_size));
            for (int a = 0; a < vocab_size; a++) {
                fo.write(String.format("%s ", vocab[a].word));
                for (int b = 0; b < layer1_size; b++)
                    fo.write(String.format("%f ", syn0[a * layer1_size + b]));
                fo.newLine();
            }
            
            /*
            TODO: not sure what's supposed to happen here but I think it's tied with something I ignored above
            TODO: in the original pt_word_buff appeared first in l 569
            // write out the pt_syn0 vecs as well
            for (a = 0; a < pt_vocab_words; a++) {
            fprintf(fo, "%s ", &pt_word_buff[a*MAX_STRING]);
            fprintf(fo2, "%s ", &pt_word_buff[a*MAX_STRING]);
                fwrite(&pt_syn0[a * layer1_size], sizeof(real), layer1_size, fo);

                for (b = 0; b < layer1_size; b++) fprintf(fo2, "%lf ", pt_syn0[a * layer1_size + b]);

            fprintf(fo, "\n");
            fprintf(fo2, "\n");
            }
            */
            
        }
        return true;
    }

    int ArgPos(String str, int argc, String[] args) {
        for (int a = 0; a < argc; a++) if (args[a].equals(str)) {
            if (a == argc - 1) {
                System.out.println(String.format("Argument missing for %s", str));
                exit(1);
            }
            return a;
        }
        return -1;
    }

    /**
     * Runs the node2vec algorithm on the input graph and returns the collection of learned vectors.
     * @throws IOException should actually never be thrown as everything happens in-memory, this is an artifact of the port
     */
    public NodeVecs getNodeVectors() throws IOException {
        String[] args = { "-output prediction/graphs/nodevecs/refVecs.vec" }; // just to make the main believe the args are not empty... always dump the output to a file
        setOutput(new ByteArrayOutputStream());
        if (run(args.length, args)) {
            // output_file should now contain the output of Node2Vec, which we read in a byte array
            byte[] output = ((ByteArrayOutputStream)output_file).toByteArray();
            // we feed this back to the class building the in-memory word vectors
            return new NodeVecs(new ByteArrayInputStream(output), props);
        } else return null;
    }

    public NodeVecs trainSaveAndGetNodeVecs(String outputFile) throws IOException {
        String[] args = { "-dummyArg" }; // just to make the main believe the args are not empty
        FileOutputStream fostream = new FileOutputStream(outputFile);
        setOutput(fostream);
        run(args.length, args);
        
        // we feed this back to the class building the in-memory word vectors
        return new NodeVecs(new FileInputStream(outputFile), props);
    }

    /** Main program as originally used in command line */
    public boolean run(int argc, String[] argv) throws IOException {
        int i;
        if (argc == 0) {
            System.out.print("Node2Vec toolkit v 0.1c\n\n");
            System.out.print("Options:\n");
            System.out.print("Parameters for training:\n");
            System.out.print("\t-train <file>\n");
            System.out.print("\t\tGraph file (each line a node: <node-id> \t [<node-id>:<weight>]*)\n");
            System.out.print("\t-pt <file>\n");
            System.out.print("\t\tPre-trained vectors for nodes (word2vec bin file format)\n");
            System.out.print("\t-output <file>\n");
            System.out.print("\t\tUse <file> to save the resulting word vectors / word clusters\n");
            System.out.print("\t-size <int>\n");
            System.out.print("\t\tSet size of word vectors; default is 100\n");
            System.out.print("\t-window <int>\n");
            System.out.print("\t\tContext (random walk) length.\n");
            System.out.print("\t-negative <int>\n");
            System.out.print("\t\tNumber of negative examples; default is 5, common values are 3 - 10 (0 = not used)\n");
            System.out.print("\t-iter <int>\n");
            System.out.print("\t\tRun more training iterations (default 5)\n");
            System.out.print("\t-min-count <int>\n");
            System.out.print("\t\tNodes with out-degree less than min-count are discarded; default is 5\n");
            System.out.print("\t-alpha <float>\n");
            System.out.print("\t\tSet the starting learning rate; default is 0.025 for skip-gram\n");
            System.out.print("\t-directed <0/1>\n");
            System.out.print("\t\twhether the graph is directed (if undirected, reverse edges are automatically added when the i/p fmt is edge list>\n");
            System.out.print("\nExample:\n");
            System.out.print("./node2vec -pt ptnodes.vec -train graph.txt -output ovec -size 200 -window 5 -sample 1e-4 -negative 5 -iter 3\n\n");
            return false;
        }
        
        if ((i = ArgPos("-size", argc, argv)) >= 0) layer1_size = Integer.parseInt(argv[i + 1]);
        if ((i = ArgPos("-onehop_pref", argc, argv)) > 0) onehop_pref = Float.parseFloat(argv[i + 1]);
        if ((i = ArgPos("-trace", argc, argv)) >= 0) debug_mode = Integer.parseInt(argv[i + 1]);
        if ((i = ArgPos("-train", argc, argv)) >= 0) train_file = new FileInputStream(argv[i + 1]);
        if ((i = ArgPos("-alpha", argc, argv)) >= 0) alpha = Float.parseFloat(argv[i + 1]);
        if ((i = ArgPos("-output", argc, argv)) >= 0) output_file = new FileOutputStream(getOutputFile(argv[i + 1]));
        if ((i = ArgPos("-directed", argc, argv)) >= 0) directed = Integer.parseInt(argv[i + 1]) != 0;
        if ((i = ArgPos("-pt", argc, argv)) > 0) pretrained_file = new FileInputStream(argv[i + 1]);
        if ((i = ArgPos("-window", argc, argv)) >= 0) window = Integer.parseInt(argv[i + 1]);
        if ((i = ArgPos("-sample", argc, argv)) >= 0) sample = Double.parseDouble(argv[i + 1]);
        if ((i = ArgPos("-negative", argc, argv)) >= 0) negative = Integer.parseInt(argv[i + 1]);
        if ((i = ArgPos("-iter", argc, argv)) >= 0) iter = Integer.parseInt(argv[i + 1]);
        if ((i = ArgPos("-min-count", argc, argv)) >= 0) min_count = Integer.parseInt(argv[i + 1]);
        if ((i = ArgPos("-dbfs", argc, argv)) >= 0) pqsampling = Boolean.parseBoolean(argv[i + 1]);
        if ((i = ArgPos("-p", argc, argv)) > 0) p1 = Float.parseFloat(argv[i + 1]);
        if ((i = ArgPos("-q", argc, argv)) > 0) q1 = Float.parseFloat(argv[i + 1]);

        if (window > MAX_CONTEXT_PATH_LEN) {
            System.out.println(String.format("Window size %d value too large. Truncating the value to %d\n", window, MAX_CONTEXT_PATH_LEN));
            window = MAX_CONTEXT_PATH_LEN;
        }
        one_minus_onehop_pref = 1 - onehop_pref;
        vocab = new vocab_node[vocab_max_size];
        vocab_hash = new int[vocab_hash_size];
        expTable = new float[EXP_TABLE_SIZE + 1];
        for (i = 0; i < EXP_TABLE_SIZE; i++) {
            // TODO: C and Java have the same 'exp' function, but need to check floats again
            expTable[i] = (float)Math.exp((i / (float)EXP_TABLE_SIZE * 2 - 1) * MAX_EXP); // Precompute the exp() table
            expTable[i] = expTable[i] / (expTable[i] + 1);                   // Precompute f(x) = x / (x + 1)
        }
        return train();
    }

    private File getOutputFile(String path) {
        File res = new File(path);
        res.mkdirs();
        return res;
    }

    public static void wordHashTestMain(String[] args) {
        Node2Vec node2Vec = new Node2Vec();
        System.out.println(node2Vec.GetWordHash("hahaha"));
        System.out.println(node2Vec.GetWordHash("cat"));
        System.out.println(node2Vec.GetWordHash("dog"));
        System.out.println(node2Vec.GetWordHash("Santa Claus"));
    }

    public static void main(String[] args) throws IOException {
        Node2Vec cmd = new Node2Vec();
        cmd.run(args.length, args);
    }
    
    public static void mainForTest(String[] args) throws IOException {
        /*
        //wordHashTestMain(args);
        String input = "data/node2vec/mat.txt.s.20k";
        String trace = "3";
        Node2Vec cmd = new Node2Vec();
        args = new String[] { "-train", input, "-trace", trace, "-output", "someVecs" };
        cmd.run(args.length, args);
        */
        
        Node2Vec node2VecMat;
        ByteArrayOutputStream node2VecMatVectorsFile;
        //PrintStream oldStdOut;
        final String matGraphFile = "data/node2vec/mat.txt.s.20k";
        final String TRACE = "3";
        final String PQSAMPLING = "true";
        
        node2VecMat = new Node2Vec();
        // redirect vector output so that we can check it
        node2VecMatVectorsFile = new ByteArrayOutputStream();
        node2VecMat.setOutput(node2VecMatVectorsFile);
        
        // run Node2Vec
        String[] node2vecParams = { "-train", matGraphFile,
            "-trace", TRACE,
            "-dbfs", PQSAMPLING,
            "-output", "data/node2vec/mat.txt.s.20k_node2vec_trace3_C_vectors.vec",
            "-directed", "0"
        };
        try {
            boolean status = node2VecMat.run(node2vecParams.length, node2vecParams);
            System.out.println(status);            
        } catch (IOException e) {
            e.printStackTrace();
        }
        
    }

    public static void mainNormal(String[] args) throws IOException {
        Node2Vec cmd = new Node2Vec();
        // for example, use args: -train ./prediction/graphs/relations.graph -output refVecs
        cmd.run(args.length, args);
    }
}
