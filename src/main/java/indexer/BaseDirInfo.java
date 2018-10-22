/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package indexer;

import java.io.File;

/**
 * Utility class to 'translate' relative paths from project bases to absolute
 * paths of the file system in the target machine.
 * 
 * @author dganguly
 */
public class BaseDirInfo {
    public static String getBaseDir() {
        return new File("").getAbsolutePath() + "/";
    }
    
    public static String getPath(String path) {
        return new File("").getAbsolutePath() + "/" + path;
    }
}
