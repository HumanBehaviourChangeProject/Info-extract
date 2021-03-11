/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.core.attributes.collection;

import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.function.Predicate;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extraction.DocVector;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.PredictionWorkflow;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;

/**
 *
 * @author debforit
 */

//class AttributeMatchComparator implements Comparator<Pair<Attribute, Float>> {
//
//    @Override
//    public int compare(Pair<Attribute, Float> ref, Pair<Attribute, Float> that) {
//        return -1*Float.compare(ref.getRight(), that.getRight());
//    }
//
//}

//class RefDataInstance implements Comparable<RefDataInstance> {
//    DataInstance d;
//    double sim; // similarity with a query
//    SortedSet<Pair<Attribute, Float>> attribSims;
//
//    boolean isSimilarPopulation;
//    boolean isSimilarIntervention;
//    Set<Attribute> commonInterventions;
//
//    public RefDataInstance(DataInstance d, double sim) {
//        this(d, sim, null);
//    }
//
//    public RefDataInstance(DataInstance d, double sim, SortedSet<Pair<Attribute, Float>> asims) {
//        this.d = d;
//        this.sim = sim;
//        this.attribSims = asims;
//    }
//
//    @Override
//    public int compareTo(RefDataInstance that) {
//        return -1*Float.compare(sim, that.sim); // descending
//    }
//
//    @Override
//    public String toString() {
//        return d.toString() + ", " + String.format("%.3f", sim) + ", (" + isSimilarIntervention + ", " + isSimilarPopulation + ")";
//    }
//
//    String getTopAttribMatchScores() {
//        StringBuffer buff = new StringBuffer();
//        int k = 0;
//        for (Pair<Attribute, Float> p: this.attribSims) {
//            buff
//                .append("<")
//                .append(p.getLeft().getType().getShortString())
//                .append(":")
//                .append(p.getLeft().getId())
//                .append(":")
//                .append(p.getRight())
//                .append("> ")
//            ;
//            k++;
//            if (k==3)
//                break;
//        }
//        return buff.toString();
//    }
//}


public class AVPRetriever extends PredictionWorkflow {

    //static final int TOP_K = 10;
    static final int NGRAM_SIZE = 3;
    static final double ALPHA = 1.0;  // BCT's: (ALPHA*code_match + (1-ALPHA)*prefix match) * text_match

    private final Set<Attribute> numericAttributes;
    
    public AVPRetriever(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                 AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                 TrainTestSplitter splitter,
                                 Properties props) throws IOException {
        super(values, annotations, splitter, props);
        numericAttributes = new HashSet<>(annotations.getNumericAttributes());
    }
    
    boolean samePrefix(String code_a, String code_b) {
        return Integer.parseInt(code_a.split("\\.")[0]) == Integer.parseInt(code_b.split("\\.")[0]);
    }
    
    boolean isNumeric(Attribute key) { return numericAttributes.contains(key); }
    
    double textSimilarity(DocVector a, DocVector b) {
        return a.cosineSim(b);
    }
    
    String extractBCTCode(Attribute a) {
        String attribName = a.getName();
        int len = attribName.length();
        if (len==0)
            return null;
        
        int i;
        for (i=0; i < len; i++) {
            if (Character.isAlphabetic(attribName.charAt(i)))
                break;
        }
        return i>0? attribName.substring(0, i-1): null;
    }

    public double computeSim(ArmifiedAttributeValuePair avp1, ArmifiedAttributeValuePair avp2, boolean useText) {
        Attribute a = avp1.getAttribute();
        Attribute b = avp2.getAttribute();
        
        if (a.getType() != b.getType())
            return 0;

        // For interventions
        if (a.getType() == AttributeType.INTERVENTION) {
            return 1;

            // CJ: add back when we want to handle prefixes (currently disabled)
//            String bctCode_a = extractBCTCode(a);
//            String bctCode_b = extractBCTCode(b);
//            // TODO check that these can't be null
//            if (bctCode_a==null || bctCode_b==null)
//                return 0;
//
//            return bctCode_a.equals(bctCode_b)? ALPHA : samePrefix(bctCode_a, bctCode_b)? 1-ALPHA: 0;
            // CJ: revisit if we can afford to compare text
//            if (useText) {
//                DocVector dv_a;
//                DocVector dv_b;
//                double textsim = 1;
//
//                try {
//                    dv_a = new DocVector(((NormalizedAttributeValuePair)avp1).getOriginal().getValue(), NGRAM_SIZE);
//                    dv_b = new DocVector(((NormalizedAttributeValuePair)avp2).getOriginal().getValue(), NGRAM_SIZE);
//                    textsim = textSimilarity(dv_a, dv_b);
//                }
//                catch (ClassCastException e) { }
//
//                // fallback to the provided value (should be the same, like "1" or "true") if the AVPs were not normalized
//                sim_value = factor * textsim;
//            }
        } else if (isNumeric(a)) {
            if (!isNumeric(b))
                return 0;
            
            if (!a.getId().equals(b.getId()))
                return 0;

            try {
                final double val1 = ParsingUtils.parseFirstDouble(avp1.getValue());
                final double val2 = ParsingUtils.parseFirstDouble(avp2.getValue());
                // TODO if val1 or val2 are 0, similarity is 0
                return 1 - Math.abs(val1 - val2) / Math.max(val1, val2);
            } catch (NumberFormatException e) {
                return avp1.getValue().equals(avp2.getValue()) ? 1 : 0;
            }
        } else {
            return avp1.getValue().equals(avp2.getValue()) ? 1 : 0;
        }

    }
    
    /*  Compute the similarity between this and that.
        Notes:
        1. Similarity works for the time being for interventions and numerical values.
        2. If the types don't match, then return 0 (not similar)
    */
    public double computeSim(ArmifiedAttributeValuePair avp_a, ArmifiedAttributeValuePair avp_b) {
        return computeSim(avp_a, avp_b, false);
    }

//    public double computeSimsBetweenAVPs(
//            Multiset<? extends ArmifiedAttributeValuePair> avps_x,
//            Multiset<? extends ArmifiedAttributeValuePair> avps_y) {
//
//        // TODO refactor
//        double totalSim = 0;
//        attribSims = new TreeSet<>(new AttributeMatchComparator());  // reinit this buffer for every new pair of query, doc
//        for (ArmifiedAttributeValuePair x: avps_x) {
//            for (ArmifiedAttributeValuePair y: avps_y) {
//                double attribSim = computeSim(x, y);
//                if (attribSim > 0) {
//                    attribSims.add(Pair.of(y.getAttribute(), attribSim));  // destination (non-query) attribute and similarity
//                    totalSim += attribSim;
//                }
//            }
//        }
//        return totalSim;  // not normalized (normalization is tricky here)
//    }
//
//    public double computeSimsBetweenAVPs(AttributeValueCollection<? extends ArmifiedAttributeValuePair> avps_x,
//            AttributeValueCollection<? extends ArmifiedAttributeValuePair> avps_y) {
//
//        // TODO  refactor
//        double totalSim = 0;
//        attribSims = new TreeSet<>(new AttributeMatchComparator());  // reinit this buffer for every new pair of query, doc
//        for (ArmifiedAttributeValuePair x: avps_x) {
//            for (ArmifiedAttributeValuePair y: avps_y) {
//                double attribSim = computeSim(x, y);
//                if (attribSim > 0) {
//                    attribSims.add(Pair.of(y.getAttribute(), attribSim));  // destination (non-query) attribute and similarity
//                    totalSim += attribSim;
//                }
//            }
//        }
//        return totalSim;  // not normalized (normalization is tricky here)
//    }

    public SortedMap<Attribute, Double> computeAttributeSimilarities(AttributeValueCollection<? extends ArmifiedAttributeValuePair> avps1,
                                              AttributeValueCollection<? extends ArmifiedAttributeValuePair> avps2) {
        SortedMap<Attribute, Double> attribSims = new TreeMap<>();
        for (ArmifiedAttributeValuePair x: avps1) {
            for (ArmifiedAttributeValuePair y: avps2) {
                if (x.getAttribute().equals(y.getAttribute())) {
                    double attribSim = computeSim(x, y);
//                if (attribSim > 0) {
                    attribSims.put(y.getAttribute(), attribSim);  // destination (non-query) attribute and similarity
                }
            }
        }
        return attribSims;  // not normalized (normalization is tricky here)
    }

    static public AttributeValueCollection<ArmifiedAttributeValuePair> constructSubspaceQuery(AttributeValueCollection<ArmifiedAttributeValuePair> q, AttributeType type) {
        Set<ArmifiedAttributeValuePair> q_sub = new HashSet<>();
        for (ArmifiedAttributeValuePair avp: q) {
            if (avp.getAttribute().getType() == type) {
                q_sub.add(avp);
            }
        }
        return new AttributeValueCollection<>(q_sub);
    }

    public boolean isSimilarAttributeGroup(SortedMap<Attribute, Double> attributeSimilarity, AttributeValueCollection<? extends ArmifiedAttributeValuePair> query, AttributeType type) {
        double score = aggregateScore(attributeSimilarity, query, a -> a.getType() == type);
        return score >= 0.666;
    }

    public double aggregateScore(SortedMap<Attribute, Double> attributeSimilarity, AttributeValueCollection<? extends ArmifiedAttributeValuePair> query, Predicate<Attribute> isRelevantAttribute) {
        double totalAttributeSimilarity = attributeSimilarity.entrySet().stream()
                .filter(e -> isRelevantAttribute.test(e.getKey()))
                .map(Map.Entry::getValue)
                .reduce(0.0, Double::sum);
        long queryAttributesInGroup = query.getAllPairs().stream().filter(e -> isRelevantAttribute.test(((ArmifiedAttributeValuePair) e).getAttribute())).count();
        return totalAttributeSimilarity / queryAttributesInGroup;
    }
    
    /*
        A sample implementation of the main function to test the functionality...
    */ 
    public static void main(String[] args) {
        try {
            Properties extraProps = new Properties();
            if (args.length > 0) {
                extraProps.load(new FileReader(args[0])); // overriding arguments
                System.out.println("Additional properties: " + extraProps.toString());
            }
            Properties props = Props.loadProperties();
            props = Props.overrideProps(props, extraProps);
            JSONRefParser refParser = new JSONRefParser(props);
            
            // AVPRetriever avpRetriever = 
    		new AVPRetriever(
                    refParser.getAttributeValuePairs(),
                    refParser.getAttributeValuePairs(),
                    new TrainTestSplitter(0.8),
                    props
            );
            
            //avpRetriever.computeAllNNs(5);
        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }
}
