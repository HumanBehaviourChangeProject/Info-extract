package experiments;

import java.util.ArrayList;
import java.util.List;

import extractor.AbstractDetectPresenceAttribute;
import extractor.InformationExtractor;
import extractor.RefComparison;
import indexer.ExtractedInfoIndexer;
import indexer.ResearchDoc;
import ref.JSONRefParser;

public class ThresholdDPExperiment {
    
    private String attribId;
    private double[] thresholdValues;
    private RefComparison[] perf;
    
    public ThresholdDPExperiment(String attribId, double[] values) {
        this.attribId = attribId;
        this.thresholdValues=values;
        if (values.length < 1) {
            System.out.println("ERROR : you need at least one value to test for the Experiment");
        } else {
            this.perf= new RefComparison[values.length];
        }
    }
    
    //runs all experiments and keeps track of performance
    public void runExperiment(String propFile) throws Exception {
        
        //Need to create an informationExtrac
        InformationExtractor extractor = new InformationExtractor(propFile);
        AbstractDetectPresenceAttribute iu = new AbstractDetectPresenceAttribute(extractor, ResearchDoc.FIELD_CONTENT, JSONRefParser.INTERVENTION, this.attribId);
        
        // For saving the extracted information in an index...
        // To be used by the web interface and also to be viewed
        // independently of this application.
        //NEED TO REMOVE THAT - SHOULD BE ELSEWHERE IN THE CODE
        ExtractedInfoIndexer ieIndexer = new ExtractedInfoIndexer(extractor.getPropFile());
      
        
        for (int i=0;i<this.thresholdValues.length;++i) {
            //sets the threshold value
            iu.setThreshold(this.thresholdValues[i]);
            //performs the evaluation and stores the results
            extractor.extractInformationIU(iu, ieIndexer);
            this.perf[i] = iu.getEval();
        }
        
        //print results
        //System.out.println(this.toString());
        
        ieIndexer.close();      
    }
    
    public String toString() {
        String foo = "";
        for (int i=0;i<this.thresholdValues.length;++i) {
            foo+= "Attribute = "+ attribId+ " ; "+ "Threshold value = " + this.thresholdValues[i] + " ; ";
            foo+= this.perf[i].toString(true,false,true);
            foo+="\n";
        }
        
        return foo;    
    }
    
    public static void main(String[] args) {

        String propFile = "init.properties";
        String[] attributes = {"3673271","3673272","3673274","3673283","3673284","3675717","3673298","3673300","3675611","3675612"};
        double[] thresholdValues= {0.01,0.1,0.15,0.2,0.25,0.3,0.5};
        List<ThresholdDPExperiment> exps =new ArrayList<ThresholdDPExperiment>();
        
        try {
            for (String attribId:attributes) {
                ThresholdDPExperiment exp = new ThresholdDPExperiment(attribId,thresholdValues); 
                exp.runExperiment(propFile);
                exps.add(exp);
            }
            for(ThresholdDPExperiment exp: exps) {
                System.out.println(exp.toString());
                System.out.println();        
            }

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    
    
    
}
