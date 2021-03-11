package com.ibm.drl.hbcp.parser.pdf.textract;

import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.MapperFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.PropertyNamingStrategy;
import com.ibm.drl.hbcp.parser.pdf.PdfAnalysisOutput;
import com.ibm.drl.hbcp.parser.pdf.textract.structure.Document;
import lombok.Getter;

import java.io.File;
import java.io.IOException;

public class TextractJsonParser implements PdfAnalysisOutput {

    @Getter
    private final TextractDocument document;

    public TextractJsonParser(File textractApiResponseJsonFile) throws IOException {
        document = getDocument(textractApiResponseJsonFile);
    }

    private static TextractDocument getDocument(File textractApiResponseJsonFile) throws IOException {
        Document parsedOutput = parseDocument(textractApiResponseJsonFile);
        return new TextractDocument(parsedOutput);
    }

    private static Document parseDocument(File jsonFile) throws IOException {
        ObjectMapper objectMapper = new ObjectMapper()
                .configure(MapperFeature.USE_STD_BEAN_NAMING, true)
                .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
        objectMapper.setPropertyNamingStrategy(new PropertyNamingStrategy.UpperCamelCaseStrategy());
        return objectMapper.readValue(jsonFile, Document.class);
    }

    public static void main(String[] args) throws IOException {
        TextractJsonParser parser = new TextractJsonParser(new File("data/pdfs_extracted_amazon/Curry 2003/apiResponse.json"));
        TextractDocument doc = parser.getDocument();
        System.out.println(doc.getValue());
    }
}
