/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.api;

import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extractor.AbstractDetectPresenceAttribute;
import com.ibm.drl.hbcp.extractor.AbstractDetectPresenceAttributesFactory;
import com.ibm.drl.hbcp.extractor.InformationExtractor;
import com.ibm.drl.hbcp.extractor.InformationUnit;
import com.ibm.drl.hbcp.extractor.PopulationGender;
import com.ibm.drl.hbcp.extractor.PopulationMaxAge;
import com.ibm.drl.hbcp.extractor.PopulationMeanAge;
import com.ibm.drl.hbcp.extractor.PopulationMinAge;
import com.ibm.drl.hbcp.extractor.SupervisedIEModel;
import com.ibm.drl.hbcp.extractor.SupervisedIEModels;
import com.ibm.drl.hbcp.inforetrieval.indexer.ResearchDoc;
import io.swagger.annotations.ApiParam;
import java.io.InputStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.store.Directory;
import org.slf4j.LoggerFactory;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import io.swagger.annotations.ApiOperation;

/**
 * This class exposes the information extraction REST APIs through the Swagger interface.
 * @author dganguly
 */

@RestController
@EnableAutoConfiguration(exclude = {org.springframework.boot.autoconfigure.gson.GsonAutoConfiguration.class})
@SpringBootApplication

/*
@EnableConfigurationProperties({
    FileStorageProperties.class
})
*/

public class ExtractorController {

    InformationExtractor persistentExtractor;
    AbstractDetectPresenceAttributesFactory fact;
    List<InformationUnit> bctList;
    SupervisedIEModels models;
    
    static org.slf4j.Logger logger = LoggerFactory.getLogger(ExtractorController.class);
    
    /**
     * This constructor function creates a new 'InformationExtractor' object with the
     * default 'init.properties' file residing on the root of the maven project.
     * It also initialises the list of attributes that are to be extracted through the
     * REST APIs. This function also trains a naive Bayes classifier model on the training
     * set of annotations. Since this is a one-time activity, it does not affect the
     * runtime of the APIs.
    */    
    public ExtractorController() {
        try {
            persistentExtractor = new InformationExtractor(
                    this.getClass().getClassLoader().getResource("init.properties").getPath()
            );
            fact = new AbstractDetectPresenceAttributesFactory(persistentExtractor);
            bctList = fact.createInformationUnits();
            
            // Train models
            logger.debug("###Training supervised models on " + bctList.size() + " BCTs");
            models = new SupervisedIEModels(persistentExtractor, bctList);
            models.trainModels();
            
        }
        catch (Exception ex) {
            Logger.getLogger(ExtractorController.class.getName()).log(Level.SEVERE, null, ex);
        }
    }
    
    InformationExtractor initIE(MultipartFile doc) throws Exception {
        InputStream fstream = doc.getInputStream();
        String fileName = doc.getOriginalFilename();
        
        InMemDocIndexer inMemIndexer = new InMemDocIndexer(fstream, fileName);
        
        Directory ramdir = inMemIndexer.indexFile(0);
        if (ramdir == null) {
            return null;
        }
        
        IndexReader reader = DirectoryReader.open(ramdir);
        
        InformationExtractor extractor = new InformationExtractor(reader);
        return extractor;
    }

    IUnitPOJO getIUnitPOJO(InformationUnit iu, InformationExtractor extractor,
                String fileName, Integer numTopPassagesToRetrieve,
                String wsizes, SupervisedIEModel model, boolean applyNB) throws Exception {
        
        IUnitPOJO iunit;
        iu.setNumWanted(numTopPassagesToRetrieve);
        
        // Write-back the training parameters
        iu.updateQuery(model.getQuery());  // Query
        logger.debug("Model classifier: " + model.getClassifier());
        extractor.setClassifier(model.getClassifier());  // classifier - could be null
        
        String[] windowSizes = wsizes.split(",");
        // setting applyNB = true calls the predict() method of the classifier from the called function
        InformationUnit predicted = extractor.extractInformationFromDoc(InMemDocIndexer.PSEUDO_DOCID, iu, windowSizes, applyNB);

        if (predicted!=null && predicted.getBestAnswer().getKey()!=null) {
            logger.debug("Extracted value: " + predicted.getBestAnswer().getKey());
            iunit = new IUnitPOJO(fileName,
                    predicted.getBestAnswer().getKey(), "", predicted.getBestAnswer().getContext());
        }
        else {
            iunit = new IUnitPOJO(fileName, "Value not found", "", "");
        }
        return iunit;
    } 
    
    IUnitPOJO getIUnitPOJO(InformationUnit iu, InformationExtractor extractor,
                String fileName, Integer numTopPassagesToRetrieve, String wsizes) throws Exception {
        
        IUnitPOJO iunit;
        iu.setNumWanted(numTopPassagesToRetrieve);
        
        String[] windowSizes = wsizes.split(",");                
        InformationUnit predicted = extractor.extractInformationFromDoc(InMemDocIndexer.PSEUDO_DOCID, iu, windowSizes);
        
        if (predicted!=null && predicted.getBestAnswer().getKey()!=null) {
            logger.debug("Extracted value: " + predicted.getBestAnswer().getKey());
            iunit = new IUnitPOJO(fileName, predicted.getBestAnswer().getKey(), "",
                    predicted.getBestAnswer().getContext());
        }
        else {
            iunit = new IUnitPOJO(fileName, "Value not found", "", "");
        }
        return iunit;
    } 
    
    /**
     * This function extracts the minimum age value from a given document
     * after constructing an in-memory passage index by slicing the content of the document
     * object into passages (either sentences or windows).
     *
     * @param numTopPassagesToRetrieve Number of top-ranked passages to use for answer extraction.
     * @param wsizes number of words to define a window; supports specifying comma-separated values for multiple window sizes, e.g. '10,20'
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     */
    @ApiOperation(value = "Extract the mimimum age of the study samples reported in the article",
        notes =
            "Behind the hood, this API function makes use of an in-memory passage index constructed by slicing the content of the supplied document " +
            "into passages (either sentences or windows). The function uses the index to retrieve a set of top most similar passages. " +
            "It then aggregates the likelihood of each candidate 'answer' (minimum age value which is potentially an integer) " +
            "by making use of the differences in positions of the matched query words from " +
            "the candidate value terms (integers within retrieved passages). Finally, it returns the value with the most aggregated score."
    )
    @RequestMapping(value = "/extract/minage",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    protected IUnitPOJO extractMinAge(
            @ApiParam("Number of top passages to retrieve for aggregating scores")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,
            @RequestParam("Select a PDF document from your local filesystem") @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        final String code = "Min Age";
        
        InformationExtractor extractor = initIE(doc);
        if (extractor == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", code, "");
        }
        
        InformationUnit iu = new PopulationMinAge(extractor, ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION);
        
        IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes);
        iunit.setCode(code);
        return iunit;
    }
    
    /**
     * This function extracts the maximum age value from a given document
     * after constructing an in-memory passage index by slicing the content of the document
     * object into passages (either sentences or windows).
     * @param numTopPassagesToRetrieve Number of top-ranked passages to use for answer extraction.
     * @param wsizes number of words to define a window; supports specifying comma-separated values for multiple window sizes, e.g. '10,20'
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     * @throws Exception 
     */
    @ApiOperation(value = "Extract the maximum age of the study samples reported in the article",
        notes =
            "Behind the hood, this API function makes use of an in-memory passage index constructed by slicing the content of the supplied document " +
            "into passages (either sentences or windows). The function uses the index to retrieve a set of top most similar passages. " +
            "It then aggregates the likelihood of each candidate 'answer' (maximum age value which is potentially an integer) " +
            "by making use of the differences in positions of the matched query words from " +
            "the candidate value terms (integers within retrieved passages). Finally, it returns the value with the most aggregated score."
    )    
    @RequestMapping(value = "/extract/maxage",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )    
    protected IUnitPOJO extractMaxAge(
            @ApiParam("Number of top passages to retrieve for aggregating scores")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,
            @RequestParam("Select a PDF document from your local filesystem") 
            @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        InformationExtractor extractor = initIE(doc);
        if (extractor == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", "Max Age", "");
        }
        
        InformationUnit iu = new PopulationMaxAge(extractor,
                ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION);
        
        IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes);
        iunit.setCode("Max Age");
        return iunit;
    }
    
    /**
     * This function extracts the average age value from a given document
     * after constructing an in-memory passage index by slicing the content of the document
     * object into passages (either sentences or windows).
     * @param numTopPassagesToRetrieve Number of top-ranked passages to use for answer extraction.
     * @param wsizes number of words to define a window; supports specifying comma-separated values for multiple window sizes, e.g. '10,20'
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     * @throws Exception 
     */
    @ApiOperation(value = "Extract the mean age of the study samples reported in the article",
        notes =
            "Behind the hood, this API function makes use of an in-memory passage index constructed by slicing the content of the supplied document " +
            "into passages (either sentences or windows). The function uses the index to retrieve a set of top most similar passages. " +
            "It then aggregates the likelihood of each candidate 'answer' (mean age value which is potentially a real number) " +
            "by making use of the differences in positions of the matched query words from " +
            "the candidate value terms (real numbers within retrieved passages). Finally, it returns the value with the most aggregated score."
    )
    @RequestMapping(value = "/extract/meanage",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )    
    protected IUnitPOJO extractAvgAge(
            @ApiParam("Number of top passages to retrieve for aggregating scores")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,            
            @RequestParam("Select a PDF document from your local filesystem") @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        InformationExtractor extractor = initIE(doc);
        
        if (extractor == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", "Mean Age", "");
        }
        
        InformationUnit iu = new PopulationMeanAge(extractor,
                ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION);
        
        IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes);
        iunit.setCode("Mean Age");
        return iunit;
    }
    
    /**
     * This function extracts the gender attribute (Male/Female/Mixed) of the
     * population of a study.
     * @param numTopPassagesToRetrieve Number of top-ranked passages to use for answer extraction.
     * @param wsizes number of words to define a window; supports specifying comma-separated values for multiple window sizes, e.g. '10,20'
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     * @throws Exception 
     */
    @ApiOperation(value = "Extract the gender information of the study samples reported in the article",
        notes =
            "Behind the hood, this API function makes use of an in-memory passage index constructed by slicing the content of the supplied document " +
            "into passages (either sentences or windows). The function uses the index to retrieve a set of top most similar passages. " +
            "It then aggregates the likelihood of each candidate 'answer' (a gender indicating phrase, e.g. \"pregnant women\", \"adult males\" etc.) " +
            "by making use of the differences in positions of the matched query words from " +
            "the candidate value terms (gender indicators within retrieved passages). Finally, it returns the value with the most aggregated score."
    )    
    @RequestMapping(value = "/extract/gender",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )    
    protected IUnitPOJO extractGender(
            @ApiParam("Number of top passages to retrieve for aggregating scores")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,            
            @RequestParam("Select a PDF document from your local filesystem") @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        InformationExtractor extractor = initIE(doc);
        
        if (extractor == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", "Gender", "");
        }
        
        InformationUnit iu = new PopulationGender(extractor,
                ResearchDoc.FIELD_CONTENT, AttributeType.POPULATION);
        
        IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes);
        iunit.setCode("Gender");
        
        return iunit;
    }

    /**
     * Find a BCT to extract from its prefix
     * @param prefix
     * @return 
     */
    InformationUnit findBCT(String prefix) {
        for (InformationUnit iu: bctList) {
            String bctName = iu.getName();
            if (bctName.startsWith(prefix))
                return iu;
        }
        return null;
    }

    /**
     * This function extracts a specified BCT (identified by a given code)
     * from a PDF document. The algorithm used to extract the BCT is unsupervised.
     * Every BCT is associated with a pre-configured query which is used to
     * retrieve a list of ranked passages. The threshold is then applied
     * to output a binary decision about the presence/absence of the BCT.
     * 
     * @param code Identifies a particular BCT with a unique code
     * @param numTopPassagesToRetrieve The number of top-ranked passages to
     * use for extracting the BCT.
     * @param wsizes Comma separated list of values that can be used as window sizes
     * @param threshold The similarity value above which the decision is a '1' (presence of the given BCT).
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     * @throws Exception 
     */
    @ApiOperation(value = "Extract a specified BCT (identified by a given code) from a PDF document using an unsupervised algorithm.",
        notes =
            "Behind the hood, this API function makes use of an in-memory passage index constructed by slicing the content of the supplied document " +
            "into passages (either sentences or windows). The function uses the index to retrieve a set of top most similar passages. " +
            "It then computes the likelihood of a retrieved piece of text indicating the presence of a BCT. " +
            "It makes use of a threshold value (supplied as a parameter) to guide the prediction (which is a Boolean variable) indicating the presence (" +
            "or absence) of the BCT."
    )
    @RequestMapping(value = "/extract/bct",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    protected IUnitPOJO extractBCT(
            @ApiParam("The code of the BCT as per the HBCP ontology, e.g. \"1.1 for Goal setting and planning\"")
            @RequestParam(value="code", required= true, defaultValue = "1.1") String code,            
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,            
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @RequestParam("A PDF document from your local filesystem") @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        InformationUnit iu = findBCT(code);
        if (iu == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Unknown BCT code", "", "");
        }
        
        InformationExtractor extractor = initIE(doc);
        if (extractor == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", iu.getName(), "");
        }
        
        ((AbstractDetectPresenceAttribute)iu).setThreshold(threshold);
        
        IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes);
        iunit.setCode(iu.getName());
        return iunit;
    }

    /**
     * This function extracts a specified BCT (identified by a given code)
     * from a PDF document. The algorithm used to extract the BCT is supervised.
     * Instead of using a pre-configured query to extract a BCT, the system
     * learns the queries from a set of annotated examples of BCTs.
     * Similar to the unsupervised approach, the threshold is then applied
     * to output a binary decision about the presence/absence of the BCT.
     *
     * @param code Identifies a particular BCT with a unique code
     * @param numTopPassagesToRetrieve The number of top-ranked passages to
     * use for extracting the BCT.
     * @param wsizes Comma separated list of values that can be used as window sizes
     * @param threshold The similarity value above which the decision is a '1' (presence of the given BCT).
     * @param applyNB A Boolean parameter indicating whether to apply a one-class only classification
     * or a binary classifier (Naive Bayes) that's trained on pseudo-negative annotations. A pseudo-negative
     * annotation is a passage (which is itself not annotated) but is top-k similar to the query learned from the annotations.
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     */
    @ApiOperation(value = "Extract a specified BCT (identified by a given code) from a PDF document using a supvervised algorithm.",
        notes =
            "Behind the hood, this API function makes use of an in-memory passage index constructed by slicing the content of the supplied document " +
            "into passages (either sentences or windows). The function uses the index to retrieve a set of top most similar passages. " +
            "It then computes the likelihood of a retrieved piece of text indicating the presence of a BCT. " +
            "Instead of a pre-configured query (as specified by a domain expert) to retrieve relevant passages from text indicating the presence of a BCT, " +
            "this API makes a pre-trained model (classfier with both positive and negative annotations or positive-only annotations) to formulate a query for passage retrieval. " +
            "It makes use of a threshold value (supplied as a parameter) to guide the prediction (which is a Boolean variable) indicating the presence (" +
            "or absence) of the BCT."            
    )
    @RequestMapping(value = "/extract/bct/supervised",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    protected IUnitPOJO extractSupervisedBCT(
            @ApiParam("The code of the BCT as per the HBCP ontology, e.g. \"1.1 for Goal setting and planning\"")
            @RequestParam(value="code", required= true, defaultValue = "1.1") String code,            
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,            
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @ApiParam("Whether to use both positive and pseudo-negative annotations (if true) or just positive annotations (if false)")
            @RequestParam(value="textclassifier", required= false, defaultValue = "true") Boolean applyNB,
            @RequestParam("A PDF document from your local filesystem") @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        InformationUnit iu = findBCT(code);
        if (iu == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Unknown BCT code", "", "");
        }
        
        InformationExtractor extractor = initIE(doc);
        if (extractor == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", iu.getName(), "");
        }
        
        String attribId = iu.getAttribId();
        logger.debug("Key attrib: |" + attribId + "|");
        SupervisedIEModel model = models.getModel(attribId);  // get model from mem
        
        if (model == null) {
            return new IUnitPOJO(doc.getOriginalFilename(),
                    "Supervised model not found for BCT code " + code, iu.getName(), "");
        }
        
        ((AbstractDetectPresenceAttribute)iu).setThreshold(threshold);
        
        // model.getIU() -- learnt query and the text classifier (if used by the applyNB classifier)
        IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes, model, applyNB);
        iunit.setCode(iu.getName());
        
        return iunit;
    }
    
    /**
     * This function extracts all BCTs (as per the BCI ontology)
     * from a PDF document. The algorithm used to extract the BCTs is supervised.
     * Instead of using a pre-configured query to extract a BCT, the system
     * learns the queries from a set of annotated examples of BCTs.
     * Similar to the unsupervised approach, the threshold is then applied
     * to output a binary decision about the presence/absence of the BCT.
     *
     * @param numTopPassagesToRetrieve The number of top-ranked passages to
     * use for extracting the BCT.
     * @param wsizes Comma separated list of values that can be used as window sizes
     * @param threshold The similarity value above which the decision is a '1' (presence of the given BCT).
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     */
    @ApiOperation(value = "Extracts all BCTs (as per the HBCP ontology) from a PDF document using a supervised algorithm.",
        notes = "Uses the same set of parameters as 'extractSupervisedBCT'. Does not require the code since it extracts all BCTs in HBCP ontology."            
    )    
    @RequestMapping(value = "/extract/allbcts/supervised",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    protected IUnitPOJOs extractAllSupervisedBCT(
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,            
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @ApiParam("Whether to use both positive and pseudo-negative annotations (if true) or just positive annotations (if false)")
            @RequestParam(value="textclassifier", required= false, defaultValue = "true") Boolean applyNB,
            @RequestParam("file") @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        IUnitPOJOs ius = new IUnitPOJOs();
        InformationExtractor extractor = initIE(doc);
        if (extractor == null) {
            return new IUnitPOJOs(new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", "", ""));
        }

        for (InformationUnit iu: bctList) {

            String attribId = iu.getAttribId();
            SupervisedIEModel model = models.getModel(attribId);  // get model from mem
            
            if (model == null) continue;
            
            ((AbstractDetectPresenceAttribute)iu).setThreshold(threshold);

            // model.getIU() -- learnt query and the text classifier (if used by the applyNB classifier)
            IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                    doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes, model, applyNB);
            iunit.setCode(iu.getName());
        
            ius.add(iunit);
        }
        
        return ius;
    }
    
    /**
     * This function extracts all BCTs (as per the HBCP ontology)
     * from a PDF document. The algorithm used to extract the BCT is unsupervised,
     * i.e. the system uses a pre-configured query to extract each BCT.
     * A threshold corresponding to each BCT is then applied
     * to output a binary decision about the presence/absence of that BCT.
     *
     * @param numTopPassagesToRetrieve The number of top-ranked passages to
     * @param wsizes Comma separated list of values that can be used as window sizes
     * @param threshold The similarity value above which the decision is a '1' (presence of the given BCT).
     * @param doc a 'MultipartFile' document object through the REST API flow.
     * @return An IUnitPOJO object that encodes in JSON the values extracted from the given pdf file.
     */
    @ApiOperation(value = "Extracts all BCTs (as per the HBCP ontology) from a PDF document using an unsupervised algorithm.",
        notes = "Uses the same set of parameters as 'extractBCT'. Does not require the code since it extracts all BCTs in HBCP ontology."             
    )    
    @RequestMapping(value = "/extract/allbcts",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE
    )
    protected IUnitPOJOs extractAllBCTs(
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "3") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,            
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @RequestParam("file") @ApiParam(value="docname", required=true) MultipartFile doc
    ) throws Exception {
        
        IUnitPOJOs ius = new IUnitPOJOs();
        InformationExtractor extractor = initIE(doc);
        if (extractor == null) {
            return new IUnitPOJOs(new IUnitPOJO(doc.getOriginalFilename(),
                    "Could not extract value. Not a PDF file", "", ""));
        }

        for (InformationUnit iu: bctList) {

            ((AbstractDetectPresenceAttribute)iu).setThreshold(threshold);

            IUnitPOJO iunit = getIUnitPOJO(iu, extractor,
                    doc.getOriginalFilename(), numTopPassagesToRetrieve, wsizes);
            iunit.setCode(iu.getName());
            
            ius.add(iunit);
        }
        
        return ius;
    }
    
    public static void main(String[] args) throws Exception {
        SpringApplication.run(ExtractorController.class, args);
    }
    
}
