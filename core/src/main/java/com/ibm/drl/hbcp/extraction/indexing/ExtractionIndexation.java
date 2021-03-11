package com.ibm.drl.hbcp.extraction.indexing;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.apache.lucene.queryparser.classic.ParseException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ibm.drl.hbcp.extraction.extractors.InformationExtractor;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;

public class ExtractionIndexation {

	private static final Logger log = LoggerFactory.getLogger(ExtractionIndexation.class);

    public static void ensure(Properties props) throws IOException, ParseException {
        File indexFolder = new File(BaseDirInfo.getPath(props.getProperty("ie.index")));
        if (needsToRun(indexFolder)) {
            log.info("Now running the extraction and indexing its output...");
            InformationExtractor.extractAndStoreInIndex(new String[0]);
            log.info("... extraction and indexing done.");
        }
    }

    private static boolean needsToRun(File indexFolder) {
        if (!indexFolder.exists()) {
            return true;
        } else {
            File[] contents = indexFolder.listFiles();
            return contents == null || contents.length == 0;
        }
    }

    /**
     * An object describing Lucene-indexed fields for values extracted in documents.
     *
     * @author dganguly
     */

    public static class ExtractionIndexedFields {

        public static final String ATTRIB_TYPE_FIELD = "ATTRIB_TYPE";
        public static final String ATTRIB_ID_FIELD = "ATTRIB_ID";
        public static final String ATTRIB_NAME_FIELD = "ATTRIB_NAME";
        public static final String EXTRACTED_VALUE_FIELD = "EXTRACTED_VALUE";
        public static final String ARM_ID = "ARM_ID";
        public static final String ARM_NAME = "ARM_NAME";
        public static final String JSON_FIELD = "JSON";
        public static final String CONTEXT_FIELD = "CONTEXT";
        public static final String DOCNAME_FIELD = "DOCNAME";
    }
}
