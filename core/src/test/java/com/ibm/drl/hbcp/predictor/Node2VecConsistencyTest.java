package com.ibm.drl.hbcp.predictor;

import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.predictor.graph.AttributeValueNode;
import com.ibm.drl.hbcp.predictor.graph.Graph;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.predictor.queries.SearchResult;
import com.ibm.drl.hbcp.predictor.queries.VectorQuery;
import com.ibm.drl.hbcp.util.Props;
import org.junit.Test;

import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class Node2VecConsistencyTest {

    private final String edgesKarateClub = "2 1\n" +
            "3 1\n" +
            "4 1\n" +
            "5 1\n" +
            "6 1\n" +
            "7 1\n" +
            "8 1\n" +
            "9 1\n" +
            "11 1\n" +
            "12 1\n" +
            "13 1\n" +
            "14 1\n" +
            "18 1\n" +
            "20 1\n" +
            "22 1\n" +
            "32 1\n" +
            "3 2\n" +
            "4 2\n" +
            "8 2\n" +
            "14 2\n" +
            "18 2\n" +
            "20 2\n" +
            "22 2\n" +
            "31 2\n" +
            "4 3\n" +
            "8 3\n" +
            "9 3\n" +
            "10 3\n" +
            "14 3\n" +
            "28 3\n" +
            "29 3\n" +
            "33 3\n" +
            "8 4\n" +
            "13 4\n" +
            "14 4\n" +
            "7 5\n" +
            "11 5\n" +
            "7 6\n" +
            "11 6\n" +
            "17 6\n" +
            "17 7\n" +
            "31 9\n" +
            "33 9\n" +
            "34 9\n" +
            "34 10\n" +
            "34 14\n" +
            "33 15\n" +
            "34 15\n" +
            "33 16\n" +
            "34 16\n" +
            "33 19\n" +
            "34 19\n" +
            "34 20\n" +
            "33 21\n" +
            "34 21\n" +
            "33 23\n" +
            "34 23\n" +
            "26 24\n" +
            "28 24\n" +
            "30 24\n" +
            "33 24\n" +
            "34 24\n" +
            "26 25\n" +
            "28 25\n" +
            "32 25\n" +
            "32 26\n" +
            "30 27\n" +
            "34 27\n" +
            "34 28\n" +
            "32 29\n" +
            "34 29\n" +
            "33 30\n" +
            "34 30\n" +
            "33 31\n" +
            "34 31\n" +
            "33 32\n" +
            "34 32\n" +
            "34 33\n";

    private final String communitiesString = "9 10 24 26 25 28 29 30 27 31 32 33 15 16 19 21 23 34\n" +
            "1 2 3 4 8 12 13 14 18 20 22 5 6 7 11 17";

    private final Graph<String> graphKarateClub = () -> Arrays.stream(edgesKarateClub.split("\\n"))
                    .map(e -> Arrays.stream(e.split(" +"))
                        .map(this::id)
                        .collect(Collectors.toList()))
                    .map(e -> new Graph.Edge<String>(e.get(0), e.get(1), 1.0))
                    .collect(Collectors.toList());

    private final List<List<String>> communitiesKarateClub = Arrays.stream(communitiesString.split("\\n"))
            .map(community -> community.split(" +"))
            .map(Arrays::asList)
            .collect(Collectors.toList());

    private final String corpus = "cat drink milk\n"
            + "cat eat fish\n"
            + "dog drink milk\n"
            + "i drink beer\n"
            + "i eat fish\n"
            + "i pet dog\n"
            + "i pet cat\n";

    private final Graph<String> graphCorpus = () -> Arrays.stream(corpus.split("\\n"))
            .map(e -> Arrays.stream(e.split(" +"))
                    .map(this::id)
                    .collect(Collectors.toList()))
            .flatMap(sentence -> getEdges(sentence).stream())
            .collect(Collectors.toList());

    private final Graph<String> graphBipartite = () -> getBipartiteEdges(communitiesKarateClub.get(0), communitiesKarateClub.get(1));

    @Test
    public void testKarateClubCommunities() throws IOException {
        Properties props = Props.loadProperties();
        props.setProperty("node2vec.layer1_size", "300");
        props.setProperty("node2vec.niters", "50");
        props.setProperty("node2vec.ns", "20");
        Node2Vec node2Vec = new Node2Vec(graphKarateClub, props);
        NodeVecs vecs = node2Vec.getNodeVectors();
        //VectorQuery query = new VectorQuery(vecs.getNodeInstanceVector(id("2")), avn -> true, 10);
        //System.out.println(query.searchTopK(vecs, 10));
        double selfSim1 = averageSimilarity(communitiesKarateClub.get(0), communitiesKarateClub.get(0), vecs);
        double selfSim2 = averageSimilarity(communitiesKarateClub.get(1), communitiesKarateClub.get(1), vecs);
        double crossSim = averageSimilarity(communitiesKarateClub.get(0), communitiesKarateClub.get(1), vecs);
        assertTrue(selfSim1 > crossSim);
        assertTrue(selfSim2 > crossSim);
    }

    
    public void testCorpus() throws IOException {
        Properties props = Props.loadProperties();
        props.setProperty("node2vec.layer1_size", "300");
        props.setProperty("node2vec.niters", "50");
        props.setProperty("node2vec.ns", "20");
        Node2Vec node2Vec = new Node2Vec(graphCorpus, props);
        NodeVecs vecs = node2Vec.getNodeVectors();
        VectorQuery query = new VectorQuery(vecs.getNodeInstanceVector(id("drink")), avn -> true, 10);
        List<SearchResult> results = query.searchTopK(vecs, 3);
        Set<String> set = results.stream().map(sr -> sr.getNode().getValue()).collect(Collectors.toSet());
        assertEquals(Sets.newHashSet("drink", "milk", "beer"), set);
    }

    public void testBipartite() throws IOException {
        Properties props = Props.loadProperties();
        props.setProperty("node2vec.layer1_size", "300");
        props.setProperty("node2vec.niters", "50");
        props.setProperty("node2vec.window", "1");
        props.setProperty("node2vec.p1", "0");
        props.setProperty("node2vec.q1", "1");
        Node2Vec node2Vec = new Node2Vec(graphBipartite, props);
        NodeVecs vecs = node2Vec.getNodeVectors();
        VectorQuery query = new VectorQuery(vecs.getNodeInstanceVector(id("1")), avn -> true, 10);
        System.out.println(query.searchTopK(vecs, 10));
        double selfSim1 = averageSimilarity(communitiesKarateClub.get(0), communitiesKarateClub.get(0), vecs);
        double selfSim2 = averageSimilarity(communitiesKarateClub.get(1), communitiesKarateClub.get(1), vecs);
        double crossSim = averageSimilarity(communitiesKarateClub.get(0), communitiesKarateClub.get(1), vecs);
        assertTrue(selfSim1 > crossSim);
        assertTrue(selfSim2 > crossSim);

    }

    private double averageSimilarity(List<String> nodes1, List<String> nodes2, NodeVecs vecs) {
        double total = 0.0;
        double res = 0.0;
        for (String node1 : nodes1) {
            for (String node2 : nodes2) {
                if (!node1.equals(node2)) {
                    total++;
                    double sim = vecs.getNodeInstanceVector(id(node1)).cosineSim(vecs.getNodeInstanceVector(id(node2)));
                    res += sim;
                }
            }
        }
        return res / total;
    }

    private String id(String value) {
        return "C:0:" + value;
    }

    private AttributeValueNode node(String value) {
        return AttributeValueNode.parse(id(value));
    }

    private List<Graph.Edge<String>> getEdges(List<String> sentence) {
        List<Graph.Edge<String>> res = new ArrayList<>();
        for (int i = 0; i < sentence.size() - 1; i++) {
            res.add(new Graph.Edge<>(sentence.get(i), sentence.get(i + 1), 1.0));
        }
        return res;
    }

    private List<Graph.Edge<String>> getBipartiteEdges(List<String> p1, List<String> p2) {
        List<Graph.Edge<String>> res = new ArrayList<>();
        for (String e1 : p1) {
            for (String e2 : p2) {
                res.add(new Graph.Edge<>(id(e1), id(e2), 1.0));
                res.add(new Graph.Edge<>(id(e1), id(e2), 1.0));
            }
        }
        return res;
    }
}
