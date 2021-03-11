/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.predictor.semisupervised;

import static com.ibm.drl.hbcp.predictor.ParametricGraphKerasDataPreparation.prepareData;

import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.ParametricGraphKerasDataPreparation;
import com.ibm.drl.hbcp.predictor.crossvalid.CrossValidationSplitter;
import com.ibm.drl.hbcp.util.Props;

/**
 *
 * @author debforit
 */
public class MixedSourceDataPreparator {
    
    public static void main(String[] args) throws IOException {
        String additionalProps = args.length>0? args[0]: null;
        Properties extraProps = new Properties();
        if (additionalProps != null) {
            extraProps.load(new FileReader(additionalProps)); // overriding arguments
        }
        Properties props = Props.overrideProps(Props.loadProperties(), extraProps);
        
        // We want to write out everything in the training file for 5-fold cv
        props.setProperty("prediction.train.ratio", "1.0");

        JSONRefParser parser = new JSONRefParser(props);
        // Reference values
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = parser.getAttributeValuePairs();
        
        // Noisy (extracted values)
        AttributeValueCollection<ArmifiedAttributeValuePair> values = CrossValidationSplitter.createFromExtractedIndex(parser, props);

        final String dir = "prediction/sentences/";        
        String trainFile = dir + props.getProperty("out.train.seqfile", "train.tsv");
        String testFile = dir + props.getProperty("out.test.seqfile", "test.tsv");
        String noisyTrainFile = dir + props.getProperty("out.noisy.train.seqfile", "train.noisy.tsv");
        String noisyTestFile = dir + props.getProperty("out.noisy.test.seqfile", "test.noisy.tsv");
        
        String outNodeVecFileName = props.getProperty("nodevecs.vecfile", "prediction/graphs/nodevecs/nodes_ref.vec");
        String extractedOutNodeVecFileName = props.getProperty("nodevecs.vecfile.extracted", "prediction/graphs/nodevecs/nodes_extracted.vec");
        
        boolean useGraph = Boolean.parseBoolean(props.getProperty("seqmodel.use_graph", "true"));
        if (!useGraph)
            outNodeVecFileName = null;
        
        String nodeRefTextConactDictFile = props.getProperty("seqmodel.modified_dict", "prediction/graphs/nodevecs/nodes_and_words_ref.vec");
        String nodeExtractedTextConactDictFile = props.getProperty("seqmodel.modified_dict", "prediction/graphs/nodevecs/nodes_and_words_extracted.vec");
        
        ParametricGraphKerasDataPreparation flow = prepareData(props, AttributeValueCollection.cast(annotations), nodeRefTextConactDictFile);        
        ParametricGraphKerasDataPreparation noisyFlow = prepareData(props, values, nodeExtractedTextConactDictFile);
        
        // write out attrib-value pair sequence (for Keras flow) for clean (ref) data
        flow.prepareData(outNodeVecFileName, trainFile, testFile);
        // write out attrib-value pair sequence (for Keras flow) for noisy (extracted) data
        noisyFlow.prepareData(extractedOutNodeVecFileName, noisyTrainFile, noisyTestFile, true);
    }
}
