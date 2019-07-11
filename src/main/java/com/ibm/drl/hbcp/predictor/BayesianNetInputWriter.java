package com.ibm.drl.hbcp.predictor;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;


/** Program to write a graph as input for a Bayesian Net approach (used as a baseline)
 * @author fbonin
 */
public class BayesianNetInputWriter {
        
        
	public static void main(String[] args)  {
        Map <String, ArrayList<String[]>> data = new HashMap <String, ArrayList<String[]>>();
          
        try {
         // open input stream test.txt for reading purpose.
         BufferedReader br = new BufferedReader(new FileReader("./prediction/graphs/relationswithDocTitle.graph"));
         
         String thisLine;
         while ((thisLine = br.readLine()) != null) {
            
             String [] line= thisLine.split("\t");
             String key=line[line.length-1];
             String [] attribute= Arrays.copyOf(line, line.length-1);
             if (data.containsKey(key))
                data.get(key).add(attribute);
             else {
                 ArrayList<String[]> value= new ArrayList<>();
                data.put(key, value);
                data.get(key).add(attribute);
             }
        //System.out.println(thisLine);
            
         }
         br.close();
      } catch(IOException e) {
         e.printStackTrace();
      }
      
      
        for (Map.Entry<String, ArrayList<String[]>> item :data.entrySet()) {
          
      
          System.out.println(item.getKey());
          for (String [] a : item.getValue()){
               for (int i=0; a.length>i;i++){
                     System.out.print(a[i]+ ' ');
                }
               System.out.print("|||");
            }
          System.out.println();
	}

          
      
      /*
 "Mean Age": 3587809 -->3  4507435
 "Minimum Age": 3587807 --1 4507433
 "Maximum Age": 3587808 -->2 4507434
 Outcome: 3909808  Outcome value intervention 1 (most complex)
      */
      
      String [] row= new String[7];
      
      for (Map.Entry<String, ArrayList<String[]>> item :data.entrySet()) {
          //System.out.println(item.getKey());
          row[0]=item.getKey();
          for (String [] a : item.getValue()){
              //a.length-1> because I ignore the weight which is the last valule of the array
               for (int i=0; a.length-1>i;i++){
                     //System.out.print(a[i]+ ' ');
                     String attributeID=a[i].split(":")[1];
                     //age:
                     if (attributeID.equalsIgnoreCase("4507433")) row[1]=a[i].split(":")[2];
                     if (attributeID.equalsIgnoreCase("4507434")) row[2]=a[i].split(":")[2];
                     if (attributeID.equalsIgnoreCase("4507435")) row[3]=a[i].split(":")[2];
                     
                     //gender
                     if (attributeID.equalsIgnoreCase("4507430")||
                             attributeID.equalsIgnoreCase("4507426")||
                             attributeID.equalsIgnoreCase("4507427")||
                             attributeID.equalsIgnoreCase("4507432")
                             ) row[4]=a[i].split(":")[1];
                     
                    //Intervention
                     if (a[i].split(":")[0].startsWith("I")) row[5]=a[i].split(":")[1];
                     
                      //Outcome
                     //if (a[i].split(":")[0].equalsIgnoreCase("O")) row[6]=a[i].split(":")[2];
                     if (attributeID.equalsIgnoreCase("3909808")||
                            attributeID.equalsIgnoreCase("3937167")||
                             attributeID.equalsIgnoreCase("3937812")||
                             attributeID.equalsIgnoreCase("3937812")||
                             attributeID.equalsIgnoreCase("4217650")||
                             attributeID.equalsIgnoreCase("3909809")) row[6]=a[i].split(":")[2];
                     
                     
             }
          
               for (int i=0; i<row.length;i++){System.out.print(row[i]+"\t");}
       //     System.out.println();
          }  
         
	}

      
      
      
            
        }

}
