package com.ibm.drl.hbcp.extraction.indexing;

import com.google.common.collect.Lists;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

public class DocumentInputStream {

    private final InputStream inputStream;
    private final String docName;

    public DocumentInputStream(InputStream inputStream, String docName) {
        this.inputStream = inputStream;
        this.docName = docName;
    }

    public static List<DocumentInputStream> fromFile(File file) throws IOException {
        return Lists.newArrayList(new DocumentInputStream(new FileInputStream(file), file.getName()));
    }

    public static List<DocumentInputStream> fromFiles(List<File> files) throws IOException {
        List<DocumentInputStream> res = new ArrayList<>();
        for (File file : files) {
            res.add(new DocumentInputStream(new FileInputStream(file), file.getName()));
        }
        return res;
    }

    public InputStream getInputStream() { return inputStream; }

    public String getDocName() { return docName; }

    @Override
    public String toString() {
        return "File: " + docName;
    }
}
