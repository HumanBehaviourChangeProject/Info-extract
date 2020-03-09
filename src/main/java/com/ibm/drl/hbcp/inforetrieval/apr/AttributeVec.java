package com.ibm.drl.hbcp.inforetrieval.apr;

import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;

import java.util.*;

public class AttributeVec {
    public final int id;
    protected final Map<String, AttributeValuePair> attrbMap;

    public AttributeVec(int id) {
        this.id = id;
        attrbMap = new HashMap<>();
    }

    public AttributeVec(AttributeVec vec) {
        id = vec.id;
        attrbMap = new HashMap<>(vec.attrbMap);
    }

    public void addAttrib(AttributeValuePair a) {
        attrbMap.put(a.getAttribute().getId(), a);
    }

    public List<AttributeValuePair> getSortedAttributes() {
        List<AttributeValuePair> res = new ArrayList<>(attrbMap.values());
        Collections.sort(res);
        return res;
    }

    // To be used to compute cosine similarity values across the
    // feature values extracted from two different passages.
    // For the time being we compute the similarity in the Hamming space,
    // i.e. we deal with only the presence/absence of a feature.
    public float cosineSim(AttributeVec that) {

        // find common attributes
        HashSet<String> commonAttribs = new HashSet<>(this.attrbMap.keySet());
        commonAttribs.retainAll(that.attrbMap.keySet());

        return commonAttribs.size();
        /*return (float) (commonAttribs.size()/
                (Math.sqrt(this.attrbMap.keySet().size()) *
                        Math.sqrt(that.attrbMap.keySet().size()))); */
    }
}