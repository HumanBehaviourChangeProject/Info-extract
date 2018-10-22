/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;
import java.io.FileReader;
import java.util.Properties;
import org.apache.commons.io.FileUtils;

/**
 * Defines the common functionalities to be performed by an indexer object.
 * @author dganguly
 */
public class AbstractIndexer {
    Properties prop;
    String indexDir;
    
    public AbstractIndexer(String propFile) throws Exception {
        prop = new Properties();
        prop.load(new FileReader(propFile));                
    }

    void createIndexDir(String indexDirPath) throws Exception {
        File indexDir = new File(indexDirPath);
        if (!indexDir.exists())
            indexDir.mkdir();
        else {
            removeIndexDir(indexDirPath);
            indexDir.mkdir();            
        }
    }
    
    String getIndexDir() throws Exception {
        return indexDir;
    }
    
    void removeIndexDir(String indexDirPath) throws Exception {
        File indexDir = new File(indexDirPath);
        if (indexDir.exists())
            FileUtils.deleteDirectory(indexDir);
    }
}
