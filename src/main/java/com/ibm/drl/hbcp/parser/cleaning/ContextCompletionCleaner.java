package com.ibm.drl.hbcp.parser.cleaning;

import com.google.common.collect.HashMultiset;
import com.google.common.collect.Lists;
import com.google.common.collect.Multiset;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.pdf.Document;
import com.ibm.drl.hbcp.parser.pdf.manager.PdfToDocumentFunction;
import com.ibm.drl.hbcp.parser.pdf.reparsing.ReparsePdfToDocument;
import com.ibm.drl.hbcp.util.FileUtils;
import com.ibm.drl.hbcp.util.ParsingUtils;
import com.ibm.drl.hbcp.util.Props;
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
 * Extends the context of numeric-type attributes when the context doesn't contain the value.
 *
 * Assumes that the NumericValueCleaner has been run before, equivalently, that the presence of the value
 * in the context can be checked with a simple String.contains().
 *
 * @author marting
 */
public class ContextCompletionCleaner extends NumericTypeCleaner {

    private final File pdfFolder;
    private final PdfToDocumentFunction pdfToDocument;

    private final Map<String, Optional<DocumentSentences>> parsedDocuments;

    private final SentenceDetector sbd;

    private static final Pattern WORD_OR_NUMBER_REGEX = Pattern.compile("[a-zA-Z]+|[0-9]+");
    private static final String OTHER_REGEX = "[^a-zA-Z0-9]*";

    private static final int MAXIMUM_SENTENCES = 5;
    private static final int INITIAL_EXTRA_CHARACTERS = 50;
    private static final int MAXIMUM_EXTRA_CHARACTERS = 500;

    // internal stats
    private int totalAVPs = 0;
    private int contextIncompleteAVPs = 0;
    private int abbyyNotFound = 0;
    private int contextNotFound = 0;
    private int emptyContexts = 0;
    private int cleanedAVPs = 0;

    public ContextCompletionCleaner(Properties props, List<String> numericAttributeIds) {
        super(numericAttributeIds);
        pdfFolder = FileUtils.potentiallyGetAsResource(new File(props.getProperty("coll")));
        pdfToDocument = new ReparsePdfToDocument(props);
        parsedDocuments = new HashMap<>();
        sbd = getSentenceDetector();
    }

    /** Extends the context of a numeric-type AVP. Assumes the AVP is numeric AND has been passed through the NumericValueCleaner */
    @Override
    protected AnnotatedAttributeValuePair clean(AnnotatedAttributeValuePair numericTypeAvp) {
        totalAVPs++;
        if (isContextIncomplete(numericTypeAvp)) {
            contextIncompleteAVPs++;
            // display the incomplete context here if you want, during debugging (convenient)
            return numericTypeAvp.withContext(getExtendedContext(numericTypeAvp));
        } else {
            return numericTypeAvp;
        }
    }

    private String getExtendedContext(AnnotatedAttributeValuePair avp) {
        // get the parsed documents
        Optional<DocumentSentences> docMaybe = getAbbyyDocument(avp.getDocName());
        if (docMaybe.isPresent()) {
            DocumentSentences doc = docMaybe.get();
            Optional<String> extension = getExtendedContextWithIncreasingQuery(avp.getContext(), avp.getValue(), doc);
            List<String> extendedContexts = extension.map(Collections::singletonList).orElse(Collections.emptyList());
            if (extendedContexts.isEmpty()) {
                return avp.getContext();
            } else {
                String newContext = extendedContexts.stream().max(Comparator.comparing(String::length)).get();
                cleanedAVPs++;
                return newContext;
            }
        } else {
            abbyyNotFound++;
            return avp.getContext();
        }
    }

    private Optional<String> getExtendedContextWithIncreasingQuery(String context, String value, DocumentSentences doc) {
        String docText = doc.getDocument();
        // get all the locations where the value is found
        List<Span> valueSpans = getValueSpans(value, docText);
        // query the doc for the context (in a flexible way)
        if (context.trim().isEmpty()) emptyContexts++;
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
        contextNotFound++;
        return Optional.empty();
    }

    private Optional<String> getExtendedContext(MatchResult contextMatch, List<Span> valueSpans, DocumentSentences doc) {
        // get the smallest span containing the context and a value
        Optional<Span> sharedSpan = getSmallestSharedSpan(contextMatch, valueSpans);
        if (sharedSpan.isPresent()) {
            // get sentences from the doc that span the 2
            List<Span> overlappingSentences = getOverlappingSentenceSpans(sharedSpan.get(), doc.sentenceSpans);
            if (overlappingSentences.size() <= MAXIMUM_SENTENCES) {
                // build the final context string
                String res = doc.getDocument().substring(
                        overlappingSentences.get(0).getStart(),
                        overlappingSentences.get(overlappingSentences.size() - 1).getEnd());
                return Optional.of(res);
            } else return Optional.empty();
        } else return Optional.empty();
    }

    private Optional<String> getExtendedContext(MatchResult matchResult, String text, String value) {
        String res = matchResult.group();
        int extraWindow = INITIAL_EXTRA_CHARACTERS;
        int start = matchResult.start();
        int end = matchResult.end();
        while (contextExcludesValue(res, value)) {
            // stop if we haven't found anything in many tries
            if (extraWindow > MAXIMUM_EXTRA_CHARACTERS) {
                return Optional.empty();
            }
            // extend the context (again)
            //TODO: using an extending of the start of the context only adds 3 cleaned contexts, doesn't seem worth it
            //start = Math.max(0,matchResult.start() - extraWindow);
            start = matchResult.start();
            end = Math.min(matchResult.end() + extraWindow, text.length());
            res = text.substring(start, end);
            extraWindow *= 2;
        }
        String fixedContextWithFullTokens = extendContextToNextSpace(start, end, text);
        return Optional.of(fixedContextWithFullTokens);
    }

    private Optional<Span> getSmallestSharedSpan(MatchResult contextMatch, List<Span> valueSpans) {
        Span contextSpan = new Span(contextMatch.start(), contextMatch.end());
        // get the smallest span covering both the context and a value span
        return valueSpans.stream()
                .map(valueSpan -> combineSpans(contextSpan, valueSpan))
                .min(Comparator.comparing(sharedSpan -> sharedSpan.getEnd() - sharedSpan.getStart()));
    }

    private List<Span> getOverlappingSentenceSpans(Span targetSpan, List<Span> sentenceSpans) {
        List<Span> res = new ArrayList<>();
        for (Span sentenceSpan : sentenceSpans) {
            if (targetSpan.intersects(sentenceSpan)) {
                res.add(sentenceSpan);
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

    private Pattern buildContextRegex(String context) {
        List<String> words = getAllWords(context);
        return buildContextRegex(words);
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

    private Optional<DocumentSentences> getAbbyyDocument(String docname) {
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

    private boolean isContextIncomplete(AnnotatedAttributeValuePair avp) {
        return contextExcludesValue(avp.getContext(), avp.getValue());
    }

    private boolean contextExcludesValue(String context, String value) {
        return !context.contains(value);
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

    private SentenceDetector getSentenceDetector() {
        SentenceDetector res = null;
        try (InputStream modelIn = new FileInputStream(FileUtils.potentiallyGetAsResource(new File("nlp/models/opennlp/en-sent.bin")))) {
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

    @Data
    private static class DocumentSentences {
        private final String document;
        private final List<Span> sentenceSpans;
    }

    public static void main(String[] args) throws IOException {
        ContextCompletionCleaner cleaner = new ContextCompletionCleaner(Props.loadProperties(), Lists.newArrayList());
        JSONRefParser parser = new JSONRefParser(Props.getDefaultPropFilename());
        AttributeValueCollection<AnnotatedAttributeValuePair> aavps = parser.getAttributeValuePairs();
        // first apply the numeric value cleaner
        Cleaner numericValueCleaner = new NumericValueCleaner(Arrays.asList(Props.loadProperties().getProperty("prediction.attribtype.numerical").split(",")));
        aavps = numericValueCleaner.getCleaned(aavps);
        // then clean only the contexts
        AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaner.getCleaned(aavps);
        Multiset<AnnotatedAttributeValuePair> delta = HashMultiset.create(cleaned);
        delta.removeAll(aavps.getAllPairs());
        // displays all the new AAVP contexts
        for (AnnotatedAttributeValuePair avp : delta) {
            System.out.println("=========================================");
            System.out.println(avp.getContext());
            System.out.println("\tcontains " + avp.getValue());
        }
        System.out.println(delta.size());
        System.out.println("Total: " + cleaner.totalAVPs);
        System.out.println("Context incomplete: " + cleaner.contextIncompleteAVPs);
        System.out.println("Parsing impossible: " + cleaner.abbyyNotFound);
        System.out.println("Context not found: " + cleaner.contextNotFound);
        System.out.println("Empty contexts: " + cleaner.emptyContexts);
        System.out.println("Cleaned: " + cleaner.cleanedAVPs);
    }
}
