package com.ibm.drl.hbcp.extraction.extractors;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;

import java.util.Collection;
import java.util.stream.Collectors;

public class NoArmAssociator implements Associator<ArmifiedAttributeValuePair, CandidateInPassage<ArmifiedAttributeValuePair>> {
    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> associate(Collection<CandidateInPassage<ArmifiedAttributeValuePair>> predictedCandidates, Collection<Arm> extractedArms) {
        return predictedCandidates.stream()
                .map(candidateAVP -> new CandidateInPassage<>(candidateAVP.getPassage(),
                        candidateAVP.getAnswer().withArm(Arm.MAIN),
                        candidateAVP.getScore(),
                        candidateAVP.getAggregationScore()))
                .collect(Collectors.toList());
    }
}
