package com.ibm.drl.hbcp.extraction.answerselectors;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.extraction.candidates.Candidate;

import java.util.*;
import java.util.stream.Collectors;

/**
 * Selects the best candidate answer (according to its score) for each arm.
 *
 * @author marting
 */
public class BestAnswerPerArmSelector<AAVP extends ArmifiedAttributeValuePair, CandidateAnswer extends Candidate<AAVP>> implements AnswerSelector<AAVP, CandidateAnswer> {

    @Override
    public Collection<CandidateAnswer> select(Collection<CandidateAnswer> candidates) {
        Map<Arm, List<CandidateAnswer>> candidatesPerArm = candidates.stream()
                .collect(Collectors.groupingBy(aavp -> aavp.getAnswer().getArm()));
        Map<Arm, CandidateAnswer> bestPerArm = candidatesPerArm.entrySet().stream()
                .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue().stream().max(Comparator.naturalOrder()).get()));
        return bestPerArm.values();
    }
}
