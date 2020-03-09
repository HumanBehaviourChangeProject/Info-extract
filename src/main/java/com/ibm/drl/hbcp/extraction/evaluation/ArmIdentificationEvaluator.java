package com.ibm.drl.hbcp.extraction.evaluation;

import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

import org.jetbrains.annotations.NotNull;

import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.extractor.RefComparison;

/**
 * Evaluator for extracting arms. It is implemented as an interface so that different
 * evaluators can be used with different matching criteria, e.g., exact match (arm ids or
 * arm names are equal); less strict (some overlap in 'standard' arm names); lenient (overlap
 * in any name in cluster).
 * 
 * @author charlesj
 *
 * @param <Doc>
 */
public interface ArmIdentificationEvaluator<Doc> extends Evaluator<Doc, Arm> {

    default boolean isCorrect(@NotNull Arm predicted, @NotNull Arm expected) {
        return predicted.equals(expected);
    }

    @Override
    default RefComparison compareWithRef(Collection<Arm> predicted, Collection<Arm> annotation) {
        // manual version of comparing sets to get tp/fp/fn/tn
        int tp = 0;
        int fp = 0;
        int fn = 0;
        Set<Arm> predictSet = new HashSet<>(predicted);
        Set<Arm> goldSet = new HashSet<>(annotation);
        for (Iterator<Arm> i = predictSet.iterator(); i.hasNext();) {
            Arm predArm = i.next();
            for (Iterator<Arm> j = goldSet.iterator(); j.hasNext();) {
                Arm goldArm = j.next();
                if (goldArm.getId().equals("0")) {
                    j.remove();  // ignore 'default' arm
                } else if (isCorrect(predArm, goldArm)) {
                    tp++;
                    i.remove();
                    j.remove();
                    break;
                }
            }
        }
        fp = predictSet.size();
        fn = goldSet.size();
        return new RefComparison(tp, fp, fn, 0);
    }
}
