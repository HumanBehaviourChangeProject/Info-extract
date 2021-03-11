package com.ibm.drl.hbcp.parser.cleaning;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.ParsingUtils;
import lombok.Data;
import opennlp.tools.sentdetect.SentenceDetector;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.util.Span;
import org.apache.commons.lang3.StringUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.*;
import java.util.regex.MatchResult;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Extracts the context sentences of an AVP from the PDF it was extracted from.
 *
 * @author mgleize
 */
public class ContextExtractor {

    private final File pdfFolder;
    private final PdfToDocumentFunction pdfToDocument;
    private final int maximumSentencesSpanningContextAndValue;
    private final int sentencesBefore;
    private final int sentencesAfter;

    private final SentenceDetector sbd;
    private final Map<String, Optional<DocumentSentences>> parsedDocuments;

    private static final Pattern WORD_OR_NUMBER_REGEX = Pattern.compile("[a-zA-Z]+|[0-9]+");
    private static final String OTHER_REGEX = "[^a-zA-Z0-9]*";

    public ContextExtractor(File pdfFolder, PdfToDocumentFunction pdfParser, int maximumSentencesSpanningContextAndValue,
                            int numberOfSentencesBefore, int numberOfSentencesAfter) {
        this.pdfFolder = pdfFolder;
        this.pdfToDocument = pdfParser;
        this.maximumSentencesSpanningContextAndValue = maximumSentencesSpanningContextAndValue;
        this.sentencesBefore = numberOfSentencesBefore;
        this.sentencesAfter = numberOfSentencesAfter;
        sbd = newSentenceDetector();
        parsedDocuments = new HashMap<>();
    }

    public String getContext(ArmifiedAttributeValuePair avp) {
        return getExtendedContext(avp).orElse(avp.getContext());
    }

    public Optional<String> getExtendedContext(ArmifiedAttributeValuePair avp) {
        // get the parsed documents
        Optional<DocumentSentences> docMaybe = getParsedDocument(avp.getDocName());
        if (docMaybe.isPresent()) {
            DocumentSentences doc = docMaybe.get();
            Optional<String> extension = getExtendedContextWithIncreasingQuery(avp.getContext(), avp.getValue(), doc);
            List<String> extendedContexts = extension.map(Collections::singletonList).orElse(Collections.emptyList());
            if (extendedContexts.isEmpty()) {
                return Optional.empty();
            } else {
                String newContext = extendedContexts.stream().max(Comparator.comparing(String::length)).get();
                return Optional.of(newContext);
            }
        } else {
            return Optional.empty();
        }
    }

    public boolean isContextIncomplete(AnnotatedAttributeValuePair avp) {
        return contextExcludesValue(avp.getContext(), avp.getValue());
    }

    private Optional<String> getExtendedContextWithIncreasingQuery(String context, String value, DocumentSentences doc) {
        String docText = doc.getDocument();
        // get all the locations where the value is found
        List<Span> valueSpans = getValueSpans(value, docText);
        // query the doc for the context (in a flexible way)
        List<String> words = getAllWords(context);
        for (int startWord = 0; startWord < words.size(); startWord++) {
            for (int endWord = startWord + 1; endWord <= words.size(); endWord++) {
                // build a regex detecting the original context (in a somewhat flexible way)
                Pattern originalContextRegex = buildContextRegex(words.subList(startWord, endWord));
                Matcher matcher = originalContextRegex.matcher(docText);
                // get all the places matching the original context in the annotation
                List<MatchResult> matchResults = getMatchResults(matcher);
                // TODO: revisit this, maybe this is too strict
                if (matchResults.size() == 1 || (endWord - startWord == words.size() && !matchResults.isEmpty())) {
                    return getExtendedContext(matchResults.get(0), valueSpans, doc);
                }
            }
        }
        return Optional.empty();
    }

    private Optional<String> getExtendedContext(MatchResult contextMatch, List<Span> valueSpans, DocumentSentences doc) {
        // get the smallest span containing the context and a value
        Optional<Span> sharedSpan = getSmallestSharedSpan(contextMatch, valueSpans);
        if (sharedSpan.isPresent()) {
            // get sentences from the doc that span the 2
            List<Span> overlappingSentences = getOverlappingSentenceSpans(sharedSpan.get(), doc.sentenceSpans);
            if (overlappingSentences.size() <= maximumSentencesSpanningContextAndValue) {
                // build the final context string
                String res = doc.getDocument().substring(
                        overlappingSentences.get(0).getStart(),
                        overlappingSentences.get(overlappingSentences.size() - 1).getEnd());
                return Optional.of(res);
            } else return Optional.empty();
        } else return Optional.empty();
    }

    private Optional<Span> getSmallestSharedSpan(MatchResult contextMatch, List<Span> valueSpans) {
        Span contextSpan = new Span(contextMatch.start(), contextMatch.end());
        // get the smallest span covering both the context and a value span
        return valueSpans.stream()
                .map(valueSpan -> combineSpans(contextSpan, valueSpan))
                .min(Comparator.comparing(sharedSpan -> sharedSpan.getEnd() - sharedSpan.getStart()));
    }

    private List<Span> getOverlappingSentenceSpans(Span targetSpan, List<Span> sentenceSpans) {
        // find the index of all the sentences overlapping the target span
        List<Integer> sentenceIndices = new ArrayList<>();
        for (int i = 0; i < sentenceSpans.size(); i++) {
            Span sentenceSpan = sentenceSpans.get(i);
            if (targetSpan.intersects(sentenceSpan)) {
                sentenceIndices.add(i);
            }
        }
        List<Span> res = new ArrayList<>();
        if (!sentenceIndices.isEmpty()) {
            // add some sentences before and some sentences after
            int firstSentenceIndex = sentenceIndices.get(0);
            int lastSentenceIndex = sentenceIndices.get(sentenceIndices.size() - 1); // included
            for (int i = Math.max(0, firstSentenceIndex - sentencesBefore); i <= Math.min(lastSentenceIndex + sentencesAfter, sentenceSpans.size() - 1); i++) {
                res.add(sentenceSpans.get(i));
            }
        }
        return res;
    }

    private Span combineSpans(Span span1, Span span2) {
        int start = Math.min(span1.getStart(), span2.getStart());
        int end = Math.max(span1.getEnd(), span2.getEnd());
        return new Span(start, end);
    }

    private List<Span> getValueSpans(String value, String docText) {
        List<Span> res = new ArrayList<>();
        Pattern valuePattern = Pattern.compile(ParsingUtils.escapeRegex(value));
        Matcher matcher = valuePattern.matcher(docText);
        while (matcher.find()) {
            Span span = new Span(matcher.start(), matcher.end());
            res.add(span);
        }
        return res;
    }

    private Pattern buildContextRegex(List<String> words) {
        String regex = StringUtils.join(words, OTHER_REGEX);
        return Pattern.compile(regex, Pattern.MULTILINE);
    }

    private List<MatchResult> getMatchResults(Matcher matcher) {
        List<MatchResult> res = new ArrayList<>();
        while (matcher.find()) {
            res.add(matcher.toMatchResult());
        }
        return res;
    }

    private List<String> getAllWords(String context) {
        List<String> res = new ArrayList<>();
        Matcher matcher = WORD_OR_NUMBER_REGEX.matcher(context);
        while (matcher.find()) {
            res.add(matcher.group());
        }
        return res;
    }

    private String extendContextToNextSpace(int start, int end, String text) { // this is to ensure proper tokenization later
        // extend the start to next space (or index 0)
        while (start > 0 && text.charAt(start - 1) != ' ') {
            start--;
        }
        // extend the end to next space (or end of text)
        while (end < text.length() && text.charAt(end) != ' ') {
            end++;
        }
        return text.substring(start, end);
    }

    private boolean contextExcludesValue(String context, String value) {
        return !context.contains(value);
    }

    private Optional<DocumentSentences> getParsedDocument(String docname) {
        Optional<DocumentSentences> res = parsedDocuments.get(docname);
        if (res == null) { // null means we haven't seen that docname before, Optional.empty() means the docname didn't have an Abbyy parse
            try {
                Document doc = pdfToDocument.getDocument(new File(pdfFolder, docname));
                // apply SBD to it
                String text = doc.getValue(false);
                Span[] spans = sbd.sentPosDetect(text);
                DocumentSentences sentences = new DocumentSentences(text, Arrays.asList(spans));
                res = Optional.of(sentences);
            } catch (IOException | NullPointerException e) {
                // NPE happens when sbd hasn't managed to initialize
                res = Optional.empty();
            }
            parsedDocuments.put(docname, res);
        }
        return res;
    }

    @Data
    private static class DocumentSentences {
        private final String document;
        private final List<Span> sentenceSpans;
    }

    public static SentenceDetector newSentenceDetector() {
        SentenceDetector res = null;
        try (InputStream modelIn = new FileInputStream(FileUtils.potentiallyGetAsResource(new File("data/nlp/models/opennlp/en-sent.bin")))) {
            // Loading sentence detection model
            final SentenceModel sentenceModel = new SentenceModel(modelIn);
            modelIn.close();
            res = new SentenceDetectorME(sentenceModel);
        } catch (IOException e) {
            System.err.println("ContextCompletionCleaner couldn't load OpenNlp's SBD model");
            e.printStackTrace();
        }
        return res;
    }

}
