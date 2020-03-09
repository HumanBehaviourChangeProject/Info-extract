package com.ibm.drl.hbcp.experiments;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Properties;

/**
 *
 * This class allow to run the extractor experiments by configuring the properties in the main.
 * Output results on syso, on a cvs file, in html and [todo] json
 * 
 * @author francesca
 */
public class Experiments {

    
    // runnig com.ibm.drl.hbcp.experiments and outputing in several ways ( csv, syso, jason[todo], html).
    //OUTPUT in output/*
    
    
    // todo CHANGE THRESHOLD TO PARAM FROM CONFIG
    private static double threshold=0.2;

    public Experiments(String propFile) throws Exception {
    }
     
    private static List<Experiment> runExperiments(boolean supervised, boolean withNB, String[] attributes, String propFile) {
        // run com.ibm.drl.hbcp.experiments: there is one instance of Experiment per each attribute
        
        List<Experiment> exps =new ArrayList<>();

        try {
            for (String attribId:attributes) {
                Experiment exp = new Experiment(attribId,propFile); 
                exp.runExperiment(supervised, withNB, threshold);
                exps.add(exp);
                // exp.toString();
            }
           

        }
        catch (Exception ex) {
            ex.printStackTrace();
        }
        return exps;
    }
    
    
    private static void printExperimentsResults(List<Experiment> exps) {
  //  System.out.println("Sto per entrare"+ exps);
        System.out.println("Attribute\tPrecision\tRecall\tF-Score\tAccuracy\tMeteor");
        
        for(Experiment exp: exps) {
                 exp.printAttributeResults();
        }
    }
    
    
     private static List<Double> computeAggregateResults(List<Experiment> exps) {
  //  System.out.println("Sto per entrare"+ exps);
        List<Double> aggregatedRes= new ArrayList<Double>();
           int len=exps.size();
           double sumP=0.0;
           double sumRec=0.0;
           double sumFscore=0.0;
           double sumAcc=0.0;
           double sumMeteor=0.0;
           
           for(Experiment exp: exps) {
               sumP = exp.res.getPrec1() + sumP;
               sumRec = exp.res.getRecall1() + sumRec;
               sumFscore = exp.res.getFscore1() + sumFscore;
               sumAcc = exp.res.getAccuracy1() + sumAcc;
               sumMeteor = exp.res.getMeteor1() + sumMeteor;
            }
           
           aggregatedRes.add(sumP/len);
           aggregatedRes.add(sumRec/len);
           aggregatedRes.add(sumFscore/len);
           aggregatedRes.add(sumAcc/len);
           aggregatedRes.add(sumMeteor/len);
           
           
           return aggregatedRes;
    }
    
    private static void printAggregatedResults(List<Double> aggregatedRes) {
         System.out.print("Avg\t");
        for (Double aggr: aggregatedRes){
            System.out.print(aggr+"\t");
        }
        System.out.println("");
    }
    private static void exportResultsToJson() {
        throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
    }

    private static void exportResultsToHTML(List<Experiment> exps, String sup, String tt) throws IOException {
        
      String date = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
      String settings= sup+tt+date;
      String FILENAME=settings+".html";

      BufferedWriter bw = new BufferedWriter(new FileWriter("output/"+FILENAME));
      
      String res="";
      
      String [][] arrayRes = new String[exps.size()+1][6];
      arrayRes[0]= new String [] {"Attribute","Precision","Recall","F-Score","Accuracy","Meteor"};
      int i=1;
      for(Experiment exp: exps) {
                 res=exp.exportAttributeResults();
                 arrayRes[i]= res.split("\t");
                 i++;
        }
     
      String htmlTable= HTMLRender.array2HTML(arrayRes);
      bw.write("Settings: "+sup +" "+ tt);  
      bw.write(htmlTable);
      bw.close();
        
    }

     private static void exportResultsToCSV(List<Experiment> exps, String sup, String tt) throws IOException {
      String date = new SimpleDateFormat("yyyyMMddhhmm").format(new Date());
      
      String settings= sup+tt+date;
      String FILENAME=settings+".csv";
      // String FILENAME="pippo.txt";
        BufferedWriter bw = new BufferedWriter(new FileWriter("output/"+FILENAME));
        bw.write("Settings\tAttribute\tPrecision\tRecall\tF-Score\tAccuracy\tMeteor\n");
        String res="";
        for(Experiment exp: exps) {
                 res=sup+"-"+tt+"\t"+exp.exportAttributeResults();
                 bw.write(res);
        }
        bw.close();
    
    }

    public static void main(String[] args) throws FileNotFoundException, Exception {
         
        //set of Exp:
        // unsup
        // supervised with th (TrainTest)
        // supervised with classifier (TrainTest)
      
        //Setting some PARAM
       
       boolean supervised=true; 
       boolean withNB=false;
       
      String propFile =  "init.properties";
      // String propFile="config2-30072018-supervised-traintest.txt";

       String[] attributes = {"3673271","3673272","3673274","3673283","3673284","3675717","3673298","3673300","3675611","3675612"};
       //String[] attributes = {"3673271","3673272"};
       
       
       Properties prop = new Properties();
       prop.load(new FileReader(propFile));
       
       System.out.println("Running...");
       
       //initialize a List of experiment ( an experiment per attribute)
       List<Experiment> exps =new ArrayList<>();
       exps=Experiments.runExperiments(supervised, withNB, attributes,propFile);
       List<Double>aggregatedRes= computeAggregateResults(exps);
        
       String tt= (Boolean.parseBoolean(prop.getProperty("traintest.activate"))) ? "TrainTest" : "CrossValidation";
       if (!(supervised)) tt="";
       
       String sup= (supervised) ? "supervised" : "unsupervised";
       System.out.println("Printing results "+sup +" "+ tt);
       
       printExperimentsResults(exps);
       printAggregatedResults(aggregatedRes);
        
        
        //Json
        //exportResultsToJson();
        
        //html
        exportResultsToHTML(exps, sup, tt);
         //csv
        exportResultsToCSV(exps, sup, tt);
       }
    
    
}
