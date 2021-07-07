package com.ibm.drl.hbcp.parser.cleaning;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Properties;

import org.apache.commons.collections4.IteratorUtils;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;

/**
 * Apply all the cleaners iteratively.
 *
 * @author marting
 */
public class Cleaners implements Cleaner {

    private final List<Cleaner> cleaners;

    public Cleaners(Properties properties) {
        List<String> predefinedNumericAttributeIds = Arrays.asList(properties.getProperty("prediction.attribtype.numerical").split(","));
        cleaners = Lists.newArrayList(
                new NEqualsArtifactCleaner(),
                new ExoticCharacterCleaner(),
                new ValueCompletionCleaner(),
                new NumericValueCleaner(predefinedNumericAttributeIds),
                new ContextCompletionCleaner(properties, predefinedNumericAttributeIds),
                new EmptyContextStandardizingCleaner(),
                new ContextInTableCleaner(properties)
        );
    }

    @Override
    public List<AnnotatedAttributeValuePair> clean(Collection<AnnotatedAttributeValuePair> original) {
        List<AnnotatedAttributeValuePair> res = IteratorUtils.toList(original.iterator());
        for (Cleaner cleaner : cleaners) {
            res = cleaner.clean(res);
        }
        return res;
    }

    /** Convenience method to directly clean AttributeValueCollections */
    public AttributeValueCollection<AnnotatedAttributeValuePair> clean(AttributeValueCollection<AnnotatedAttributeValuePair> aavps) {
        return getCleaned(aavps);
    }

    public static void main(String[] args) throws IOException {
        Cleaners cleaners = new Cleaners(Props.loadProperties());
        JSONRefParser parser = new JSONRefParser(Props.loadProperties());
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = parser.getAttributeValuePairs();
        AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(annotations);
        System.out.println("Annotation count: " + annotations.size());
        System.out.println("Amount cleaned: " + Cleaner.delta(cleaned, annotations).size());
    }
}
