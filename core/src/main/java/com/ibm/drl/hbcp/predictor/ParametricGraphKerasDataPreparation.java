package com.ibm.drl.hbcp.predictor;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.core.wvec.NodeVecs;
import com.ibm.drl.hbcp.core.wvec.WordVecs;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.predictor.data.TrainTestSplitter;
import com.ibm.drl.hbcp.predictor.graph.AttribNodeRelations;
import com.ibm.drl.hbcp.predictor.graph.Node2Vec;
import com.ibm.drl.hbcp.predictor.graph.RelationGraphBuilder;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;
import lombok.Value;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;

import java.util.List;
import java.util.Properties;

public class ParametricGraphKerasDataPreparation extends PredictionWorkflow {

    private final boolean withWords;
    WordVecs contextVecs;

    private static final Logger log = LoggerFactory.getLogger(ParametricGraphKerasDataPreparation.class);

    public ParametricGraphKerasDataPreparation(AttributeValueCollection<? extends ArmifiedAttributeValuePair> values,
                                               AttributeValueCollection<? extends ArmifiedAttributeValuePair> annotations,
                                               TrainTestSplitter splitter,
                                               Properties props,
                                               String pubmedPath,
                                               String concatenatedNodeWordPath,
                                               boolean withWords, String contextVecFile) throws IOException {
        super(values, annotations, splitter, props);
        getInstancesManager().initWithPreTrainedWordVecs(pubmedPath, concatenatedNodeWordPath);
        this.withWords = withWords;
        if (contextVecFile != null)
            contextVecs = loadContextVecs(contextVecFile);
    }

    final private WordVecs loadContextVecs(String contextVecFile) throws FileNotFoundException {
        WordVecs wordVecs = new WordVecs(new FileInputStream(contextVecFile), "\t");
        return wordVecs;
    }

    public void prepareData(String outNodeVecFileName, String trainFile, String testFile) throws IOException {
        prepareData(outNodeVecFileName, trainFile, testFile, false);
    }

    public void prepareData(String outNodeVecFileName, String trainFile, String testFile, boolean extracted) throws IOException {
        boolean includeTextAttribs = false;
        NodeVecs ndvecs = null;

        if (outNodeVecFileName != null) {
            final RelationGraphBuilder rgb = new RelationGraphBuilder(props, getTrainAVPs());
            final AttribNodeRelations graph = rgb.getGraph(true);
            Node2Vec node2Vec = new Node2Vec(graph, props);  // this also writes embedding vector to disk
            // Save the embedding vectors to be used in the Keras flow
            System.out.println("Saving vec file to " + outNodeVecFileName);
            ndvecs = node2Vec.trainSaveAndGetNodeVecs(outNodeVecFileName);
        }

        //System.out.println("ndvecs = " + ndvecs.getDimension());

        int startArmId = !extracted? 0: 1000; // a big enough number
        getInstancesManager().writeTrainTestFiles(
                trainFile, getTrainingDocs(), testFile, getTestDocs(),
                includeTextAttribs, ndvecs, withWords, contextVecs, startArmId);

        System.out.println("oov% = " + getInstancesManager().oov());

        // will need to evaluate after keras has run... so no evaluation here
    }

    private List<String> getTrainingDocs() { return docnamesTrainTest.getLeft(); }
    private List<String> getTestDocs() { return docnamesTrainTest.getRight(); }

    public static ParametricGraphKerasDataPreparation prepareData(Properties props,
                                                                  AttributeValueCollection<ArmifiedAttributeValuePair> annotations,
                                                                  String nodeTextConactDictFile) throws IOException {
        boolean withWords = Boolean.parseBoolean(props.getProperty("seqmodel.use_text", "true"));

        // A string parameter to indicate the context vector file (for the time being Bio-BERT).
        String contextVecsPath = props.getProperty("context.vecs");

        float trainRatio = Float.parseFloat(props.getProperty("prediction.train.ratio", "1.0"));

        log.info("Preparing graph embeddings for prediction model...");

        ParametricGraphKerasDataPreparation flow = new ParametricGraphKerasDataPreparation(
                annotations,
                annotations,
                new TrainTestSplitter(trainRatio),
                props,
                "../data/pubmed/hbcpcontext.vecs",
                nodeTextConactDictFile,
                withWords,
                contextVecsPath
        );
        return flow;
    }

    /** The beginning of the script scripts/predictionresources4api.sh in Java, for use from the API to train prediction
     * models on the fly
     * ONLY VALID ON UNIX SYSTEMS, NOT WINDOWS */
    public static ResourcePaths predictionResources4ApiStart(AttributeValueCollection<AnnotatedAttributeValuePair> annotations,
                                                    int dimension, int node2VecWindow,
                                                    double node2VecP, double node2VecQ) throws IOException, InterruptedException {
        Properties extraProps = PredictionResources4ApiPropertiesGenerator.generate(dimension, node2VecWindow, node2VecP, node2VecQ);
        main(annotations, extraProps);
        // executes the end script that merges the global and per-instance vectors
        String endScriptName = "predictionresources4apiEnd.sh";
        String scriptPath = FileUtils.potentiallyGetAsResource(new File("scripts/", endScriptName)).getAbsolutePath();
        ProcessBuilder pb = new ProcessBuilder("sh", scriptPath,
                PredictionResources4ApiPropertiesGenerator.GNODE_FILE,
                PredictionResources4ApiPropertiesGenerator.INODE_FILE,
                PredictionResources4ApiPropertiesGenerator.MERGED_NODE_FILE
        );
        pb.inheritIO();
        pb.directory(new File("."));
        Process process = pb.start();
        int exitCode = process.waitFor();
        // here, the folder resources contains a train set and vector files for prediction training
        if (exitCode == 0) {
            return new ResourcePaths(new File(".", PredictionResources4ApiPropertiesGenerator.TRAIN_FILE),
                    new File(".", PredictionResources4ApiPropertiesGenerator.MERGED_NODE_FILE));
        } else {
            throw new IOException("Error during merging of embedding files");
        }
    }

    @Value
    public static class ResourcePaths {
        private File trainFile;
        private File mergedVecFile;
    }


    public static void main(AttributeValueCollection<AnnotatedAttributeValuePair> annotations,
                                                    Properties extraProperties) throws IOException {
        Properties props = Props.overrideProps(Props.loadProperties(), extraProperties);

        log.info("Annotation stats for prediction training: {} entities in {} documents", annotations.size(), annotations.getDocNames().size());
        String nodeTextConactDictFile = props.getProperty("seqmodel.modified_dict", "prediction/graphs/nodevecs/nodes_and_words.vec");

        ParametricGraphKerasDataPreparation flow = prepareData(props, AttributeValueCollection.cast(annotations), nodeTextConactDictFile);

        final String defaultDir = props.getProperty("out.seqfiles.dir", "prediction/sentences/");
        String trainFile = props.getProperty("out.train.seqfile", defaultDir + "train.tsv");
        String testFile = props.getProperty("out.test.seqfile", defaultDir + "test.tsv");
        String outNodeVecFileName = props.getProperty("nodevecs.vecfile", "prediction/graphs/nodevecs/refVecs.vec");
        boolean useGraph = Boolean.parseBoolean(props.getProperty("seqmodel.use_graph", "true"));
        if (!useGraph)
            outNodeVecFileName = null;

        flow.prepareData(outNodeVecFileName, trainFile, testFile);
    }

    public static void main(String[] args) throws IOException {
        String additionalPropsFile = args.length>0? args[0]: null;
        Properties extraProps = new Properties();
        if (additionalPropsFile != null) {
            extraProps = Props.loadProperties(additionalPropsFile);
        }
        // the prop file defined above shouldn't override "ref.json", so the method called next is right to ignore these props
        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = JSONRefParser.loadAnnotationsForPredictionTraining();
        main(annotations, extraProps);
    }
}
