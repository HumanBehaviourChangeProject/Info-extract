package com.ibm.drl.hbcp.parser.pdf.grobid;

import com.google.common.collect.Lists;
import com.ibm.drl.hbcp.parser.pdf.*;
import lombok.Getter;
import org.grobid.core.exceptions.GrobidException;
import org.grobid.core.layout.LayoutToken;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class GrobidParser implements PdfAnalysisOutput {

    private final File pdf;
    @Getter
    private final Document document;

    public GrobidParser(File pdf) throws Exception {
        this.pdf = pdf;
        document = buildDocument();
    }

    private Document buildDocument() throws Exception {
        // get the section + their text, in order
        Map<String, String> sectionToText = GrobidPDFProcessor.getInstance().getPDFSectionAndText(pdf.getAbsolutePath());
        // make text blocks out of this
        List<Block> textBlocks = sectionToText.values().stream().map(Block::fromText).collect(Collectors.toList());
        // get the tables
        List<NLPLeaderboardTable> tables = GrobidPDFProcessor.getInstance().getCleanedTables(pdf.getAbsolutePath());
        // make table blocks out of them
        List<TableBlock> tableBlocks = tables.stream().map(TableBlock::new).collect(Collectors.toList());
        // build all the blocks: first the text, and at the end, append the table blocks
        List<Block> allBlocks = new ArrayList<>(textBlocks);
        allBlocks.addAll(tableBlocks);
        // build the document
        return () -> Lists.newArrayList(new Page() {
            @Override
            public List<? extends Block> getBlocks() { return allBlocks; }
        });
    }

    private static class TableBlock implements Block {
        @Getter
        private final List<TableValue> table;

        TableBlock(NLPLeaderboardTable table) {
            this.table = table.getNumberCells().stream()
                    .map(this::convert)
                    .collect(Collectors.toList());
        }

        private TableValue convert(NLPLeaderboardTable.TableCell cell) {
            // get text
            String text = joinTokens(cell.lt);
            // get row headers
            List<String> rowHeaders = cell.associatedTags_row.values().stream()
                    .map(tableCell -> joinTokens(tableCell.lt))
                    .collect(Collectors.toList());
            // get column headers
            List<String> columnHeaders = cell.associatedTags_column.values().stream()
                    .map(tableCell -> joinTokens(tableCell.lt))
                    .collect(Collectors.toList());
            return new TableValue(text, rowHeaders, columnHeaders, this);
        }

        private static String joinTokens(List<LayoutToken> tokens) {
            if (tokens.size() >= 2) {
                List<Double> spaces = getSpacesBetweenTokens(tokens);
                double averageSpace = spaces.stream().reduce(0.0, Double::sum) / spaces.size();
                double maxSpace = spaces.stream().max(Double::compareTo).get();
                List<String> delimiters = spaces.stream().map(space -> getDelimiter(space, averageSpace, maxSpace)).collect(Collectors.toList());
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < tokens.size() - 1; i++) {
                    sb.append(tokens.get(i).getText());
                    sb.append(delimiters.get(i));
                }
                sb.append(tokens.get(tokens.size() - 1));
                return sb.toString();
            } else {
                return tokens.stream()
                        .map(LayoutToken::getText)
                        .collect(Collectors.joining(""));
            }
        }

        private static List<Double> getSpacesBetweenTokens(List<LayoutToken> tokens) {
            List<Double> res = new ArrayList<>();
            for (int i = 0; i < tokens.size() - 1; i++) {
                double space = Math.abs(tokens.get(i).x + tokens.get(i).width - tokens.get(i + 1).x);
                res.add(space);
            }
            return res;
        }

        private static String getDelimiter(double space, double averageSpace, double maxSpace) {
            if (space < 0.1) { // basically if the space == 0.0 modulo espilon
                return "";
            } else {
                return " ";
            }
        }

        @Override
        public Type getType() { return Type.TABLE; }

        @Override
        public List<? extends Line> getLines() { return null; }

        @Override
        public String getValue() {
            throw new AssertionError("Never makes sense to call getValue on a Grobid TableBlock.");
        }
    }

    public static void main(String[] args) throws Exception {
        File[] files = { new File("data/All_330_PDFs/Abdullah 2005.pdf"), new File("data/All_330_PDFs/Baker 2006 (c) primary paper.pdf") };
        File[] newPdfFolder = new File("data/NewSmoking83_13Nov19").listFiles();
        for (File pdf : files) {
            if (pdf.getName().endsWith(".pdf")) {
                try {
                    GrobidParser parser = new GrobidParser(pdf);
                    // this is to get an object you can manipulate with code
                    Document doc = parser.getDocument();
                    // this is to write the result of the parsing in a JSON format (the one that's "universally" used for all PDF parsers)
                    //doc.writeToFile(new File("data/NewSmoking83_13Nov19_extracted/" + pdf.getName() + ".json"));
                } catch (GrobidException e) {
                    System.err.println("Grobid couldn't parse " + pdf.getName());
                    e.printStackTrace();
                }
            }
        }
    }
}
