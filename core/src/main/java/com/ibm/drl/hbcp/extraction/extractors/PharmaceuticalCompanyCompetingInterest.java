package com.ibm.drl.hbcp.extraction.extractors;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.Arm;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.Attribute;
import com.ibm.drl.hbcp.extraction.candidates.CandidateInPassage;
import com.ibm.drl.hbcp.extraction.evaluation.Evaluator;
import com.ibm.drl.hbcp.extraction.evaluation.MultiValueUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateArmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.evaluation.PredicateUnarmifiedEvaluator;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.passages.Passage;
import com.ibm.drl.hbcp.extraction.passages.PassageInIndex;
import com.ibm.drl.hbcp.extraction.evaluation.RefComparison;
import com.ibm.drl.hbcp.inforetrieval.indexer.IndexingMethod;
import com.ibm.drl.hbcp.parser.Attributes;
import com.ibm.drl.hbcp.util.Props;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.queryparser.classic.ParseException;
import org.jetbrains.annotations.NotNull;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.Properties;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Extract mentions of pharmaceutical companies (see also {@link PharmaceuticalCompanyFunding})
 *
 * @author charlesj
 *
 */
public class PharmaceuticalCompanyCompetingInterest extends OrOfKeywordsExtractor<ArmifiedAttributeValuePair> implements IndexBasedAVPExtractor {

    private static final String ATTRIB_NAME = "Pharmaceutical company competing interest";
    private static final List<String> QUERY_WORDS = Lists.newArrayList(
        "Argenta",
                "AstraZeneca",
                "Beecham",
                "Boehringer Mannheim",
                "Borregaard A/S",
                "CVS Caremark",
                "Ciba Pharmaceuticals",
                "GSK",
                "Glaxo Norway A/S",
                "Glaxo Smith Kline",
                "Glaxo-SmithKline",
                "GlaxoSmithKlein",
                "GlaxoSmithKline",
                "GlaxoSmithKline Consumer Healthcare",
                "Hoechst",
                "Lakeside Pharmaceutical",
                "Lederle Laboratories",
                "Lundbeck Ltd",
                "Marion & Rousseau",
                "Merck Sharp and Dohme,",
                "Merrell Dow Pharmaceutical",
                "Niconovum",
                "Nicorette",
                "Novartis",
                "NovoNordisk",
                "Pfizer",
                "Pfzer",
                "Pfızer",
                "Pharmacia",
                "Pﬁzer",
                "Roche Diagnostics",
                "Sanoﬁ-Aventis",
                "Smith Kline",
                "SmithKline Beecham",
                "SmithKline- Beecham",
                "Danish Pharmaceutical Association",
                "United Pharmaceuticals"
    );

    private final Attribute attribute;

    public PharmaceuticalCompanyCompetingInterest(IndexingMethod indexingMethod, int numberOfTopPassages) throws ParseException {
        super(indexingMethod, numberOfTopPassages, QUERY_WORDS, true);
        this.attribute = Attributes.get().getFromName(ATTRIB_NAME);
    }

    @Override
    public Set<Attribute> getExtractedAttributes() {
        return Sets.newHashSet(attribute);
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
    public List<Evaluator<IndexedDocument, ArmifiedAttributeValuePair>> getEvaluators() {
        PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair> predicateUnarmifiedEvaluator =
                new PredicateUnarmifiedEvaluator<IndexedDocument, ArmifiedAttributeValuePair>() {
                    @Override
                    public boolean isCorrect(@NotNull ArmifiedAttributeValuePair predicted, @NotNull ArmifiedAttributeValuePair expected) {
                        return expected.getValue().contains(predicted.getValue()) || predicted.getValue().contains(expected.getValue());
                    }
                };
        return Lists.newArrayList(
                predicateUnarmifiedEvaluator,
                new MultiValueUnarmifiedEvaluator<>(predicateUnarmifiedEvaluator),
                new PredicateArmifiedEvaluator(predicateUnarmifiedEvaluator)
        );
    }

    @Override
    public Collection<CandidateInPassage<ArmifiedAttributeValuePair>> extract(PassageInIndex passage) {
        String preProcessedWindow = preProcess(passage.getText());
        Set<String> matches = getAllMatchingCandidates(preProcessedWindow);

        List<Pair<String, Double>> candidates = matches.stream().map(c -> Pair.of(c, passage.getScore())).collect(Collectors.toList());

        return candidates.stream()
                .map(candidateWithScore -> newCandidate(candidateWithScore.getLeft(), candidateWithScore.getRight(), passage))
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return ATTRIB_NAME;
    }

    public static void main(String[] args) throws IOException, ParseException {
        Properties props = Props.loadProperties("init.properties");
        PharmaceuticalCompanyCompetingInterest hp = new PharmaceuticalCompanyCompetingInterest(IndexingMethod.slidingWindow(50, props),50);
        for (RefComparison evaluation : hp.evaluate(props)) {
            System.out.println(evaluation);
        }
    }
}