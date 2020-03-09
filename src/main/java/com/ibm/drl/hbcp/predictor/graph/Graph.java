package com.ibm.drl.hbcp.predictor.graph;

import java.util.List;
import java.util.Objects;

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
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Edge<?> edge = (Edge<?>) o;
            return source.equals(edge.source) &&
                    target.equals(edge.target);
        }

        @Override
        public int hashCode() {
            return Objects.hash(source, target, weight);
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
