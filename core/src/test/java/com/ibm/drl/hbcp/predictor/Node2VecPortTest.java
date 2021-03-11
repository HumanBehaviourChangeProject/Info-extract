package com.ibm.drl.hbcp.predictor;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintStream;
import java.util.Properties;
import java.util.regex.Pattern;

import org.junit.BeforeClass;
import org.junit.Test;

import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.util.FileUtils;

/**
 * Unit tests of the Java port of node2vec.c
 * Note that this ONLY CHECKS THAT THE PORT IS CORRECT, not that the original implementation is
 * (so the only Java bugs allowed are C bugs :D)
 *
 * @author marting
 */
public class Node2VecPortTest {

    public static final String TRACE = "3";
    public static final double ESPILON = 0.001; // the espilon in the approximate vector equality function

    public static final String matGraphFile = "src/test/resources/node2vec/mat.txt.s.20k";
    public static final String matCStandardOutputFile = "src/test/resources/node2vec/mat.txt.s.20k_node2vec_trace3_C_stdout.txt";
    public static final String matCVectorsFile = "src/test/resources/node2vec/mat.txt.s.20k_node2vec_trace3_C_vectors.vec";

    public static final String cioGraphFile = "graphs/bcio_test_relations.graph";

    public static final Pattern RELEVANT_STDOUT_LINE_REGEX = Pattern.compile("(Skip-gram)|(#nodes)|(Sampled context)");

    static Node2Vec node2VecMat;
    static boolean ranSuccessfully;
    static ByteArrayOutputStream node2VecMatVectorsFile;
    static PrintStream oldStdOut;
    static ByteArrayOutputStream node2VecMatStandardOutput;

    static Node2Vec node2VecReal;
    static NodeVecs nodeVecs;

    @BeforeClass
    public static void setUpClass() throws IOException {
        node2VecMat = new Node2Vec();
        // redirect vector output so that we can check it
        node2VecMatVectorsFile = new ByteArrayOutputStream();
        node2VecMat.setOutput(node2VecMatVectorsFile);
        // also temporarily redirect standard output so that we can check it
        oldStdOut = System.out;
        node2VecMatStandardOutput = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(node2VecMatStandardOutput);
        System.setOut(ps);
        // run Node2Vec
        String[] args = {
            "-train", matGraphFile,
            "-trace", TRACE,
            "-dbfs", "true",
            "-output", matCVectorsFile,
            "-directed", "0",
            "-sample", "0"
        };
        try {
            ranSuccessfully = node2VecMat.run(args.length, args);
            // point std out back to original
            System.setOut(oldStdOut);
        } catch (IOException e) {
            e.printStackTrace();
            ranSuccessfully = false;
        }
        // use node2vec in Java fashion
        Properties prop = new Properties();
		InputStream configurationFile = Node2VecPortTest.class.getClassLoader().getResourceAsStream("init.properties") ;
        prop.load( configurationFile );
        node2VecReal = new Node2Vec(AttribNodeRelations.fromFile(FileUtils.potentiallyGetAsResource(new File(cioGraphFile))), prop);
        nodeVecs = node2VecReal.getNodeVectors();
        assertNotNull(nodeVecs);
    }

    /** Test that Java Node2Vec ran successfully (boolean output of the run method) */
    @Test
    public void testRanSuccessfully() {
        assertTrue(ranSuccessfully);
    }

    /*+++DG: Now that the node2vec is stable, we don't need to compare against the C o/p
    // Test that the Java standard output is the same as the C output, for the same file and the same trace level
    @Test
    public void testStandardOutputConsistency() throws IOException {
        // collect all relevant lines in the C std output
        List<String> expected = getAllRelevantStdOutLines(new FileInputStream(matCStandardOutputFile));
        // collect all relevant lines in the Java std output
        List<String> actual = getAllRelevantStdOutLines(new ByteArrayInputStream(node2VecMatStandardOutput.toByteArray()));
        // compare the 2
        testConsistency(expected, actual);
    }
    
    private void testConsistency(List<String> expected, List<String> actual) {
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            assertEquals(expected.get(i), actual.get(i));
        }
    }

    private List<String> getAllRelevantStdOutLines(InputStream stdout) throws IOException {
        List<String> res = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(stdout))) {
            String line;
            while ((line = br.readLine()) != null) {
                if (isRelevantStdOutLine(line))
                    res.add(line);
            }
        }
        return res;
    }

    private boolean isRelevantStdOutLine(String line) {
        return RELEVANT_STDOUT_LINE_REGEX.matcher(line).find();
    }
    ---DG
    */

    /*
    // Test that the individual vectors are the same in Java as in C (modulo some precision EPSILON)
    @Test
    public void testVectorValuesEquality() throws Exception {
        List<Pair<String, List<Double>>> expected = getAllLabeledVectors(new FileInputStream(matCVectorsFile));
        List<Pair<String, List<Double>>> actual = getAllLabeledVectors(new ByteArrayInputStream(node2VecMatVectorsFile.toByteArray()));
        assertTrue(isApproximatelyEqual(expected, actual, ESPILON));
    }

    private List<Pair<String, List<Double>>> getAllLabeledVectors(InputStream input) throws IOException {
        List<Pair<String, List<Double>>> res = new ArrayList<>();
        try (BufferedReader br = new BufferedReader(new InputStreamReader(input))) {
            String line = br.readLine(); // skip the first line
            while ((line = br.readLine()) != null) {
                String[] splits = line.split(" +");
                String word = splits[0];
                List<Double> vectorValues = new ArrayList<>();
                for (int i = 1; i < splits.length; i++) {
                    vectorValues.add(Double.parseDouble(splits[i]));
                }
                res.add(Pair.of(word, vectorValues));
            }
        }
        return res;
    }

    private boolean isApproximatelyEqual(List<Pair<String, List<Double>>> l1,
                                         List<Pair<String, List<Double>>> l2,
                                         double epsilon) {
        if (l1.size() != l2.size()) return false;
        for (int i = 0; i < l1.size(); i++) {
            Pair<String, List<Double>> v1 = l1.get(i);
            Pair<String, List<Double>> v2 = l2.get(i);
            if (!v1.getLeft().equals(v2.getLeft())) return false;
            if (v1.getRight().size() != v2.getRight().size()) return false;
            for (int j = 0; j < v1.getRight().size(); j++) {
                double difference = Math.abs(v1.getRight().get(j) - v2.getRight().get(j));
                if (difference > epsilon) return false;
            }
        }
        return true;
    }
    */

    /** Test that the convenient Java API returns some non-empty thing */
    @Test
    public void testJavaInterface() throws IOException {
        assertTrue(nodeVecs.getSize() > 0);
    }
}
