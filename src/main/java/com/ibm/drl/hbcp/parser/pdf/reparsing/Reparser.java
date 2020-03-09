package com.ibm.drl.hbcp.parser.pdf.reparsing;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.PdfAnalysisOutput;
import com.ibm.drl.hbcp.parser.pdf.reparsing.structure.ReparseDocument;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public class Reparser implements PdfAnalysisOutput {

    @Getter
    private final ReparseDocument document;

    public Reparser(File documentJson) throws IOException {
        document = parseDocument(documentJson);
    }

    private static ReparseDocument parseDocument(File jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(PropertyNamingStrategy.LOWER_CAMEL_CASE);
        return objectMapper.readValue(jsonFile, ReparseDocument.class);
    }

    public static void main(String[] args) throws IOException {
        Reparser parser = new Reparser(new File("data/All_330_PDFs_extracted/Harris 2010.pdf.json"));
        Document doc = parser.getDocument();
        System.out.println(doc.toPrettyString());
    }
}
