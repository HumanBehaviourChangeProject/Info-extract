package com.ibm.drl.hbcp.predictor.graph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * A simple graph as the list of its (weighted) edges.
 *
 * @param <E> type of the vertices/nodes of the graph
 * @author marting
 */
public interface Graph<E> {

    /** The edges of the graph */
    List<? extends Edge<E>> getEdges();

    /**
     * Edge of the graph with two nodes and an edge weight
     */
    class Edge<E> {
        public final E source;
        public final E target;
        public double weight;

        public Edge(E source, E target, double weight) {
            this.source = source; this.target = target; this.weight = weight;
        }

        @Override
        public String toString() {
            return source + "\t" + target + "\t" + weight;
        }
    }

    /**
     * Concatenate edges with newline into one string.
     * Each edge has two nodes and an edge weight (i.e., 3 columns), which are joined by tab.
     * 
     * @return graph with one line per edge
     */
    default String toText() {
        StringBuilder sb = new StringBuilder();
        for (Edge<E> edge : getEdges()) {
            sb.append(edge);
            sb.append("\n");
        }
        return sb.toString();
    }
}
