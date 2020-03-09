package com.ibm.drl.hbcp.util;

import com.ibm.drl.hbcp.predictor.api.Jsonable;

import javax.json.JsonValue;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Path;

public class FileUtils {

    public static void writeToFile(Object o, File file) throws IOException {
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(o.toString());
        }
    }

    public static File potentiallyGetAsResource(File file) {
        // will first try to get a resource with decreasingly faithful paths
        ClassLoader loader = Thread.currentThread().getContextClassLoader();
        Path original = file.toPath();
        for (int i = 0; i < original.getNameCount(); i++) {
            // remove 'i' ancestors from the path, starting from the root
            Path path = original.subpath(i, original.getNameCount());
            URL url = loader.getResource(path.toString());
            if (url != null) {
                try {
                    return new File(url.toURI());
                } catch (URISyntaxException e) {
                    // the path name wasn't valid for some reason, signal it but don't crash the program here
                    // it will likely crash later when the caller tries to use an invalid File :D
                    e.printStackTrace();
                    return file;
                }
            }
            // null happens if the path was valid, but not pointing to a resource
        }
        // at this point none of the paths tried was a valid resource
        System.out.println("Warning: resource loading failed, using original path: " + file.getAbsolutePath());
        return file;
    }

    public static void writeJsonToFile(JsonValue json, File file) throws IOException {
        // create the folders if needed
        File parent = file.getParentFile();
        parent.mkdirs();
        // write the file
        try (BufferedWriter bw = new BufferedWriter(new FileWriter(file))) {
            bw.write(new Jsonable() {
                @Override
                public JsonValue toJson() {
                    return json;
                }
            }.toPrettyString());
            bw.newLine();
        }
    }
}
