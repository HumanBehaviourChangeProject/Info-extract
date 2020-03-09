/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ibm.drl.hbcp.experiments.flair;

import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.ibm.drl.hbcp.core.attributes.ArmifiedAttributeValuePair;
import com.ibm.drl.hbcp.core.attributes.collection.AttributeValueCollection;
import com.ibm.drl.hbcp.extraction.indexing.IndexManager;
import com.ibm.drl.hbcp.extraction.indexing.IndexedDocument;
import com.ibm.drl.hbcp.extraction.indexing.SentenceBasedIndexManager;
import com.ibm.drl.hbcp.extraction.indexing.SlidingWindowIndexManager;
import com.ibm.drl.hbcp.inforetrieval.indexer.BaseDirInfo;
import com.ibm.drl.hbcp.parser.AnnotatedAttributeValuePair;
import com.ibm.drl.hbcp.parser.JSONRefParser;
import com.ibm.drl.hbcp.parser.cleaning.Cleaners;
import com.ibm.drl.hbcp.parser.pdf.reparsing.Reparser;
import com.ibm.drl.hbcp.util.Props;
import edu.stanford.nlp.simple.Sentence;
import edu.stanford.nlp.simple.Token;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Properties;
import java.util.Set;
import java.util.TreeMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.apache.commons.lang3.tuple.Pair;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;


/**
 *
 * @author yhou
 */
public class GenerateTrainingData_NameAsCategory_LRECPaper {
/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

/**
 *
 * @author yhou
 */

    public List<String> generateTestingSentence(String docName) {
        List<String> sentences = new ArrayList();
//        File xmlPdfOutput = new File("./data/pdfs_Sprint1234_extracted/" + docName + ".xml");
        File jsonPdfOutput = new File("./data/All_330_PDFs_extracted/" + docName + ".json");
        try {
            Reparser parser = new Reparser(jsonPdfOutput);
            for (String str : parser.toText().split("\n")) {
                if (str.equalsIgnoreCase("acknowledgements") || str.equalsIgnoreCase("references")) {
                    break;
                }
                if (str.matches(".*?http:.*?")) {
                    continue;
                }
                if (str.split(" ").length < 6) {
                    continue;
                }
                str = splitDashBetweenNumbers(str);
                sentences.add(str);
            }
        } catch (Exception e) {
            System.err.println(e.toString());
        }
        return sentences;
    }

    public void generateTrainTestData_BIO_Tagging() throws IOException, Exception {
        int traintestsplit = 50;
        
        FileWriter writer1 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/train_lrec.csv"));
        StringBuffer sb1 = new StringBuffer();
        FileWriter writer2 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/test_lrec.csv"));
        StringBuffer sb2 = new StringBuffer();
        FileWriter writer4 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/unmatch_lrec.txt"));
        StringBuffer sb4 = new StringBuffer();
        List<String> testfiles = new ArrayList();

        List<Integer> instanceLength = new ArrayList();
        Map<Integer, String> instanceLengthDebug = new HashMap();
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        IndexManager index = getDefaultIndexManager(props);

//pilot        Set<String> targetedAttri = Sets.newHashSet("5730447", "5140146", "5579089", "5579090", "5579088", "5579092", "5579093", "5579095","5579096","5594105","5579097", "3673271", "3673272", "3673274", "3673283", "3673284", "3675717", "3673298", "3673300", "3675611", "3675612");

//full-priority rank1
//        Set<String> targetedAttri = Sets.newHashSet("5730447", "5140146", "5579089", "5579090", "5579088", "5579092", "5579093", "5579095","5579096","5594105","5579097", "3673271", "3673272", "3673274", "3673283", "3673284", "3675717", "3673298", "3673300", "3675611", "3675612",
//                "5579083","4788959","3675720","4087191","3870696","3870695","4087186","5579711",
//                "4085489","5579699","4788958","4087184","3870686","5580216","5580235");

//priority rank1 + rank2
        Set<String> targetedAttri = Sets.newHashSet(
//                "Minimum age",
//                "Maximum age",
//                "All male",
//                "All female",
//                "Mixed gender",
                "1.1.Goal setting (behavior)",
                "1.2 Problem solving",
                "1.4 Action planning",
                "2.2 Feedback on behaviour",
                "2.3 Self-monitoring of behavior",
                "3.1 Social support (unspecified)",
                "5.1 Information about health consequences",
                "5.3 Information about social and environmental consequences",
                "11.1 Pharmacological support",
                "11.2 Reduce negative emotions",

                "Arm name",
                "Outcome value",
                "Mean age",
                "Proportion identifying as female gender",
                "Mean number of times tobacco used",
                "Proportion identifying as male gender",
                "Proportion identifying as belonging to a specific ethnic group",
                "Lower-level geographical region",
                "Smoking",
                "4.1 Instruction on how to perform the behavior",
                
                "Longest follow up",
                "Effect size p value",
                "Effect size estimate",
                "Biochemical verification",
                "Proportion employed",
                "4.5. Advise to change behavior",
                "Proportion achieved university or college education",
                "Country of intervention",
                "Self report",
                "Odds Ratio",
                
                "Aggregate patient role",
                "Aggregate health status type",
                "Aggregate relationship status",
                "Proportion in a legal marriage or union",
                "Mean number of years in education completed",
                "Proportion belonging to specified family or household income category",
                "Proportion belonging to specified individual income category", 
                "Healthcare facility", 
                "Doctor-led primary care facility",
                "Hospital facility",
                
                "Site",
                "Individual-level allocated",
                "Individual-level analysed",
                "Face to face",
                "Distance",
                "Printed material",
                "Digital content type",
                "Website / Computer Program / App",
                "Somatic",
                "Patch",
                
                "Pill",
                "Individual",
                "Group-based",
                "Health Professional",
                "Psychologist",
                "Researcher not otherwise specified",
                "Interventionist not otherwise specified",
                "Expertise of Source");

        Map<String, List<Integer>> stat = new HashMap();
            for(String attri: targetedAttri){
                stat.put(attri, Lists.newArrayList(0,0,0,0));
        }
      
        List<String> trainingDocs = new ArrayList();
        List<String> allDocs = new ArrayList();
//        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser);
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation_fromJson_LrecOpenAccess(index, refParser);

        int count = 0;
        int instanceNum = 0;
        int instanceNum_annotate = 0;
        int problematicAnnotationCount = 0;
        for (Pair<String, Collection<AnnotatedAttributeValuePair>> pairsPerDoc : groundTruth) {
//            IndexedDocument doc = pairsPerDoc.getKey();
            String doc = pairsPerDoc.getKey();
            allDocs.add(doc);
            Set<String> matchedTableSent = new HashSet();
//            System.err.println(doc.getDocName());
            Map<String, Set<String>> annotationPerContext = new HashMap();
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String context = cap.getContext();
                String attrName = cap.getAttribute().getName();
                
                if (!targetedAttri.contains(cap.getAttribute().getName().trim())) {
                    continue;
                }
                instanceNum_annotate++;
                //annotated context
                int oldNum = stat.get(cap.getAttribute().getName().trim()).get(0);
                stat.get(cap.getAttribute().getName().trim()).set(0, oldNum + 1);
                if (isContextFromTable(context)) {
//                    System.err.println("table context:" + context);
                    //context is from table
                    int oldNum1 = stat.get(cap.getAttribute().getName().trim()).get(1);
                    stat.get(cap.getAttribute().getName().trim()).set(1, oldNum1 + 1);
                    continue;
                }
                if (context.isEmpty() || annotation.isEmpty()) {
                    //context is empty
                    int oldNum2 = stat.get(cap.getAttribute().getName().trim()).get(2);
                    stat.get(cap.getAttribute().getName().trim()).set(2, oldNum2 + 1);
                    continue;
                }
                if (cap.getAttribute().getId().equalsIgnoreCase("5730447")) {//arm name
                    List<String> armNames = cap.getArm().getAllNames();
                    annotation = "";
//                    System.err.println("context:" + context);
                    for (String s : armNames) {
//                        System.err.println("armName:" + s);
//                        annotation = s + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_");
                        annotation = s + "##" + cap.getAttribute().getName().trim();
                        if (annotationPerContext.containsKey(context)) {
//                            annotationPerContext.get(context).add(annotation + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"));
                            annotationPerContext.get(context).add(annotation + "##" + cap.getAttribute().getName().trim());
                        } else {
//                            Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"));
                            Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getName().trim());
                            annotationPerContext.put(context, anno);
                        }
                    }
                } else {
                    if (annotationPerContext.containsKey(context)) {
//                        annotationPerContext.get(context).add(annotation + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"));
                        annotationPerContext.get(context).add(annotation + "##"  + cap.getAttribute().getName().trim());
                    } else {
//                        Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getId() + "_" + cap.getAttribute().getName().replace(" ", "_"));
                        Set<String> anno = Sets.newHashSet(annotation + "##" + cap.getAttribute().getName().trim());
                        annotationPerContext.put(context, anno);
                    }
                }
            }
            //write to file per doc
            count++;
            if(count>traintestsplit){
//                testfiles.add(doc.getDocName());
                testfiles.add(doc);
            }else{
                trainingDocs.add(doc);
            }
            for (String context : annotationPerContext.keySet()) {
//                System.err.println("context:" + context);
//                System.err.println("highlightedText:" + annotationPerContext.get(context));
                instanceNum_annotate++;
                boolean problematicAnnotation = true;
                List<String> splitsent = new ArrayList();
                for(String str: context.split("( ; |\\.; |\\. ;|\\.;|\\.;|;;;)")){
                    if(str.split(" ").length>100||str.contains("\n")){
                        for(String str1: str.split("(\\. |\\, \\(|; \\(|\\n|\n|\\?)")){
                            splitsent.add(str1);
                        }
                    }else{
                        splitsent.add(str);
                    }
                }
//                for (String str : context.split("( ; |.; |. ;|.;|.;)")) {
//                for (String str : context.split("(;|,)")) {
                  for(String str: splitsent){
                      if (str.split(" ").length >= 4) {
                        str = splitDashBetweenNumbers(str);
                        Sentence sent = new Sentence(str);
                        List<NERToken> annotation = getAnnotationOnSent(sent, annotationPerContext.get(context));
//                        System.err.println(annotation + "--" + annotationPerContext.get(context));
                        if(annotation==null){
//                           System.err.println("context:" + context);
//                           System.err.println("highlightedText:" + annotationPerContext.get(context));
//                           System.err.println("\n");
                        }
                        if (annotation != null) {
                           if(context.contains("has a value of")){
                               matchedTableSent.add(context);
//                               System.err.println("context:" + context);
//                               System.err.println("highlightedText:" + annotationPerContext.get(context));
//                               System.err.println("\n");
                           }
                           problematicAnnotation = false; 
                           for(NERToken token: annotation){     
                               if(token.nertag.contains("B-")){
                                   //generated instances
//                                   String attri = token.nertag.split("_")[0].replace("B-", "");
                                   String attri = token.nertag.replace("B-", "");
                                   int oldNum3 = stat.get(attri).get(3);
                                   stat.get(attri).set(3, oldNum3+ 1);
                               }
                               
                           }
                           instanceLength.add(annotation.size());
                           instanceLengthDebug.put(annotation.size(), str);
                            if (count <= traintestsplit) {
                                for (NERToken token : annotation) {
//                                    sb1.append(doc.getDocName().replace(" ", "_") + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag + "\n");
                                    sb1.append(doc.replace(" ", "_") + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                                }
                                sb1.append("\n");
                                instanceNum++;
                            } else {
                                for (NERToken token : annotation) {
//                                    sb2.append(doc.getDocName().replace(" ", "_") + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag + "\n");
                                    sb2.append(doc.replace(" ", "_") + "\t" + token.word + "\t" + token.postag + "\t" + token.nertag.trim().replace(" ", "_") + "\n");
                                }
                                sb2.append("\n");
                                instanceNum++;
                            }
                        }

                    }
                }
                if(problematicAnnotation){
                    sb4.append("context:" + context).append("\n");
                    sb4.append("highlightedText:" + annotationPerContext.get(context)).append("\n");
                    sb4.append("\n");
                    problematicAnnotationCount++;
                }
            }
//            System.err.print(matchedTableSent);
        //for table sentences, bring in all 'O' sentences
            List<String> sents = generateTestingSentence(doc);
            int tableSentCount = 0;
            for(String sent: sents){
                if(sent.contains("has a value of")&&!matchedTableSent.contains(sent)){
                    Sentence tableSent = new Sentence(sent);
                    List<Token> tokens_original = tableSent.tokens();
                    tableSentCount++;
                    if(tableSentCount>10) break;
                    if(count<=traintestsplit){ //training
                        for(Token t: tokens_original){
                            sb1.append(doc.replace(" ", "_") + "\t" + t.originalText() + "\t" + t.posTag() + "\t" + "O" + "\n");
                        }
                        sb1.append("\n");
                    }else{ //testing
                        for(Token t: tokens_original){
                        sb2.append(doc.replace(" ", "_") + "\t" + t.originalText() + "\t" + t.posTag() + "\t" + "O" + "\n");
                    }
                        sb2.append("\n");
                    }
                     
                }
            }// add table sentences which do not have any annotations

        }

        writer1.write(sb1.toString());
        writer1.close();
        writer2.write(sb2.toString());
        writer2.close();
        writer4.write(sb4.toString());
        writer4.close();
        System.err.println("num of doc:" + count + ":" + allDocs.size());
        System.err.println("num of train doc:" + trainingDocs.size());
        System.err.println("num of test doc:" + testfiles.size());
        System.err.println("num of training+testing instances:" + instanceNum);
        System.err.println("num of HBCP annotated instances:" + instanceNum_annotate);
        System.err.println("num of problematic annotated instances:" + problematicAnnotationCount);
        for (String doc : testfiles) {
            List<String> sents = generateTestingSentence(doc);
            if(sents.isEmpty()){
                System.err.println(doc + ": no corresponding xml file");
                continue;
            }
//            FileWriter writer3 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/testfile/" + doc + ".txt"));
            FileWriter writer3 = new FileWriter(new File(BaseDirInfo.getBaseDir() + "flairExp/testfile_lrec/" + doc + ".txt"));
            StringBuffer sb3 = new StringBuffer();
            for (String sent : sents) {
                sb3.append(sent).append("\n");
            }
            writer3.write(sb3.toString());
            writer3.close();
        }
        for(String attri: stat.keySet()){
            System.err.println(attri + "\t" + stat.get(attri).get(0) + "\t" + stat.get(attri).get(1) + "\t" + stat.get(attri).get(2) + "\t" + stat.get(attri).get(3));
        }
//        for(int i: instanceLength){
//            System.err.println(i);
//        }
        Collections.sort(instanceLength, Collections.reverseOrder()); 
        System.err.println("longest instance:" + instanceLength.get(0));
        System.err.println("isntanceLength:" + instanceLength);
        for(int i=0; i<instanceLength.size(); i++){
            if(instanceLength.get(i)>100){
                System.err.println(instanceLengthDebug.get(instanceLength.get(i)));
            }
        }
        System.err.println(instanceLengthDebug.get(instanceLength.get(0)));

    }

    private List<NERToken> getAnnotationOnSent(Sentence sent, Set<String> annotationsWithType) throws IOException, Exception {
        List<Token> tokens_original = sent.tokens();
        List<NERToken> tokens = new ArrayList();
        for (Token tok : tokens_original) {
//            NERToken nertoken = new NERToken(tok.word(), tok.posTag(), "O");
            NERToken nertoken = new NERToken(tok.originalText(), tok.posTag(), "O");
            tokens.add(nertoken);
        }
        List<String> tokenStr = new ArrayList();
        for (NERToken token : tokens) {
            tokenStr.add(token.word);
        }
        //derive annotation
        Map<Integer, String> annotation = new TreeMap();
        for (String str : annotationsWithType) {
            String highlightedText = str.split("##")[0];
            String type = str.split("##")[1];
            annotateSentFromKB(tokenStr, Sets.newHashSet(highlightedText), type, annotation);
        }
        if (annotation.isEmpty()) {
            return null;
        }
        for (Integer index : annotation.keySet()) {
            String type = annotation.get(index).split("#")[1];
            int length = Integer.valueOf(annotation.get(index).split("#")[0]);
            tokens.get(index).setNER("B-" + type);
            for (int i = 1; i < length; i++) {
                tokens.get(index + i).setNER("I-" + type);
            }
        }
        return tokens;
    }

    private void annotateSentFromKB(List<String> tokens, Set<String> kb, String type, Map<Integer, String> annotation) {
        for (String entryStr : kb) {
            Sentence sent = new Sentence(entryStr);
            List<String> entry = new ArrayList();
            for (Token t : sent.tokens()) {
                entry.add(t.originalText());
            }
//            for (int i = 0; i < entryStr.split(" ").length; i++) {
//                entry.add(entryStr.split(" ")[i].toLowerCase());
//            }
            int index = Collections.indexOfSubList(tokens, entry);
            if (index > -1) {
                annotation.put(index, entry.size() + "#" + type);
            }
        }
    }

    private Boolean isContextFromTable(String context) {
        boolean res = false;
        Pattern p = Pattern.compile(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%).*?");
        Matcher m = p.matcher(context);
        int wordCount = 0;
        List<String> numbers = new ArrayList();
        for (String s : context.split(" ")) {
            if (s.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%).*?")) {
                numbers.add(s);
            }
        }
        if (numbers.size() > 0 && numbers.size() / (0.0 + context.split(" ").length - numbers.size()) > 0.5) {
            res = true;
        }
        if (context.matches(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%) (\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%) (\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%).*?")) {
            res = true;
        }
        return res;
    }

    /**
     * Returns a list of documents paired with their annotations from a doc
     * index and a JSON annotation parser. Useful to quickly evaluate the
     * extractor with a default index/JSON.
     */
    List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> getGroundTruthForEvaluation(
            IndexManager index,
            JSONRefParser refParser
    ) throws IOException {
        List<IndexedDocument> allDocs = index.getAllDocuments();
        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> res = new ArrayList<>();
        for (IndexedDocument doc : allDocs) {
            Collection<? extends ArmifiedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(doc.getDocName());
            // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
            if (annotations == null) {
                String docname = doc.getDocName();
                System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                continue;
            }
            res.add(Pair.of(doc, new ArrayList<>(annotations)));
        }
        return res;
    }

    /**
     * Get an index manager as defined by the properties. Can fail if properties
     * are missing or wrongly defined
     */
    IndexManager getDefaultIndexManager(Properties props) throws IOException {
        // figure out the directory of the index
        String indexPath = props.getProperty("index");
        File indexDir = new File(indexPath);
        Directory directory = FSDirectory.open(indexDir.toPath());
        // pick between sentence-based or sliding window paragraphs
        if (Boolean.parseBoolean(props.getProperty("use.sentence.based"))) {
            int numberOfSentencesPerParagraph = Integer.parseInt(props.getProperty("para.number.of.sentences"));
            return new SentenceBasedIndexManager(directory, numberOfSentencesPerParagraph, IndexManager.DEFAULT_ANALYZER);
        } else {
            return new SlidingWindowIndexManager(directory, props.getProperty("window.sizes").split(","), IndexManager.DEFAULT_ANALYZER);
        }
    }
    
    public void count() throws IOException, Exception{
          Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        IndexManager index = getDefaultIndexManager(props);

        List<Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>>> groundTruth = getGroundTruthForEvaluation(index, refParser);
        int instanceNum_annotate = 0;
        for (Pair<IndexedDocument, Collection<ArmifiedAttributeValuePair>> pairsPerDoc : groundTruth) {
            IndexedDocument doc = pairsPerDoc.getKey();
            Map<String, Set<String>> annotationPerContext = new HashMap();
            for (ArmifiedAttributeValuePair cap : pairsPerDoc.getValue()) {
                String annotation = cap.getValue();
                String context = cap.getContext();
                if (cap.getAttribute().getId().equalsIgnoreCase("5140146")) {
                    instanceNum_annotate++;
                }
            }
        }
        System.err.println(instanceNum_annotate);
    }
    
    
    public void tmp() throws IOException, Exception{
        Properties props = Props.loadProperties();
        JSONRefParser refParser = new JSONRefParser(props);
        refParser.getAttributeValuePairs().byDoc();   
        for(String docname: refParser.getAttributeValuePairs().byDoc().keySet()){
            Collection<? extends ArmifiedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);
        }
    }
    
 
    /**
     * Returns a list of documents paired with their annotations from a doc
     * index and a JSON annotation parser. Useful to quickly evaluate the
     * extractor with a default index/JSON.
     */
    List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getGroundTruthForEvaluation_fromJson(
            IndexManager index,
            JSONRefParser refParser
    ) throws IOException {
        Cleaners cleaners = new Cleaners(Props.loadProperties());
//        AttributeValueCollection<AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs();
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> res = new ArrayList<>();
        for (String docname : refParser.getAttributeValuePairs().byDoc().keySet()) {
// 	AttributeValueCollection<AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);           
            Collection<? extends AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);
            // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
            if (annotations == null) {
                System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                continue;
            }
            AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(new AttributeValueCollection<>(annotations));
//            res.add(Pair.of(docname, new ArrayList<>(annotations)));
            res.add(Pair.of(docname, new ArrayList<>(cleaned)));
        }
        return res;
    }
    
    
    /**
     * Returns a list of documents paired with their annotations from a doc
     * index and a JSON annotation parser. Useful to quickly evaluate the
     * extractor with a default index/JSON.
     */
    List<Pair<String, Collection<AnnotatedAttributeValuePair>>> getGroundTruthForEvaluation_fromJson_LrecOpenAccess(
            IndexManager index,
            JSONRefParser refParser
    ) throws IOException {
        String openAccessJsonsPath = "data/lrec2020/openaccesspapers_extracted_humanreadable";
        Set<String> shortTitles = Arrays.stream(new File(openAccessJsonsPath).listFiles())
                .map(File::getName)
                .map(filename -> filename.replaceAll("\\.json", ""))
                .collect(Collectors.toSet());
         Cleaners cleaners = new Cleaners(Props.loadProperties());
        List<Pair<String, Collection<AnnotatedAttributeValuePair>>> res = new ArrayList<>();
        for (String docname : refParser.getAttributeValuePairs().byDoc().keySet()) {
            Collection<? extends AnnotatedAttributeValuePair> annotations = refParser.getAttributeValuePairs().byDoc().get(docname);
            // copy the annotations to a new list to make them true ArmifiedAttributeValuePair (remove the "Annotated" part)
            if (annotations == null) {
                System.err.println(docname + " NOT FOUND in the JSON " + refParser.getFile().getName());
                continue;
            }
            Optional<JSONRefParser.PdfInfo> pdfInfo = refParser.getDocInfo(docname);
            if (pdfInfo.isPresent()) {
                String shortTitle = pdfInfo.get().getShortTitle();
                if(!shortTitles.contains(shortTitle)) continue;
            }
            AttributeValueCollection<AnnotatedAttributeValuePair> cleaned = cleaners.clean(new AttributeValueCollection<>(annotations));
            res.add(Pair.of(docname, new ArrayList<>(cleaned)));
        }
        return res;
    }
    
    public String splitDashBetweenNumbers(String originalSent){
        String newSent = originalSent;
        Pattern number_number = Pattern.compile(".*?(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%)-(\\d+|\\d+\\%|\\d+\\.\\d+|\\d+\\.\\d+\\%|[a-zA-Z].*?).*?");
        Matcher m = number_number.matcher(originalSent);
        List<String> candidates = new ArrayList();
        while(m.find()){
            candidates.add(m.group(1) + "-" + m.group(2));
         }
        for(String cand: candidates){
            String newStr  = cand.split("-")[0] + " " + "-" + " "+ cand.split("-")[1];
            newSent = newSent.replace(cand, newStr);
        }
        return newSent;
    }

    public static void main(String[] args) throws IOException, Exception {
        GenerateTrainingData_NameAsCategory_LRECPaper generatorTrainTest = new GenerateTrainingData_NameAsCategory_LRECPaper();
//        String s1 = "6 months Web 27.4 PTC 30.6 1.17 (0.86, 1.58)";
//        String s2 = "Model 1: Quit at 12 weeks 11 (28.2%) 18 (43.9%) 2.0 (0.8, 5.1) 2.2 b (0.8, 6.2) Model 2: Quit at 26 weeks 7 (18.0%) 15 (36.6%) 2.6 (0.9, 7.4) 3.6 c (1.1, 11.4) NRT vs. NRT plus hypnotherapy";
//        System.err.println(generatorTrainTest.isContextFromTable(s1));
//        System.err.println(generatorTrainTest.isContextFromTable(s2));

        generatorTrainTest.generateTrainTestData_BIO_Tagging();
//        generatorTrainTest.count();

//          generatorTrainTest.generateTestingSentence("Abroms 2008.pdf");
//        String text = "We briefly reviewed some of the practicalities of stopping; and we said that although it might be hard, if anyone really wanted to stop, he could.";
////        String text = "Figure 2 illustrates the 24-hr point-prevalence outcome data for the two treatment conditions at baseline and at 12-month and 24-month follow-ups.";
//        Sentence sent = new Sentence(generatorTrainTest.splitDashBetweenNumbers(text));
//          
//          for(Token t: sent.tokens()){
////              System.err.println(t.originalText());
//          }

    }

}

