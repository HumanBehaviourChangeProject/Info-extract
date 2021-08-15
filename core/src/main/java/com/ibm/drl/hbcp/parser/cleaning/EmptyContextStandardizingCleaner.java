package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * An empty context is a new convention appearing in Physical Activity annotations and indicates a value
 * found in a table. Most of the time, only one of the in-table population characteristics or outcome has a non-empty
 * context, and this will be the name/title/caption of the table in the PDF.
 *
 * This cleaner empties all contexts supposed to be associated with table values in this new convention, and puts
 * systematically the name/title/caption of the table in the "highlighted text" field instead, so that further cleaners
 * can consistently identify and locate table values.
 *
 * @author mgleize
 */
public class EmptyContextStandardizingCleaner implements Cleaner {

    public static final Logger logger = LoggerFactory.getLogger(EmptyContextStandardizingCleaner.class);

    @Override
    public List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original) {
        List<AnnotatedAttributeValuePair> res = new ArrayList<>();
        // split across docnames
        AttributeValueCollection<AnnotatedAttributeValuePair> collection = new AttributeValueCollection<>(original);
        for (String docName : collection.byDoc().keySet()) {
            Multiset<AnnotatedAttributeValuePair> avps = collection.byDoc().get(docName);
            // only values to touch are of the population type, since the outcomes already respect the convention
            // described in the class description
            List<AnnotatedAttributeValuePair> toFix = avps.stream()
                    .filter(this::isContextInTable)
                    .collect(Collectors.toList());
            List<AnnotatedAttributeValuePair> notToFix = avps.stream()
                    .filter(aavp -> !isContextInTable(aavp))
                    .collect(Collectors.toList());
            // there should be a (hopefully unique) non-empty context in this collection to mark the table caption
            List<String> nonEmptyContexts = toFix.stream()
                    .filter(aavp -> !aavp.getContext().isEmpty())
                    .map(AnnotatedAttributeValuePair::getContext)
                    .distinct()
                    .collect(Collectors.toList());
            // if there are none or several, simply signal it and take the first (or don't touch anything)
            if (nonEmptyContexts.isEmpty()) {
                // haven't found any table name, just return the collection as is
                res.addAll(new ArrayList<>(avps));
            } else {
                String tableCaption = nonEmptyContexts.get(0);
                if (nonEmptyContexts.size() > 1) {
                    logger.debug("In {}: several potential table captions have been detected, taking the first", docName);
                }
                // fix all the toFix values (leave context empty, put the table caption as "highlighted text")
                List<AnnotatedAttributeValuePair> fixed = toFix.stream()
                        .map(aavp -> getStandardizedTableValue(aavp, tableCaption))
                        .collect(Collectors.toList());
                // add to the result the fixed values AND the ones to leave untouched
                res.addAll(fixed);
                res.addAll(notToFix);
            }
        }
        return res;
    }

    private AnnotatedAttributeValuePair getStandardizedTableValue(AnnotatedAttributeValuePair tableOriginalValue,
                                                                  String tableCaption) {
        return new AnnotatedAttributeValuePair(tableOriginalValue.getAttribute(), tableOriginalValue.getValue(),
                tableOriginalValue.getDocName(), tableOriginalValue.getArm(),
                "",
                tableCaption,
                tableOriginalValue.getSprintNo(), tableOriginalValue.getAnnotationPage());
    }

    private boolean isContextInTable(AnnotatedAttributeValuePair original) {
        // for a population attribute
        // either an empty context, or a context starting with "Table"
        return original.getAttribute().getType() == AttributeType.POPULATION
                && (original.getContext().isEmpty()
                    || contextStartsWithTable(original));
    }

    private boolean contextStartsWithTable(AnnotatedAttributeValuePair original) {
        return original.getContext().trim().toLowerCase().startsWith("table ");
    }
}
