package com.ibm.drl.hbcp.extraction.indexing;

import com.ibm.drl.hbcp.extraction.extractors.InformationExtractor;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import org.apache.lucene.queryparser.classic.ParseException;

import java.io.File;
import java.io.IOException;
import java.util.Properties;

public class ExtractionIndexation {

    public static void ensure(Properties props) throws IOException, ParseException {
        File indexFolder = new File(BaseDirInfo.getPath(props.getProperty("ie.index")));
        if (needsToRun(indexFolder)) {
            System.out.println("Now running the extraction and indexing its output...");
            InformationExtractor.extractAndStoreInIndex(new String[0]);
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
}
