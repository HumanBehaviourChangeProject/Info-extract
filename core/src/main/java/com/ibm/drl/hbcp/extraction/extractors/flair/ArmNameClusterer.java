package com.ibm.drl.hbcp.extraction.extractors.flair;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.text.WordUtils;

import com.aliasi.cluster.CompleteLinkClusterer;
import com.aliasi.cluster.Dendrogram;
import com.aliasi.cluster.HierarchicalClusterer;
import com.aliasi.spell.EditDistance;
import com.aliasi.util.Distance;
import com.google.common.collect.Lists;

/**
 * Clusters arm names
 *
 * @author mgleize
 */
public class ArmNameClusterer {

    private final int assumedArmCount;

    private final List<String> stopWords = Lists.newArrayList(
            "group", "arm", "condition", "intervention"
    );

    private final Distance<CharSequence> editDistance = new EditDistance(false);
    private final Distance<CharSequence> editDistanceInitials = new Distance<CharSequence>() {

        @Override
        public double distance(CharSequence e1, CharSequence e2) {
            String e1Upper = acronym(e1.toString());
            String e2Upper = acronym(e2.toString());
            double res = editDistance.distance(e1Upper, e2Upper);
            return res;
        }

        private String acronym(String s) {
            // capitalize every first word
            String res = s;
            res = WordUtils.capitalizeFully(res);
            res = res.replaceAll("[^A-Z]", "");
            res = res.trim();
            return res;
        }
    };
    // at most one character change is allowed in the acronym version of the strings
    private static final double MAX_INITIAL_DISTANCE = 1.0;
    private final Distance<String> compositeDistance = (charSequence, e1) -> {
        String c1 = normalize(charSequence);
        String c2 = normalize(e1);
        double initialDistance = editDistanceInitials.distance(c1, c2);
        double fullDistance = editDistance.distance(c1, c2);
        if (c1.contains(c2) || c2.contains(c1)) {
            return 0.0;
        } else {
            return initialDistance * fullDistance;
        }
    };
    private HierarchicalClusterer<String> clusterer = new CompleteLinkClusterer<>(Integer.MAX_VALUE, compositeDistance);

    public ArmNameClusterer(int assumedArmCount) {
        this.assumedArmCount = assumedArmCount;
    }

    public Set<Set<String>> getArmNameClusters(Collection<String> armNames) {
        Dendrogram<String> clDendrogram = clusterer.hierarchicalCluster(new HashSet<>(armNames));
        // Dendrograms to Clusterings
        return clDendrogram.partitionK(assumedArmCount);
    }

    private String normalize(String s) {
        String res = s;
        for (String stopWord: stopWords) {
            String previous = res;
            res = res.replaceAll(stopWord, "");
            // only allow the removal of 1 stopword
            if (!res.equals(previous)) break;
        }
        res = res.trim();
        return res;
    }

}
