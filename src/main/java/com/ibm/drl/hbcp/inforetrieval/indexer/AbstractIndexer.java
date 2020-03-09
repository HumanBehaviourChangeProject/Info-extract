/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.inforetrieval.indexer;

import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Properties;

/**
 * Implements some of the common functionalities for creating/removing indexes.
 * @author dganguly
 */
public class AbstractIndexer {
    protected final Properties prop;
    String indexDir;

    public AbstractIndexer(Properties props) throws IOException {
        prop = props;
    }

    public AbstractIndexer(String propFile) throws IOException {
        this(Props.loadProperties(propFile));
    }

    /**
     * Creates an index directory
     * @param indexDirPath Path relative to the base directory of the project.
     * @throws IOException 
     */
    void createIndexDir(String indexDirPath) throws IOException {
        File indexDir = new File(indexDirPath);
        if (!indexDir.exists())
            indexDir.mkdir();
        else {
            removeIndexDir(indexDirPath);
            indexDir.mkdir();            
        }
    }
    
    String getIndexDir() {
        return indexDir;
    }
    
    /**
     * Removes the index stored in a specified path.
     * @param indexDirPath
     * @throws IOException 
     */
    void removeIndexDir(String indexDirPath) throws IOException {
        File indexDir = new File(indexDirPath);
        if (indexDir.exists())
            FileUtils.deleteDirectory(indexDir);
    }
}
