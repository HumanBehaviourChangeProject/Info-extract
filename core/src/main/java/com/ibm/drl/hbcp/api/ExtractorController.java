/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ibm.drl.hbcp.api;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.AttributeType;
import com.ibm.drl.hbcp.extraction.candidates.Candidate;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.extractors.flair.InformationExtractorFlair;
import com.ibm.drl.hbcp.extraction.extractors.flair.NamedParsedDocument;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.indexing.MultipartFileIndexManager;
import com.ibm.drl.hbcp.extraction.indexing.docparsing.AbbyyXmlOutputDocParser;
import com.ibm.drl.hbcp.extraction.indexing.docparsing.PdfDocParser;
import com.ibm.drl.hbcp.extraction.indexing.docparsing.ResearchDocParser;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.abbyy.AbbyyXmlParser;
import com.ibm.drl.hbcp.parser.pdf.tika.TikaParser;
import com.ibm.drl.hbcp.predictor.api.AttributeInfo;
import com.ibm.drl.hbcp.predictor.api.Jsonable;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import lombok.Value;
import org.apache.commons.collections4.map.LRUMap;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.tika.exception.TikaException;
import org.slf4j.LoggerFactory;
import org.slf4j.Logger;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.builder.SpringApplicationBuilder;
import org.springframework.boot.web.servlet.support.SpringBootServletInitializer;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import org.xml.sax.SAXException;

import javax.inject.Named;
import javax.json.Json;
import java.io.File;
import java.io.IOException;
import java.util.*;
import java.util.stream.Collectors;

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

public class ExtractorController extends SpringBootServletInitializer {

    private final Properties props;
    private final Set<String> extractableBctIds;
    private final JSONRefParser parser;

    static Logger log = LoggerFactory.getLogger(ExtractorController.class);

    private final Map<String, DocumentExtractionResult> cache = Collections.synchronizedMap(new LRUMap<>(1000));

    private final Set<String> openAccessDocnames;
    
    /**
     * This constructor function creates a new 'InformationExtractor' object with the
     * default 'init.properties' file residing on the root of the maven project.
     * It also initialises the list of attributes that are to be extracted through the
     * REST APIs. This function also trains a naive Bayes classifier model on the training
     * set of annotations. Since this is a one-time activity, it does not affect the
     * runtime of the APIs.
    */    
    public ExtractorController() throws IOException, ParseException {
        props = Props.loadProperties();
        parser = new JSONRefParser(props);
        openAccessDocnames = getOpenAccessDocnames();
        extractableBctIds = new HashSet<>(Arrays.asList(props.getProperty("attributes.typedetect.ids").split(",")));
    }

    private Set<String> getOpenAccessDocnames() {
        // get the directory with the parsed JSONs
        File directory = FileUtils.potentiallyGetAsResource(new File("data/openaccesspapers_extracted"));
        // get all the JSON filenames
        Set<String> jsonFilenames = Arrays.stream(directory.listFiles())
                .map(File::getName)
                .collect(Collectors.toSet());
        // remove the parentheses and replace .json by .pdf to get mostly valid docnames
        Set<String> res = jsonFilenames.stream()
                .map(jsonName -> jsonName.replaceAll("[()]", ""))
                .map(jsonName -> jsonName.replaceAll("\\.json", ".pdf"))
                .collect(Collectors.toSet());
        log.info("Open access papers: {} papers", res.size());
        return res;
    }

    /**
     * Returns all the intervention attributes handled by the extraction system.
     * Others will not yield relevant results.
     */
    @ApiOperation(value = "Returns the intervention attributes.",
            notes = "Returns all the intervention attributes (e.g., BCTs) handled by the extraction system. " +
                    "Others will not yield relevant results.")
    @RequestMapping(value = "/api/extract/options/intervention", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String interventionOptionsEndpoint() {
        // get all the infos
        List<AttributeInfo> res = extractableBctIds.stream()
                .map(Attributes.get()::getFromId)
                .filter(attr -> attr != null)
                .filter(attr -> attr.getType() == AttributeType.INTERVENTION)
                .map(AttributeInfo::fromAttribute)
                .collect(Collectors.toList());
        // generate the JSON output
        return Jsonable.getJsonList(res);
    }

    /**
     * Returns the outcome value attribute handled by the system.
     */
    @ApiOperation(value = "Returns the outcome value attribute.",
            notes = "Returns all the outcome value attribute handled by the extraction system. ")
    @RequestMapping(value = "/api/extract/options/outcomevalue", method = RequestMethod.GET, produces="application/json;charset=utf-8")
    public String outcomeValueOptionsEndpoint() {
        // get all the infos
        List<AttributeInfo> res = Lists.newArrayList("Outcome value").stream()
                .map(Attributes.get()::getFromName)
                .filter(attr -> attr != null)
                .map(AttributeInfo::fromAttribute)
                .collect(Collectors.toList());
        // generate the JSON output
        return Jsonable.getJsonList(res);
    }

    @ApiOperation(value = "Returns extraction results for a single PDF paper.")
    @RequestMapping(value = "/api/extract/all",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces="application/json;charset=utf-8"
    )
    protected String extractAll(
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "5") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @ApiParam("Whether to use an ABBYY XML file as input instead of a PDF (true to use ABBYY, false to use raw PDF")
            @RequestParam(value = "useAbbyy", required = false, defaultValue = "false") boolean useAbbyy,
            @ApiParam("Whether to use the faster, less accurate unsupervised extraction algorithm")
            @RequestParam(value = "useUnsupervisedBaseline", required = false, defaultValue = "false") boolean useUnsupervisedBaseline,
            @ApiParam(value="An uploaded Behavior Changer paper in PDF format", required = true)
            @RequestParam(value="file")  MultipartFile doc
    ) throws TikaException, IOException, SAXException, ParseException {
        List<List<ArmifiedAttributeValuePair>> resWithPage = extractMultipartFilesCached(
                numTopPassagesToRetrieve, wsizes, threshold,
                useAbbyy, useUnsupervisedBaseline,
                new MultipartFile[] { doc }
        );
        return Jsonable.getJsonList(resWithPage.get(0));
    }

    @ApiOperation(value = "Returns extraction results for a batch of PDF papers.")
    @RequestMapping(value = "/api/extract/all/multi",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces="application/json;charset=utf-8"
    )
    protected String extractAllMulti(
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "5") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @ApiParam("Whether to use an ABBYY XML file as input instead of a PDF (true to use ABBYY, false to use raw PDF")
            @RequestParam(value = "useAbbyy", required = false, defaultValue = "false") boolean useAbbyy,
            @ApiParam("Whether to use the faster, less accurate unsupervised extraction algorithm")
            @RequestParam(value = "useUnsupervisedBaseline", required = false, defaultValue = "false") boolean useUnsupervisedBaseline,
            @ApiParam(value = "One or more uploaded Behavior Changer papers in PDF format", required = true)
            @RequestParam("files") MultipartFile[] docs
    ) throws TikaException, IOException, SAXException, ParseException {
        log.info("Requesting batch extraction for {} documents.", docs.length);
        // build the result JSON object
        List<List<ArmifiedAttributeValuePair>> results = extractMultipartFilesCached(
                numTopPassagesToRetrieve, wsizes, threshold,
                useAbbyy, useUnsupervisedBaseline,
                docs
        );
        Map<String, List<ArmifiedAttributeValuePair>> docNameToResults = new HashMap<>();
        for (int i = 0; i < docs.length; i++) {
            docNameToResults.put(getFilename(docs[i]), results.get(i));
        }
        return Jsonable.getJsonStringToListMap(docNameToResults);
        /* Legacy call
        Properties extraProps = new Properties();
        extraProps.setProperty("ntoppassages", String.valueOf(numTopPassagesToRetrieve));
        extraProps.setProperty("threshold", String.valueOf(threshold));
        ResearchDocParser docParser = useAbbyy ? new AbbyyXmlOutputDocParser() : new PdfDocParser();
        List<ExtractedArmDocument> res = new ArrayList<>();
        for (MultipartFile doc : docs) {
            String filename = doc.getOriginalFilename();
            System.out.println("Requesting extraction for " + filename);
            String title = "";
            List<ArmifiedAttributeValuePair> extractedValues = cache.get(filename);
            if (extractedValues == null) {
                try (IndexManager singleDocIndex = new MultipartFileIndexManager(doc, docParser, filename, wsizes.split(","))) {
                    title = singleDocIndex.getTitle(0).orElse("");
                    List<CandidateInPassage<ArmifiedAttributeValuePair>> valuesForOneDoc = new ArrayList<>();
                    try (com.ibm.drl.hbcp.extraction.extractors.InformationExtractor extractor =
                                 new com.ibm.drl.hbcp.extraction.extractors.InformationExtractor(Props.loadProperties(), extraProps)) {
                        for (IndexedDocument indexedDoc : singleDocIndex.getAllDocuments()) { // there should be only one
                            Collection<CandidateInPassage<ArmifiedAttributeValuePair>> values = extractor.extract(indexedDoc);
                            valuesForOneDoc.addAll(values);
                        }
                    }
                    // add docname and pageNumbers
                    List<ArmifiedAttributeValuePair> valuesWithPage = Candidate.unwrap(valuesForOneDoc).stream()
                            .map(avp -> new ArmifiedAttributeValuePair(avp.getAttribute(), avp.getValue(), filename, avp.getArm(), avp.getContext()))
                            .map(avp -> ArmifiedAttributeValuePair.withPage(avp, singleDocIndex.getPage(avp.getContext(), 0)))
                            .collect(Collectors.toList());
                    extractedValues = valuesWithPage;
                    cache.put(filename, extractedValues);
                }
            } else { log.info("Found in cache."); }
            // add to the overall results
            res.addAll(ExtractedArmDocument.getDocuments(filename, title, "", extractedValues));

        }
        return res;
        */
    }

    protected List<List<ArmifiedAttributeValuePair>> extractMultipartFilesCached(int numTopPassagesToRetrieve, String wsizes,
                                                                  float threshold, boolean useAbbyy, boolean useUnsupervisedBaseline,
                                                                  MultipartFile[] docs) throws TikaException, IOException, SAXException, ParseException {
        // look up cached results for all documents
        DocumentExtractionResult[] results = new DocumentExtractionResult[docs.length];
        for (int i = 0; i < docs.length; i++) {
            results[i] = cache.get(getFilename(docs[i]));
        }
        // perform the batch extraction for missing results
        List<MultipartFile> missingDocs = new ArrayList<>();
        for (int i = 0; i < results.length; i++) {
            if (results[i] == null) {
                missingDocs.add(docs[i]);
            } else {
                log.info("Found in cache: {}", getFilename(docs[i]));
            }
        }
        List<List<ArmifiedAttributeValuePair>> missingResults = extractMultipartFiles(numTopPassagesToRetrieve, wsizes,
                threshold, useAbbyy, useUnsupervisedBaseline, missingDocs);
        // add to cache
        for (int i = 0; i < missingDocs.size(); i++) {
            String filename = getFilename(missingDocs.get(i));
            cache.put(filename, new DocumentExtractionResult(filename, missingResults.get(i)));
        }
        // return results
        List<List<ArmifiedAttributeValuePair>> res = new ArrayList<>();
        int iMissingDocs = 0;
        for (DocumentExtractionResult result : results) {
            if (result != null) {
                res.add(result.getExtractedValues());
            } else {
                res.add(missingResults.get(iMissingDocs++));
            }
        }
        return res;
    }

    protected List<List<ArmifiedAttributeValuePair>> extractMultipartFiles(int numTopPassagesToRetrieve, String wsizes,
                                                                           float threshold, boolean useAbbyy, boolean useUnsupervisedBaseline,
                                                                           List<MultipartFile> docs) throws TikaException, IOException, SAXException, ParseException {
        // prepare the extraction: properties, parsers
        Properties extraProps = new Properties();
        extraProps.setProperty("ntoppassages", String.valueOf(numTopPassagesToRetrieve));
        extraProps.setProperty("threshold", String.valueOf(threshold));
        ResearchDocParser docParser = useAbbyy ? new AbbyyXmlOutputDocParser() : new PdfDocParser();
        // final results will be filled by either Flair API or the unsupervised baseline, then page numbers will be added
        List<List<ArmifiedAttributeValuePair>> resWithoutPages = new ArrayList<>();
        if (!useUnsupervisedBaseline) { // using Flair
            InformationExtractorFlair flair = new InformationExtractorFlair();
            List<NamedParsedDocument> parsedDocuments = new ArrayList<>();
            for (MultipartFile doc : docs) {
                // parse the document (PDF, or ABBYY output)
                Document parsedDocument = useAbbyy ? new AbbyyXmlParser(doc.getInputStream()).getDocument() : new TikaParser(doc.getInputStream()).getDocument();
                NamedParsedDocument input = new NamedParsedDocument(getFilename(doc), parsedDocument);
                parsedDocuments.add(input);
            }
            // batch call to the Flair API
            log.info("Extracting using Flair API on {} documents...", parsedDocuments.size());
            List<List<CandidateInPassage<ArmifiedAttributeValuePair>>> candidates = flair.extract(parsedDocuments);
            // unwrap candidates
            resWithoutPages = candidates.stream().map(Candidate::unwrap).collect(Collectors.toList());
        }
        // in the next step we assign page numbers, after optionally using the unsupervised baseline to extract the results
        List<List<ArmifiedAttributeValuePair>> res = new ArrayList<>();
        for (int i = 0; i < docs.size(); i++) {
            MultipartFile doc = docs.get(i);
            try (IndexManager singleDocIndex = new MultipartFileIndexManager(doc, docParser, getFilename(doc), wsizes.split(","))) {
                if (useUnsupervisedBaseline) {
                    log.info("Extracting using unsupervised baseline: {}...", getFilename(doc));
                    // using Lucene+Regex-based unsupervised baseline
                    try (com.ibm.drl.hbcp.extraction.extractors.InformationExtractor extractor =
                                 new com.ibm.drl.hbcp.extraction.extractors.InformationExtractor(Props.loadProperties(), extraProps)) {
                        for (IndexedDocument indexedDoc : singleDocIndex.getAllDocuments()) { // there should be only one
                            Collection<CandidateInPassage<ArmifiedAttributeValuePair>> values = extractor.extract(indexedDoc);
                            resWithoutPages.add(Candidate.unwrap(new ArrayList<>(values)));
                        }
                    }
                }
                // assign page numbers
                List<ArmifiedAttributeValuePair> valuesWithoutPage = resWithoutPages.get(i);
                List<ArmifiedAttributeValuePair> valuesWithPage = valuesWithoutPage.stream()
                        .map(avp -> new ArmifiedAttributeValuePair(avp.getAttribute(), avp.getValue(), getFilename(doc), avp.getArm(), avp.getContext()))
                        .map(avp -> ArmifiedAttributeValuePair.withPage(avp, singleDocIndex.getPage(avp.getContext(), 0)))
                        .collect(Collectors.toList());
                res.add(valuesWithPage);
            }
        }
        return res;
    }

    @ApiOperation(value = "Returns extraction results for a batch of PDF papers, using the annotations for these papers.",
            hidden = true)
    @RequestMapping(value = "/api/extract/all/multi/annotations",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces="application/json;charset=utf-8"
    )
    protected String extractAllMultiAnnotations(
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "5") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @ApiParam(value="Uploaded Behavior Changer papers in PDF format")
            @RequestParam(value="files")  MultipartFile[] docs
    ) {
        System.out.println("/api/extract/all/multi/annotations");
        return Jsonable.getJsonList(
                getAllMultiAnnotations(
                        numTopPassagesToRetrieve, wsizes, threshold,
                        Arrays.stream(docs).map(MultipartFile::getOriginalFilename).toArray(String[]::new)
                )
        );
    }

    @ApiOperation(value = "Returns all the annotations available for all the annotated papers.",
            hidden = true)
    @RequestMapping(value = "/api/extract/all/multi/annotations/all",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces="application/json;charset=utf-8"
    )
    protected String extractAllMultiAnnotationsAllFiles(
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "5") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold
    ) {
        System.out.println("/api/extract/all/multi/annotations/all");
        String[] allFilenames = parser.getAttributeValuePairs().getDocNames().toArray(new String[0]);
        return Jsonable.getJsonList(getAllMultiAnnotations(numTopPassagesToRetrieve, wsizes, threshold, allFilenames));
    }

    protected List<ExtractedArmDocument> getAllMultiAnnotations(int numTopPassagesToRetrieve, String wsizes,
                                                                float threshold, String[] filenames) {
        List<ExtractedArmDocument> res = new ArrayList<>();
        for (int i = 0; i < filenames.length; i++) {
            String filename = filenames[i];
            // first check that it's in the list of openaccess papers (GDPR compliance baby)
            if (openAccessDocnames.contains(filename)) {
                System.out.println("Requesting annotations for " + filename);
                Collection<AnnotatedAttributeValuePair> annotatedValues = parser.getAttributeValuePairs().byDoc().get(filename);
                if (annotatedValues == null) {
                    System.err.println("No annotations for file: " + filename);
                    annotatedValues = new ArrayList<>();
                }
                Optional<JSONRefParser.PdfInfo> info = parser.getDocInfo(filename);
                res.addAll(ExtractedArmDocument.getDocuments(
                        filename,
                        info.map(JSONRefParser.PdfInfo::getTitle).orElse(""),
                        info.map(JSONRefParser.PdfInfo::getIntroduction).orElse(""),
                        new ArrayList<>(annotatedValues)));
            }
        }
        return res;
    }

    @ApiOperation(value = "Returns the manual annotations for a single PDF paper.",
            hidden = true)
    @RequestMapping(value = "/api/extract/all/annotations",
            method = RequestMethod.POST,
            consumes = MediaType.MULTIPART_FORM_DATA_VALUE,
            produces="application/json;charset=utf-8"
    )
    protected String extractAllAnnotations(
            @ApiParam("Number of top passages to retrieve for aggregating the confidences of BCT presence")
            @RequestParam(value="ntoppassages", required= false, defaultValue = "5") Integer numTopPassagesToRetrieve,
            @ApiParam("A comma separated list of window sizes to use, e.g. '10,20'")
            @RequestParam(value="wsizes", required= false, defaultValue = "10,20") String wsizes,
            @ApiParam("A threshold value within the range of [0, 1] (e.g. 0.25)")
            @RequestParam(value="threshold", required= false, defaultValue = "0.2") Float threshold,
            @ApiParam(value="An uploaded Behavior Changer paper in PDF format")
            @RequestParam(value="file")  MultipartFile doc
    ) {
        String filename = doc.getOriginalFilename();
        System.out.println("Requesting annotations for " + filename);
        Collection<AnnotatedAttributeValuePair> res = parser.getAttributeValuePairs().byDoc().get(filename);
        if (res == null) {
            System.err.println("No annotations for file: " + filename);
            res = new ArrayList<>();
        }
        return Jsonable.getJsonList(new ArrayList<>(res));
    }

    private String getFilename(MultipartFile file) {
        return file.getOriginalFilename();
    }

    @Value
    private static class DocumentExtractionResult {
        String filename;
        List<ArmifiedAttributeValuePair> extractedValues;
    }

    @Override
    protected SpringApplicationBuilder configure(SpringApplicationBuilder application) {
        return application.sources(ExtractorController.class);
    }
    
    public static void main(String[] args) throws Exception {
        SpringApplication.run(ExtractorController.class, args);
    }
    
}
