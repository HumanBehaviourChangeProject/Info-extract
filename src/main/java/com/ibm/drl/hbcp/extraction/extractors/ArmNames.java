package com.ibm.drl.hbcp.extraction.extractors;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.answerselectors.Identity;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.inforetrieval.indexer.PaperIndexer;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.util.Props;

/**
 * Extractor for getting all arm names.  Best arm names are not selected per passage or document
 * but all candidate arm names are extracted.
 * 
 * @author yhou
 *
 */
public class ArmNames extends RegexQueryExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    public final static String QUERY_STRING = "group OR groups OR condition OR conditions OR intervention OR interventions OR arm OR arms";
    private final static List<Pattern> REGEXES = Lists.newArrayList(
            Pattern.compile(" (in) the (.*?) (group|condition|intervention|arm)"),
            Pattern.compile("\\.(In) the (.*?) (group|condition|intervention|arm)"),
            Pattern.compile(" (the|The) (\\w+) (group|condition|intervention|arm)"),
            Pattern.compile(" (the|The) (\\w+ \\w+) (group|condition|intervention|arm)"),
            Pattern.compile(".*(the|The) (.*?) (groups|conditions|arms)"),
            Pattern.compile("\\.(The) (.*?) (group|condition|intervention|arm)"));
    private final Attribute attribute;
    private final Set<Attribute> relevantAttributeSet;
//    public static final String ATTRIB_ID = "5730447";
    public static final String ATTRIB_NAME = "Arm name";

    public ArmNames(int numberOfTopPassages) throws ParseException {
        this(IndexingMethod.NONE, numberOfTopPassages);
    }

    public ArmNames(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_STRING, REGEXES, false, new Identity<>());
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
        this.relevantAttributeSet = Sets.newHashSet(attribute);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return relevantAttributeSet;
    }

    @Override
    protected Set<String> getValidMatches(Matcher matcher) {
        String armCandidate = matcher.group(2);
        if (armCandidate.split(" ").length > 5 || armCandidate.contains(", ") || armCandidate.contains(". ") || armCandidate.contains(" the") || armCandidate.toLowerCase().matches("(first|second|same|latter|former|various)")|| armCandidate.toLowerCase().matches(".*?(previous|both|next|one|two|three|four|five|six|2|3|4|5|6).*?")||armCandidate.contains("other")||armCandidate.contains("another")||armCandidate.matches(".*?(in|between|of|from).*?")) {
            return new HashSet<>();
        }
        if(armCandidate.matches("(.*)? (and|or) (.*)?")){
            Pattern p = Pattern.compile("(.*)? (and|or) (.*)?");
            Matcher m = p.matcher(armCandidate);
            while(m.find()){
                String armCandidate1 = m.group(1);
                String armCandidate2 = m.group(3);
                return Sets.newHashSet(armCandidate1, armCandidate2);
            }
        }
        return Sets.newHashSet(armCandidate);

    }

    @Override
    protected CandidateInPassage<ArmifiedAttributeValuePair> newCandidate(String value, double score, Passage passage) {
        return new CandidateInPassage<>(
                passage,
                new ArmifiedAttributeValuePair(attribute, value, passage.getDocname(), Arm.EMPTY, passage.getText()),
                score,
                1.0);
    }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(PassageInIndex passage) {
        String preProcessedWindow = preProcess(passage.getText());
        Set<String> matches = getAllMatchingCandidates(preProcessedWindow);

        String[] tokens = preProcessedWindow.split("\\s+");

        List<Pair<String, Integer>> candidatesWithPosition = new ArrayList<>();
        List<Pair<String, Integer>> queryTermsWithPosition = new ArrayList<>();

        List<Pair<String, Integer>> withPosition = matches.stream().map(c -> Pair.of(c, passage.getText().indexOf(c))).collect(Collectors.toList());
        candidatesWithPosition.addAll(withPosition);

        for (int i = 0; i < tokens.length; i++) {
            int position = i; // forced to use an extra variable for the lambda
            String token = tokens[i];
            token = PaperIndexer.analyze(passage.getIndexManager().getAnalyzer(), token);

            // see if that token is a query term and set the position
            int index = Arrays.binarySearch(this.queryTerms, token);
            if (index > -1) {
                queryTermsWithPosition.add(Pair.of(token, position)); // i is the position of the query term in 'window'.
            }
        }

        List<Pair<String, Double>> res = getScoredCandidates(candidatesWithPosition, queryTermsWithPosition);
//        for(Pair pair: res){
//           System.err.println(pair.getLeft()  + ":" + pair.getRight() + "--"+ passage.getText());
//        }
        return res.stream()
                .map(candidateWithScore -> newCandidate(candidateWithScore.getLeft(), candidateWithScore.getRight(), passage))
                .collect(Collectors.toList());
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        ArmNames armNameExtractor = new ArmNames(100);
        int count = 0;
        try (IndexManager index = armNameExtractor.getDefaultIndexManager(props)) {
            JSONRefParser refParser = new JSONRefParser(props);
            Cleaners cleaners = new Cleaners(props);
            List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth
                    = armNameExtractor.getGroundTruthForEvaluation(index, refParser, cleaners);
            for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> docAndAnnotations : groundTruth) {
                Collection<ArmifiedAttributeValuePair> relevantAnnotations = armNameExtractor.getRelevant(docAndAnnotations.getRight());
                Collection<CandidateInPassage<ArmifiedAttributeValuePair>> prediction = armNameExtractor.extract(docAndAnnotations.getKey());
//                System.err.println("annotation:" + relevantAnnotations);
                if(prediction.size()<2){
                    count++;
                    System.err.println("doc:" + docAndAnnotations.getKey().getDocName());
                    System.err.println("prediction:" + prediction);
                }
            }
        }
        System.err.println(count);
    }

}
