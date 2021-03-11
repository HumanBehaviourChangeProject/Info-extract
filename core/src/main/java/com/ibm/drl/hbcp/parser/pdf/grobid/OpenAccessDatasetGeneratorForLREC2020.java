package com.ibm.drl.hbcp.parser.pdf.grobid;

import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToGrobidParse;
import com.ibm.drl.hbcp.parser.pdf.reparsing.Reparser;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.Props;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Script class to process and generate the dataset for our submission at LREC2020
 *
 * @author mgleize
 */
public class OpenAccessDatasetGeneratorForLREC2020 {

    final static PdfToDocumentFunction grobidParser = new PdfToGrobidParse();

    public static void writeAllOpenAccessPapersJson(File tabSeparatedOpenAccessDataset, File folder, JSONRefParser annotations) throws Exception {
        try (BufferedReader br = new BufferedReader(new FileReader(tabSeparatedOpenAccessDataset))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] splits = line.split("\t");
                String shortTitle = splits[1];
                Optional<JSONRefParser.PdfInfo> pdfInfo = annotations.getDocInfoFromShortTitle(shortTitle);
                File pdf = new File("data/All_330_PDFs_renamed/" + pdfInfo.get().getFilename());
                try {
                    grobidParser.getDocument(pdf).writeToFile(new File(folder, shortTitle + ".json"));
                } catch (IOException e) {
                    System.err.println("Failed on " + shortTitle);
                }
                System.out.println("Done: " + shortTitle);
            }
        }
    }

    public static void writeAllHumanReadableJsons(File tabSeparatedOpenAccessDataset, File inputJsonFolder, JSONRefParser annotations, File outputJsonFolder) throws IOException {
        try (BufferedReader br = new BufferedReader(new FileReader(tabSeparatedOpenAccessDataset))) {
            String line = br.readLine();
            while ((line = br.readLine()) != null) {
                String[] splits = line.split("\t");
                String shortTitle = splits[1];
                JSONRefParser.PdfInfo pdfInfo = annotations.getDocInfoFromShortTitle(shortTitle).get();
                File extractionJson = new File(inputJsonFolder, shortTitle + ".json");
                Reparser reparsed = new Reparser(extractionJson);
                Document doc = reparsed.getDocument();
                FileUtils.writeJsonToFile(
                        doc.toHumanReadableJson(pdfInfo.getTitle(), pdfInfo.getShortTitle(), pdfInfo.getFilename(), pdfInfo.getIntroduction()),
                        new File(outputJsonFolder, shortTitle + ".json")
                );
                System.out.println("Done: " + shortTitle);
            }
        }
    }

    public static AttributeValueCollection<AnnotatedAttributeValuePair> openAccessDataset(String inputJsonFolder, JSONRefParser annotations) {
        AttributeValueCollection<AnnotatedAttributeValuePair> all = annotations.getAttributeValuePairs();
        System.out.println("All: " + all.size());
        Set<String> shortTitles = Arrays.stream(new File(inputJsonFolder).listFiles())
                .map(File::getName)
                .map(filename -> filename.replaceAll("\\.json", ""))
                .collect(Collectors.toSet());
        List<AnnotatedAttributeValuePair> onlyOpenAccess = new ArrayList<>();
        for (AnnotatedAttributeValuePair value : all.getAllPairs()) {
            String docname = value.getDocName();
            Optional<JSONRefParser.PdfInfo> pdfInfo = annotations.getDocInfo(docname);
            if (pdfInfo.isPresent()) {
                String shortTitle = pdfInfo.get().getShortTitle();
                if (shortTitles.contains(shortTitle)) {
                    onlyOpenAccess.add(value);
                }
            }
        }
        System.out.println("Open-access: " + onlyOpenAccess.size());
        return new AttributeValueCollection<>(onlyOpenAccess);
    }

    public static void main_OpenAccessPaperProcessing(String[] args) throws Exception {
        File openAccess = new File("data/openAcessPapers.txt");
        File folder = new File("data/openaccesspapers_extracted");
        File outputFolder = new File("data/openaccesspapers_extracted_humanreadable");
        JSONRefParser annotations = new JSONRefParser(new File("data/jsons/SmokingPapers407_19Nov19.json"));
        // that's the first step, the actual Grobid PDF parsing
        //w.writeAllOpenAccessPapersJson(openAccess, folder, annotations);
        // second step is print in human-readable format
        writeAllHumanReadableJsons(openAccess, folder, annotations, outputFolder);
    }

    public static void filterAnnotationsForOpenAccess() throws IOException {
        String openAccessJsonsPath = "data/lrec2020/openaccesspapers_extracted_humanreadable";
        JSONRefParser annotations = new JSONRefParser(new File("data/jsons/SmokingPapers407_19Nov19.json"));
        AttributeValueCollection<AnnotatedAttributeValuePair> openAccessAnnotations = openAccessDataset(openAccessJsonsPath, annotations);
        Cleaners cleaners = new Cleaners(Props.loadProperties());
        AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(openAccessAnnotations);
        System.out.println("All done and ready for sequence labeling training (" + cleaned.size() + " values)");
    }

    public static void main(String[] args) throws IOException {
        filterAnnotationsForOpenAccess();
    }
}
