package com.ibm.drl.hbcp.extraction.extractors;

import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.core.attributes.AttributeValuePair;
import com.ibm.drl.hbcp.extraction.candidates.Candidate;

import java.util.Collection;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * An extractor of attribute-value pairs (most of the extractors usually).
 * @param <Document> type of document the extractor takes as input
 * @param <AVP> an attribute-value pair type
 * @param <CandidateAVP> a candidate type wrapping a attribute-value pair
 */
public interface AttributeExtractor<Document, AVP extends AttributeValuePair, CandidateAVP extends Candidate<AVP>>
        extends EvaluatedExtractor<Document, AVP, CandidateAVP> {

    Set<Attribute> getExtractedAttributes();

    default Collection<AVP> getRelevant(Collection<AVP> annotations) {
        Set<Attribute> relevantAttributes = getExtractedAttributes();
        return annotations.stream()
                .filter(avp -> relevantAttributes.contains(avp.getAttribute()))
                .collect(Collectors.toList());
    }
}
