package extractor;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import indexer.ResearchDoc;
import java.util.Random;


/**
 * Generates cross-validation folds per attribute.
 * 
 * @author dganguly
 */
public class CVFoldGenerator {
    private int numFolds;
    private Map<Integer,Integer> buckets; // key is the docId, value is the bucket number
    
    static final long SEED = 123456;
    
    public CVFoldGenerator(int k) {
        this.numFolds=k;
        if (k<2) {
            this.numFolds =2;
            System.out.println("k has to be at least 2, value has been overridden");
        }
        this.buckets= new HashMap<Integer,Integer>();
    }
    
       
    public int getNumFolds() {
        return numFolds;
    }

    public Map<Integer, Integer> getBuckets() {
        return buckets;
    }

    /**
     * Generates the folds.
     * 
     * @param numDocs Total number of documents that are annotated -- to be partitioned into train and a test.
     * @param iu Encodes the attribute to extract
     */
    public void generateFolds(int numDocs,InformationUnit iu) throws IOException {
        
        //Need to check first that there is at
        
        //initialize in a list (necessary for shuffling later)
        List<Integer> documents = new ArrayList<Integer>();
        for (int i=0;i<numDocs;++i) {
            documents.add(i);
        }
        
        //Start with a simple assignment approach           
        for (int j=0; j<documents.size(); j++) {            
           int bucket = j % this.numFolds;
           this.buckets.put(j, bucket);
        }
        
        //randomly generate a new one if necessary
        while (!this.isValid(iu)) {
            iu.logger.info("FOLD not valid - generating a new one");
            //Shuffle
            Collections.shuffle(documents, new Random(SEED));
            //regenrate
            this.buckets= new HashMap<Integer,Integer>();
            for (int j=0; j<documents.size(); j++) {            
                int bucket = j % this.numFolds;
                this.buckets.put(j, bucket);
             }
        }
    }
    
    /**
    * Checks if two positive annotations are in two different buckets.
     * @param iu Attribute to extract.
     * @return True if it's a valid CV fold, false otherwise.
     */
    public boolean isValid(InformationUnit iu) throws IOException {
        boolean foo= false;
        
        Set<Integer> bucketsWithPositiveAnnotation = new HashSet<Integer>();
        //CHANGE IT HERE
        for (Integer numDoc:this.buckets.keySet()) {
            
            String docName = iu.extractor.reader.document(numDoc).get(ResearchDoc.FIELD_NAME);                    
            iu.reInit(docName);
 
            if (iu.hasPositiveAnnotation()) {
                bucketsWithPositiveAnnotation.add(this.buckets.get(numDoc));
            }
            
            if (bucketsWithPositiveAnnotation.size()>=2) {
                foo = true;
                break;
            }
        }
        
        return foo;
    }
}
